package com.studenthub.service;

import com.studenthub.dto.AuthResponse;
import com.studenthub.dto.LoginRequest;
import com.studenthub.dto.RegisterRequest;
import com.studenthub.exception.ConflictException;
import com.studenthub.model.User;
import com.studenthub.repository.UserRepository;
import com.studenthub.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authManager;
    private final UserDetailsService userDetailsService;

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByUsername(req.getUsername()))
            throw new ConflictException("Username already taken");
        if (userRepository.existsByEmail(req.getEmail()))
            throw new ConflictException("Email already registered");

        User user = User.builder()
                .username(req.getUsername())
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .fullName(req.getFullName())
                .build();
        userRepository.save(user);

        UserDetails details = userDetailsService.loadUserByUsername(user.getUsername());
        return new AuthResponse(jwtService.generateToken(details), user.getUsername(), user.getRole().name());
    }

    public AuthResponse login(LoginRequest req) {
        authManager.authenticate(new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword()));
        UserDetails details = userDetailsService.loadUserByUsername(req.getUsername());
        User user = userRepository.findByUsername(req.getUsername()).orElseThrow();
        return new AuthResponse(jwtService.generateToken(details), user.getUsername(), user.getRole().name());
    }
}
