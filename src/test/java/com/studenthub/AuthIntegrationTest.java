package com.studenthub;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studenthub.dto.LoginRequest;
import com.studenthub.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AuthIntegrationTest extends BaseIntegrationTest {

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void registerAndLogin_shouldReturnJwt() {
        RegisterRequest reg = new RegisterRequest();
        reg.setUsername("testuser");
        reg.setEmail("test@example.com");
        reg.setPassword("password123");
        reg.setFullName("Test User");

        ResponseEntity<Map> registerResp = restTemplate.postForEntity("/api/auth/register", reg, Map.class);
        assertThat(registerResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(registerResp.getBody()).containsKey("token");

        LoginRequest login = new LoginRequest();
        login.setUsername("testuser");
        login.setPassword("password123");

        ResponseEntity<Map> loginResp = restTemplate.postForEntity("/api/auth/login", login, Map.class);
        assertThat(loginResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(loginResp.getBody()).containsKey("token");
    }

    @Test
    void register_duplicateUsername_shouldReturn409() {
        RegisterRequest reg = new RegisterRequest();
        reg.setUsername("duplicate");
        reg.setEmail("dup1@example.com");
        reg.setPassword("password123");

        restTemplate.postForEntity("/api/auth/register", reg, Map.class);

        reg.setEmail("dup2@example.com");
        ResponseEntity<Map> resp = restTemplate.postForEntity("/api/auth/register", reg, Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }
}
