package com.studenthub.controller;

import com.studenthub.dto.CourseRequest;
import com.studenthub.dto.CourseResponse;
import com.studenthub.dto.PagedResponse;
import com.studenthub.service.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
@Tag(name = "Courses")
public class CourseController {

    private final CourseService courseService;

    @GetMapping
    @Operation(summary = "List all courses (paginated)")
    public PagedResponse<CourseResponse> list(
            @RequestParam(required = false) String department,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return courseService.listAll(department, Pages.of(page, size, Sort.by("code")));
    }

    @GetMapping("/search")
    @Operation(summary = "Search courses by name, code, or department")
    public PagedResponse<CourseResponse> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return courseService.search(q, Pages.of(page, size, Sort.by("code")));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a course by ID")
    public CourseResponse getById(@PathVariable Long id) {
        return courseService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    @Operation(summary = "Create a new course", security = @SecurityRequirement(name = "bearerAuth"))
    public CourseResponse create(@Valid @RequestBody CourseRequest req) {
        return courseService.create(req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('INSTRUCTOR') or hasRole('ADMIN')")
    @Operation(summary = "Update a course", security = @SecurityRequirement(name = "bearerAuth"))
    public CourseResponse update(@PathVariable Long id, @Valid @RequestBody CourseRequest req) {
        return courseService.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a course", security = @SecurityRequirement(name = "bearerAuth"))
    public void delete(@PathVariable Long id) {
        courseService.delete(id);
    }
}
