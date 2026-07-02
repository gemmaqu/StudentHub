package com.studenthub;

import com.studenthub.dto.CourseRequest;
import com.studenthub.dto.LoginRequest;
import com.studenthub.dto.RegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CourseIntegrationTest extends BaseIntegrationTest {

    @Autowired
    TestRestTemplate restTemplate;

    private String adminToken;

    @BeforeEach
    void setup() {
        RegisterRequest reg = new RegisterRequest();
        reg.setUsername("admin_course_" + System.nanoTime());
        reg.setEmail(reg.getUsername() + "@test.com");
        reg.setPassword("password123");

        ResponseEntity<Map> resp = restTemplate.postForEntity("/api/auth/register", reg, Map.class);
        adminToken = (String) resp.getBody().get("token");
    }

    @Test
    void listCourses_publicEndpoint_shouldReturn200() {
        ResponseEntity<Map> resp = restTemplate.getForEntity("/api/courses", Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void searchCourses_shouldReturn200() {
        ResponseEntity<Map> resp = restTemplate.getForEntity("/api/courses/search?q=cs", Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
