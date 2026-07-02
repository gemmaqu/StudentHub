package com.studenthub.service;

import com.studenthub.dto.PagedResponse;
import com.studenthub.dto.ResourceRequest;
import com.studenthub.dto.ResourceResponse;
import com.studenthub.exception.ResourceNotFoundException;
import com.studenthub.model.Course;
import com.studenthub.model.Resource;
import com.studenthub.model.Resource.ResourceType;
import com.studenthub.model.User;
import com.studenthub.repository.CourseRepository;
import com.studenthub.repository.ResourceRepository;
import com.studenthub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ResourceService {

    private final ResourceRepository resourceRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    @Cacheable(value = "resources", key = "'search:' + #q + ':' + #courseId + ':' + #type + ':' + #pageable.pageNumber")
    public PagedResponse<ResourceResponse> search(String q, Long courseId, ResourceType type, Pageable pageable) {
        return PagedResponse.of(resourceRepository.search(q, courseId, type, pageable), ResourceResponse::from);
    }

    @Cacheable(value = "resources", key = "'course:' + #courseId + ':' + #pageable.pageNumber")
    public PagedResponse<ResourceResponse> getByCourse(Long courseId, Pageable pageable) {
        return PagedResponse.of(resourceRepository.findByCourseId(courseId, pageable), ResourceResponse::from);
    }

    @Cacheable(value = "resources", key = "#id")
    public ResourceResponse getById(Long id) {
        return ResourceResponse.from(findOrThrow(id));
    }

    @Transactional
    @CacheEvict(value = "resources", allEntries = true)
    public ResourceResponse create(ResourceRequest req, String uploaderUsername) {
        Course course = courseRepository.findById(req.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + req.getCourseId()));
        User uploader = userRepository.findByUsername(uploaderUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + uploaderUsername));

        Resource resource = Resource.builder()
                .title(req.getTitle())
                .description(req.getDescription())
                .fileUrl(req.getFileUrl())
                .fileType(req.getFileType())
                .fileSize(req.getFileSize())
                .resourceType(req.getResourceType())
                .course(course)
                .uploader(uploader)
                .build();
        return ResourceResponse.from(resourceRepository.save(resource));
    }

    @Transactional
    @CacheEvict(value = "resources", allEntries = true)
    public ResourceResponse update(Long id, ResourceRequest req, String username) {
        Resource resource = findOrThrow(id);
        if (!resource.getUploader().getUsername().equals(username))
            throw new AccessDeniedException("Only the uploader may edit this resource");

        resource.setTitle(req.getTitle());
        resource.setDescription(req.getDescription());
        resource.setFileUrl(req.getFileUrl());
        resource.setFileType(req.getFileType());
        resource.setFileSize(req.getFileSize());
        resource.setResourceType(req.getResourceType());
        return ResourceResponse.from(resourceRepository.save(resource));
    }

    @Transactional
    @CacheEvict(value = "resources", allEntries = true)
    public void delete(Long id, String username) {
        Resource resource = findOrThrow(id);
        if (!resource.getUploader().getUsername().equals(username))
            throw new AccessDeniedException("Only the uploader may delete this resource");
        resourceRepository.delete(resource);
    }

    @Transactional
    public ResourceResponse recordDownload(Long id) {
        Resource resource = findOrThrow(id);
        resourceRepository.incrementDownloadCount(id);
        resource.setDownloadCount(resource.getDownloadCount() + 1);
        return ResourceResponse.from(resource);
    }

    private Resource findOrThrow(Long id) {
        return resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found: " + id));
    }
}
