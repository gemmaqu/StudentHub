package com.studenthub.controller;

import com.studenthub.dto.PagedResponse;
import com.studenthub.dto.ResourceRequest;
import com.studenthub.dto.ResourceResponse;
import com.studenthub.model.Resource.ResourceType;
import com.studenthub.service.ResourceService;
import com.studenthub.service.StoredFile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

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
        return resourceService.search(q, courseId, type, Pages.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @GetMapping("/course/{courseId}")
    @Operation(summary = "List all resources for a course")
    public PagedResponse<ResourceResponse> getByCourse(
            @PathVariable Long courseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return resourceService.getByCourse(courseId, Pages.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a resource by ID")
    public ResourceResponse getById(@PathVariable Long id) {
        return resourceService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a resource hosted at an external URL", security = @SecurityRequirement(name = "bearerAuth"))
    public ResourceResponse create(
            @Valid @RequestBody ResourceRequest req,
            @AuthenticationPrincipal UserDetails user) {
        return resourceService.create(req, user.getUsername());
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Upload a file as a new resource", security = @SecurityRequirement(name = "bearerAuth"))
    public ResourceResponse upload(
            @RequestPart("file") MultipartFile file,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam ResourceType resourceType,
            @RequestParam Long courseId,
            @AuthenticationPrincipal UserDetails user) {
        return resourceService.upload(file, title, description, resourceType, courseId, user.getUsername());
    }

    @GetMapping("/{id}/file")
    @Operation(summary = "Download an uploaded file and count the download")
    public ResponseEntity<org.springframework.core.io.Resource> file(@PathVariable Long id) {
        StoredFile stored = resourceService.download(id);
        return ResponseEntity.ok()
                .contentType(contentType(stored.contentType()))
                .contentLength(stored.size())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(stored.filename(), StandardCharsets.UTF_8).build().toString())
                .body(stored.content());
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

    private static MediaType contentType(String value) {
        try {
            return MediaType.parseMediaType(value);
        } catch (InvalidMediaTypeException e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
