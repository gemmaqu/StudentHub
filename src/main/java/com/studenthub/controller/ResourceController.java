package com.studenthub.controller;

import com.studenthub.dto.PagedResponse;
import com.studenthub.dto.ResourceRequest;
import com.studenthub.dto.ResourceResponse;
import com.studenthub.model.Resource.ResourceType;
import com.studenthub.service.ResourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/resources")
@RequiredArgsConstructor
@Tag(name = "Resources")
public class ResourceController {

    private final ResourceService resourceService;

    @GetMapping("/search")
    @Operation(summary = "Search resources by keyword, course, or type")
    public PagedResponse<ResourceResponse> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) ResourceType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return resourceService.search(q, courseId, type, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @GetMapping("/course/{courseId}")
    @Operation(summary = "List all resources for a course")
    public PagedResponse<ResourceResponse> getByCourse(
            @PathVariable Long courseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return resourceService.getByCourse(courseId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a resource by ID")
    public ResourceResponse getById(@PathVariable Long id) {
        return resourceService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Upload / register a new resource", security = @SecurityRequirement(name = "bearerAuth"))
    public ResourceResponse create(
            @Valid @RequestBody ResourceRequest req,
            @AuthenticationPrincipal UserDetails user) {
        return resourceService.create(req, user.getUsername());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a resource (uploader only)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResourceResponse update(
            @PathVariable Long id,
            @Valid @RequestBody ResourceRequest req,
            @AuthenticationPrincipal UserDetails user) {
        return resourceService.update(id, req, user.getUsername());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a resource (uploader only)", security = @SecurityRequirement(name = "bearerAuth"))
    public void delete(@PathVariable Long id, @AuthenticationPrincipal UserDetails user) {
        resourceService.delete(id, user.getUsername());
    }

    @PostMapping("/{id}/download")
    @Operation(summary = "Record a download and get the file URL")
    public ResourceResponse download(@PathVariable Long id) {
        return resourceService.recordDownload(id);
    }
}
