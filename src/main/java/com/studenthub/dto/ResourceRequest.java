package com.studenthub.dto;

import com.studenthub.model.Resource.ResourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResourceRequest {

    @NotBlank
    @Size(max = 255)
    private String title;

    private String description;

    @NotBlank
    @Size(max = 1024)
    @Pattern(regexp = "^https?://.+", message = "must be an http or https URL")
    private String fileUrl;

    private String fileType;

    private Long fileSize;

    @NotNull
    private ResourceType resourceType;

    @NotNull
    private Long courseId;
}
