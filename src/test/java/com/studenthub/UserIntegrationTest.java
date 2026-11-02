package com.studenthub;

import com.studenthub.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class UserIntegrationTest extends BaseIntegrationTest {

    @Test
    void me_returnsProfileWithoutPassword() {
        ResponseEntity<Map> resp = rest.exchange("/api/users/me", HttpMethod.GET,
                authed(tokenFor(User.Role.INSTRUCTOR), null), Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().get("role")).isEqualTo("INSTRUCTOR");
        assertThat(resp.getBody()).doesNotContainKey("password");
    }

    @Test
    void myResources_listsOnlyOwnUploads() {
        String mine = tokenFor(User.Role.STUDENT);
        String other = tokenFor(User.Role.STUDENT);
        long courseId = createCourse();
        rest.postForObject("/api/resources", authed(mine, Map.of("title", "mine", "fileUrl", "https://e.com/m",
                "resourceType", "OTHER", "courseId", courseId)), Map.class);
        rest.postForObject("/api/resources", authed(other, Map.of("title", "theirs", "fileUrl", "https://e.com/t",
                "resourceType", "OTHER", "courseId", courseId)), Map.class);

        Map<?, ?> body = rest.exchange("/api/users/me/resources", HttpMethod.GET, authed(mine, null), Map.class).getBody();

        assertThat((List<Map<String, Object>>) body.get("content"))
                .extracting(r -> r.get("title")).containsExactly("mine");
    }
}
