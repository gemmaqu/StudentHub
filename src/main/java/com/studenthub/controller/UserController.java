package com.studenthub.controller;

import com.studenthub.dto.PagedResponse;
import com.studenthub.dto.ResourceResponse;
import com.studenthub.dto.UserResponse;
import com.studenthub.exception.ResourceNotFoundException;
import com.studenthub.repository.UserRepository;
import com.studenthub.service.ResourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
@Tag(name = "Users")
public class UserController {

    private final UserRepository userRepository;
    private final ResourceService resourceService;

    @GetMapping
    @Operation(summary = "Get the current user's profile", security = @SecurityRequirement(name = "bearerAuth"))
    public UserResponse me(@AuthenticationPrincipal UserDetails user) {
        return userRepository.findByUsername(user.getUsername())
                .map(UserResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + user.getUsername()));
    }

    @GetMapping("/resources")
    @Operation(summary = "List resources uploaded by the current user", security = @SecurityRequirement(name = "bearerAuth"))
    public PagedResponse<ResourceResponse> myResources(
            @AuthenticationPrincipal UserDetails user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return resourceService.getByUploader(user.getUsername(),
                Pages.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }
}
