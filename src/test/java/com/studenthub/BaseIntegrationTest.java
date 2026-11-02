package com.studenthub;

import com.redis.testcontainers.RedisContainer;
import com.studenthub.dto.RegisterRequest;
import com.studenthub.model.User;
import com.studenthub.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class BaseIntegrationTest {

    // Started once per JVM: Spring caches the context between test classes, so
    // per-class @Container lifecycles would leave that context pointing at stopped containers.
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("studenthub_test")
            .withUsername("test")
            .withPassword("test");

    static final RedisContainer redis = new RedisContainer("redis:7-alpine");

    protected static final Path STORAGE_DIR = createStorageDir();

    private static Path createStorageDir() {
        try {
            return Files.createTempDirectory("studenthub-uploads");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static {
        postgres.start();
        redis.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("jwt.secret", () -> "dGVzdC1vbmx5LWp3dC1zZWNyZXQta2V5LWZvci1pbnRlZ3JhdGlvbi10ZXN0cw==");
        registry.add("studenthub.storage.dir", STORAGE_DIR::toString);
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired
    protected TestRestTemplate rest;

    @Autowired
    protected UserRepository users;

    protected String tokenFor(User.Role role) {
        RegisterRequest reg = new RegisterRequest();
        reg.setUsername("u" + System.nanoTime());
        reg.setEmail(reg.getUsername() + "@test.com");
        reg.setPassword("password123");
        Map<?, ?> body = rest.postForObject("/api/auth/register", reg, Map.class);
        User user = users.findByUsername(reg.getUsername()).orElseThrow();
        user.setRole(role);
        users.save(user);
        return (String) body.get("token");
    }

    protected HttpEntity<?> authed(String token, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return new HttpEntity<>(body, headers);
    }

    protected long createCourse() {
        String code = "C" + System.nanoTime() % 1_000_000_000L;
        Map<String, String> req = Map.of("code", code, "name", "Course " + code, "department", "CS");
        Map<?, ?> body = rest.postForObject("/api/courses", authed(tokenFor(User.Role.ADMIN), req), Map.class);
        return ((Number) body.get("id")).longValue();
    }
}
