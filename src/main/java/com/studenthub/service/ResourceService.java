package com.studenthub.service;

import com.studenthub.config.EvictCatalogCaches;
import com.studenthub.dto.PagedResponse;
import com.studenthub.dto.ResourceRequest;
import com.studenthub.dto.ResourceResponse;
import com.studenthub.exception.BadRequestException;
import com.studenthub.exception.ResourceNotFoundException;
import com.studenthub.model.Course;
import com.studenthub.model.Resource;
import com.studenthub.model.Resource.ResourceType;
import com.studenthub.model.User;
import com.studenthub.repository.CourseRepository;
import com.studenthub.repository.ResourceRepository;
import com.studenthub.repository.ResourceSpecifications;
import com.studenthub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.function.IntConsumer;

@Service
@RequiredArgsConstructor
public class ResourceService {

    private static final int MAX_TITLE_LENGTH = 255;

    private final ResourceRepository resourceRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final FileStorage fileStorage;

    @Cacheable(value = "resources", key = "'search:' + #q + ':' + #courseId + ':' + #type + ':' + #pageable.pageNumber + ':' + #pageable.pageSize")
    public PagedResponse<ResourceResponse> search(String q, Long courseId, ResourceType type, Pageable pageable) {
        return PagedResponse.of(
                resourceRepository.findAll(ResourceSpecifications.search(q, courseId, type), pageable),
                ResourceResponse::from);
    }

    @Cacheable(value = "resources", key = "'course:' + #courseId + ':' + #pageable.pageNumber + ':' + #pageable.pageSize")
    public PagedResponse<ResourceResponse> getByCourse(Long courseId, Pageable pageable) {
        return PagedResponse.of(resourceRepository.findByCourseId(courseId, pageable), ResourceResponse::from);
    }

    public PagedResponse<ResourceResponse> getByUploader(String username, Pageable pageable) {
        return PagedResponse.of(resourceRepository.findByUploaderId(findUser(username).getId(), pageable), ResourceResponse::from);
    }

    @Cacheable(value = "resources", key = "#id")
    public ResourceResponse getById(Long id) {
        return ResourceResponse.from(findOrThrow(id));
    }

    @Transactional
    @EvictCatalogCaches
    public ResourceResponse create(ResourceRequest req, String uploaderUsername) {
        Resource resource = Resource.builder()
                .title(req.getTitle())
                .description(req.getDescription())
                .fileUrl(req.getFileUrl())
                .fileType(req.getFileType())
                .fileSize(req.getFileSize())
                .resourceType(req.getResourceType())
                .course(findCourse(req.getCourseId()))
                .uploader(findUser(uploaderUsername))
                .build();
        return ResourceResponse.from(resourceRepository.save(resource));
    }

    @Transactional
    @EvictCatalogCaches
    public ResourceResponse upload(MultipartFile file, String title, String description, ResourceType type,
                                   Long courseId, String uploaderUsername) {
        if (file.isEmpty()) throw new BadRequestException("File must not be empty");
        if (!StringUtils.hasText(title) || title.length() > MAX_TITLE_LENGTH)
            throw new BadRequestException("Title must be between 1 and " + MAX_TITLE_LENGTH + " characters");
        Course course = findCourse(courseId);
        User uploader = findUser(uploaderUsername);

        String key = fileStorage.store(file);
        afterCompletion(status -> {
            if (status != TransactionSynchronization.STATUS_COMMITTED) fileStorage.delete(key);
        });

        Resource resource = resourceRepository.save(Resource.builder()
                .title(title)
                .description(description)
                .fileUrl("pending")
                .fileType(contentTypeOf(file))
                .fileSize(file.getSize())
                .storageKey(key)
                .originalFilename(safeFilename(file.getOriginalFilename()))
                .resourceType(type)
                .course(course)
                .uploader(uploader)
                .build());
        resource.setFileUrl("/api/resources/" + resource.getId() + "/file");
        return ResourceResponse.from(resource);
    }

    @Transactional
    @EvictCatalogCaches
    public ResourceResponse update(Long id, ResourceRequest req, String username) {
        Resource resource = findOrThrow(id);
        if (!resource.getUploader().getUsername().equals(username))
            throw new AccessDeniedException("Only the uploader may edit this resource");

        resource.setTitle(req.getTitle());
        resource.setDescription(req.getDescription());
        resource.setResourceType(req.getResourceType());
        if (resource.getStorageKey() == null) {
            resource.setFileUrl(req.getFileUrl());
            resource.setFileType(req.getFileType());
            resource.setFileSize(req.getFileSize());
        }
        return ResourceResponse.from(resourceRepository.save(resource));
    }

    @Transactional
    @EvictCatalogCaches
    public void delete(Long id, String username) {
        Resource resource = findOrThrow(id);
        if (!resource.getUploader().getUsername().equals(username))
            throw new AccessDeniedException("Only the uploader may delete this resource");
        resourceRepository.delete(resource);

        String key = resource.getStorageKey();
        if (key != null) {
            afterCompletion(status -> {
                if (status == TransactionSynchronization.STATUS_COMMITTED) fileStorage.delete(key);
            });
        }
    }

    // Evicts only this entry: downloads are frequent, so cached pages may show a count up to one TTL old.
    @Transactional
    @CacheEvict(value = "resources", key = "#id")
    public ResourceResponse recordDownload(Long id) {
        if (resourceRepository.incrementDownloadCount(id) == 0)
            throw new ResourceNotFoundException("Resource not found: " + id);
        return ResourceResponse.from(findOrThrow(id));
    }

    @Transactional
    @CacheEvict(value = "resources", key = "#id")
    public StoredFile download(Long id) {
        Resource resource = findOrThrow(id);
        if (resource.getStorageKey() == null)
            throw new ResourceNotFoundException("Resource has no uploaded file: " + id);
        org.springframework.core.io.Resource content = fileStorage.load(resource.getStorageKey())
                .orElseThrow(() -> new ResourceNotFoundException("Stored file is missing: " + id));
        StoredFile stored = new StoredFile(content, resource.getOriginalFilename(), resource.getFileType(), resource.getFileSize());
        resourceRepository.incrementDownloadCount(id);
        return stored;
    }

    private Resource findOrThrow(Long id) {
        return resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found: " + id));
    }

    private Course findCourse(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + id));
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    private static String safeFilename(String original) {
        String name = StringUtils.getFilename(StringUtils.cleanPath(original == null ? "" : original));
        if (!StringUtils.hasText(name)) return "download";
        return name.length() > 255 ? name.substring(name.length() - 255) : name;
    }

    private static String contentTypeOf(MultipartFile file) {
        String type = file.getContentType();
        return type == null || type.length() > 150 ? MediaType.APPLICATION_OCTET_STREAM_VALUE : type;
    }

    private static void afterCompletion(IntConsumer callback) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                callback.accept(status);
            }
        });
    }
}
