package com.studenthub.dto;

import com.studenthub.model.User;

import java.time.LocalDateTime;

public record UserResponse(Long id, String username, String email, String fullName, User.Role role, LocalDateTime createdAt) {

    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getUsername(), u.getEmail(), u.getFullName(), u.getRole(), u.getCreatedAt());
    }
}
