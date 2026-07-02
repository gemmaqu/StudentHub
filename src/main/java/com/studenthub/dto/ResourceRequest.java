package com.studenthub.dto;

import com.studenthub.model.Resource.ResourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ResourceRequest {

    @NotBlank
    private String title;

    private String description;

    @NotBlank
    private String fileUrl;

    private String fileType;

    private Long fileSize;

    @NotNull
    private ResourceType resourceType;

    @NotNull
    private Long courseId;
}
