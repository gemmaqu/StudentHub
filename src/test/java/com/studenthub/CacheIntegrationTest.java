package com.studenthub;

import com.studenthub.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CacheIntegrationTest extends BaseIntegrationTest {

    @Autowired
    CacheManager cacheManager;

    private Map<String, Object> linkResource(long courseId, String title) {
        return Map.of("title", title, "fileUrl", "https://example.com/" + title,
                "resourceType", "LECTURE_NOTES", "courseId", courseId);
    }

    private long createResource(String token, long courseId, String title) {
        Map<?, ?> body = rest.postForObject("/api/resources", authed(token, linkResource(courseId, title)), Map.class);
        return ((Number) body.get("id")).longValue();
    }

    @Test
    void courseById_servedTwice_secondResponseComesFromRedis() {
        long id = createCourse();

        ResponseEntity<Map> first = rest.getForEntity("/api/courses/" + id, Map.class);
        ResponseEntity<Map> second = rest.getForEntity("/api/courses/" + id, Map.class);

        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(second.getBody()).isEqualTo(first.getBody());
        assertThat(cacheManager.getCache("courses").get(id)).isNotNull();
    }

    @Test
    void pagedSearch_servedTwice_roundTripsThroughRedis() {
        String token = tokenFor(User.Role.STUDENT);
        long courseId = createCourse();
        createResource(token, courseId, "paged-one");

        String url = "/api/resources/search?courseId=" + courseId;
        ResponseEntity<Map> first = rest.getForEntity(url, Map.class);
        ResponseEntity<Map> second = rest.getForEntity(url, Map.class);

        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((List<?>) second.getBody().get("content")).hasSize(1);
        assertThat(second.getBody()).isEqualTo(first.getBody());
    }

    @Test
    void differentPageSizes_doNotShareACacheEntry() {
        String token = tokenFor(User.Role.STUDENT);
        long courseId = createCourse();
        createResource(token, courseId, "size-a");
        createResource(token, courseId, "size-b");

        String base = "/api/resources/course/" + courseId + "?size=";
        assertThat((List<?>) rest.getForObject(base + "1", Map.class).get("content")).hasSize(1);
        assertThat((List<?>) rest.getForObject(base + "2", Map.class).get("content")).hasSize(2);
    }

    @Test
    void updatingResource_evictsCachedRead() {
        String token = tokenFor(User.Role.STUDENT);
        long courseId = createCourse();
        long id = createResource(token, courseId, "before");
        rest.getForObject("/api/resources/" + id, Map.class);

        rest.exchange("/api/resources/" + id, HttpMethod.PUT,
                authed(token, linkResource(courseId, "after")), Map.class);

        assertThat(rest.getForObject("/api/resources/" + id, Map.class).get("title")).isEqualTo("after");
    }

    @Test
    void creatingResource_refreshesCachedCourseResourceCount() {
        String token = tokenFor(User.Role.STUDENT);
        long courseId = createCourse();
        assertThat(rest.getForObject("/api/courses/" + courseId, Map.class).get("resourceCount")).isEqualTo(0);

        createResource(token, courseId, "counted");

        assertThat(rest.getForObject("/api/courses/" + courseId, Map.class).get("resourceCount")).isEqualTo(1);
    }

    @Test
    void recordingDownload_refreshesCachedResource() {
        String token = tokenFor(User.Role.STUDENT);
        long id = createResource(token, createCourse(), "downloaded");
        rest.getForObject("/api/resources/" + id, Map.class);

        rest.postForObject("/api/resources/" + id + "/download", authed(token, null), Map.class);

        assertThat(rest.getForObject("/api/resources/" + id, Map.class).get("downloadCount")).isEqualTo(1);
    }
}
