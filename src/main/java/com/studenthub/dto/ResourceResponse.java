package com.studenthub.dto;

import com.studenthub.model.Resource;
import com.studenthub.model.Resource.ResourceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceResponse {
    private Long id;
    private String title;
    private String description;
    private String fileUrl;
    private String fileType;
    private Long fileSize;
    private ResourceType resourceType;
    private Long courseId;
    private String courseCode;
    private String courseName;
    private Long uploaderId;
    private String uploaderUsername;
    private Long downloadCount;
    private LocalDateTime createdAt;

    public static ResourceResponse from(Resource r) {
        return ResourceResponse.builder()
                .id(r.getId())
                .title(r.getTitle())
                .description(r.getDescription())
                .fileUrl(r.getFileUrl())
                .fileType(r.getFileType())
                .fileSize(r.getFileSize())
                .resourceType(r.getResourceType())
                .courseId(r.getCourse().getId())
                .courseCode(r.getCourse().getCode())
                .courseName(r.getCourse().getName())
                .uploaderId(r.getUploader().getId())
                .uploaderUsername(r.getUploader().getUsername())
                .downloadCount(r.getDownloadCount())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
