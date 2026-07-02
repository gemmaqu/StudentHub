package com.studenthub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CourseRequest {

    @NotBlank
    @Size(max = 20)
    private String code;

    @NotBlank
    private String name;

    private String description;

    private String department;
}
