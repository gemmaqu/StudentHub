package com.studenthub.service;

import com.studenthub.dto.CourseRequest;
import com.studenthub.dto.CourseResponse;
import com.studenthub.dto.PagedResponse;
import com.studenthub.exception.ConflictException;
import com.studenthub.exception.ResourceNotFoundException;
import com.studenthub.model.Course;
import com.studenthub.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;

    @Cacheable(value = "courses", key = "'all:' + #pageable.pageNumber + ':' + #pageable.pageSize")
    public PagedResponse<CourseResponse> listAll(Pageable pageable) {
        return PagedResponse.of(courseRepository.findAll(pageable), CourseResponse::from);
    }

    @Cacheable(value = "courses", key = "#id")
    public CourseResponse getById(Long id) {
        return CourseResponse.from(findOrThrow(id));
    }

    @Cacheable(value = "courses", key = "'search:' + #q + ':' + #pageable.pageNumber")
    public PagedResponse<CourseResponse> search(String q, Pageable pageable) {
        return PagedResponse.of(courseRepository.search(q, pageable), CourseResponse::from);
    }

    @Transactional
    @CacheEvict(value = "courses", allEntries = true)
    public CourseResponse create(CourseRequest req) {
        if (courseRepository.existsByCode(req.getCode()))
            throw new ConflictException("Course code already exists: " + req.getCode());
        Course course = Course.builder()
                .code(req.getCode().toUpperCase())
                .name(req.getName())
                .description(req.getDescription())
                .department(req.getDepartment())
                .build();
        return CourseResponse.from(courseRepository.save(course));
    }

    @Transactional
    @CacheEvict(value = "courses", allEntries = true)
    public CourseResponse update(Long id, CourseRequest req) {
        Course course = findOrThrow(id);
        course.setName(req.getName());
        course.setDescription(req.getDescription());
        course.setDepartment(req.getDepartment());
        return CourseResponse.from(courseRepository.save(course));
    }

    @Transactional
    @CacheEvict(value = "courses", allEntries = true)
    public void delete(Long id) {
        courseRepository.delete(findOrThrow(id));
    }

    private Course findOrThrow(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + id));
    }
}
