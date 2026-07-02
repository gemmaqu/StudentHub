package com.studenthub.dto;

import com.studenthub.model.Course;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CourseResponse {
    private Long id;
    private String code;
    private String name;
    private String description;
    private String department;
    private int resourceCount;
    private LocalDateTime createdAt;

    public static CourseResponse from(Course c) {
        return CourseResponse.builder()
                .id(c.getId())
                .code(c.getCode())
                .name(c.getName())
                .description(c.getDescription())
                .department(c.getDepartment())
                .resourceCount(c.getResources() == null ? 0 : c.getResources().size())
                .createdAt(c.getCreatedAt())
                .build();
    }
}
