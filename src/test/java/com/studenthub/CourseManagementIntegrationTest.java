package com.studenthub;

import com.studenthub.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CourseManagementIntegrationTest extends BaseIntegrationTest {

    @Test
    void create_asStudent_returns403() {
        ResponseEntity<Map> resp = rest.exchange("/api/courses", HttpMethod.POST,
                authed(tokenFor(User.Role.STUDENT), Map.of("code", "NOPE1", "name", "Nope")), Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void create_withSameCodeInDifferentCase_returns409() {
        String admin = tokenFor(User.Role.ADMIN);
        String code = "dup" + System.nanoTime() % 100_000_000L;
        rest.exchange("/api/courses", HttpMethod.POST, authed(admin, Map.of("code", code, "name", "A")), Map.class);

        ResponseEntity<Map> resp = rest.exchange("/api/courses", HttpMethod.POST,
                authed(admin, Map.of("code", code.toUpperCase(), "name", "B")), Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void delete_asAdmin_removesCourseFromCache() {
        long id = createCourse();
        rest.getForObject("/api/courses/" + id, Map.class);

        rest.exchange("/api/courses/" + id, HttpMethod.DELETE, authed(tokenFor(User.Role.ADMIN), null), Void.class);

        assertThat(rest.getForEntity("/api/courses/" + id, Map.class).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void list_filteredByDepartment_returnsOnlyThatDepartment() {
        String admin = tokenFor(User.Role.ADMIN);
        String dept = "Dept" + System.nanoTime();
        rest.exchange("/api/courses", HttpMethod.POST,
                authed(admin, Map.of("code", "D" + System.nanoTime() % 100_000_000L, "name", "In", "department", dept)), Map.class);

        Map<?, ?> body = rest.getForObject("/api/courses?department=" + dept, Map.class);

        assertThat((List<?>) body.get("content")).hasSize(1);
    }

    @Test
    void swaggerUi_isPublic() {
        assertThat(rest.getForEntity("/swagger-ui.html", String.class).getStatusCode().is2xxSuccessful()).isTrue();
    }
}
