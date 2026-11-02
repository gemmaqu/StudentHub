package com.studenthub;

import com.studenthub.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceIntegrationTest extends BaseIntegrationTest {

    private static final String DOCX =
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    private HttpEntity<?> multipart(String token, long courseId, String filename, String contentType, byte[] data) {
        HttpHeaders fileHeaders = new HttpHeaders();
        fileHeaders.setContentType(MediaType.parseMediaType(contentType));
        ByteArrayResource file = new ByteArrayResource(data) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("file", new HttpEntity<>(file, fileHeaders));
        form.add("title", "Uploaded notes");
        form.add("resourceType", "LECTURE_NOTES");
        form.add("courseId", String.valueOf(courseId));
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        return new HttpEntity<>(form, headers);
    }

    private long storedFileCount() throws IOException {
        try (Stream<Path> files = Files.list(STORAGE_DIR)) {
            return files.count();
        }
    }

    private ResponseEntity<Map> upload(HttpEntity<?> request) {
        return rest.exchange("/api/resources/upload", HttpMethod.POST, request, Map.class);
    }

    @Test
    void upload_thenDownload_returnsSameBytesAndCountsTheDownload() {
        byte[] data = "midterm notes".getBytes(StandardCharsets.UTF_8);
        ResponseEntity<Map> created = upload(multipart(tokenFor(User.Role.STUDENT), createCourse(),
                "notes.docx", DOCX, data));

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Object id = created.getBody().get("id");
        assertThat(created.getBody().get("fileUrl")).isEqualTo("/api/resources/" + id + "/file");
        assertThat(created.getBody().get("fileSize")).isEqualTo(data.length);
        assertThat(created.getBody().get("fileType")).isEqualTo(DOCX);

        ResponseEntity<byte[]> file = rest.getForEntity("/api/resources/" + id + "/file", byte[].class);

        assertThat(file.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(file.getBody()).isEqualTo(data);
        assertThat(file.getHeaders().getContentDisposition().getType()).isEqualTo("attachment");
        assertThat(file.getHeaders().getContentDisposition().getFilename()).isEqualTo("notes.docx");
        assertThat(rest.getForObject("/api/resources/" + id, Map.class).get("downloadCount")).isEqualTo(1);
    }

    @Test
    void upload_withPathTraversalFilename_doesNotKeepThePath() {
        ResponseEntity<Map> created = upload(multipart(tokenFor(User.Role.STUDENT), createCourse(),
                "../../evil.txt", "text/plain", "x".getBytes(StandardCharsets.UTF_8)));

        ResponseEntity<byte[]> file = rest.getForEntity(
                "/api/resources/" + created.getBody().get("id") + "/file", byte[].class);

        assertThat(file.getHeaders().getContentDisposition().getFilename()).isEqualTo("evil.txt");
    }

    @Test
    void upload_emptyFile_returns400() {
        ResponseEntity<Map> resp = upload(multipart(tokenFor(User.Role.STUDENT), createCourse(),
                "empty.txt", "text/plain", new byte[0]));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void upload_withoutToken_returns401() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> resp = rest.exchange("/api/resources", HttpMethod.POST,
                new HttpEntity<>("{}", headers), String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void file_forLinkResource_returns404() {
        Map<String, Object> link = Map.of("title", "video", "fileUrl", "https://example.com/v",
                "resourceType", "VIDEO_LINK", "courseId", createCourse());
        Map<?, ?> body = rest.postForObject("/api/resources", authed(tokenFor(User.Role.STUDENT), link), Map.class);

        ResponseEntity<String> resp = rest.getForEntity("/api/resources/" + body.get("id") + "/file", String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void create_withNonHttpUrl_returns400() {
        Map<String, Object> link = Map.of("title", "bad", "fileUrl", "javascript:alert(1)",
                "resourceType", "OTHER", "courseId", createCourse());

        ResponseEntity<Map> resp = rest.exchange("/api/resources", HttpMethod.POST,
                authed(tokenFor(User.Role.STUDENT), link), Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void update_onUploadedResource_keepsItsFileUrl() {
        String token = tokenFor(User.Role.STUDENT);
        long courseId = createCourse();
        Map<?, ?> created = upload(multipart(token, courseId, "a.txt", "text/plain", new byte[]{1})).getBody();
        Map<String, Object> edit = Map.of("title", "renamed", "fileUrl", "https://example.com/other",
                "resourceType", "OTHER", "courseId", courseId);

        Map<?, ?> updated = rest.exchange("/api/resources/" + created.get("id"), HttpMethod.PUT,
                authed(token, edit), Map.class).getBody();

        assertThat(updated.get("title")).isEqualTo("renamed");
        assertThat(updated.get("fileUrl")).isEqualTo(created.get("fileUrl"));
    }

    @Test
    void delete_byAnotherUser_returns403() {
        long courseId = createCourse();
        Map<?, ?> created = upload(multipart(tokenFor(User.Role.STUDENT), courseId, "a.txt", "text/plain", new byte[]{1})).getBody();

        ResponseEntity<Map> resp = rest.exchange("/api/resources/" + created.get("id"), HttpMethod.DELETE,
                authed(tokenFor(User.Role.STUDENT), null), Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void delete_removesTheStoredFile() throws Exception {
        String token = tokenFor(User.Role.STUDENT);
        long filesBefore = storedFileCount();
        Object id = upload(multipart(token, createCourse(), "a.txt", "text/plain", new byte[]{1})).getBody().get("id");
        assertThat(storedFileCount()).isEqualTo(filesBefore + 1);

        rest.exchange("/api/resources/" + id, HttpMethod.DELETE, authed(token, null), Void.class);

        assertThat(storedFileCount()).isEqualTo(filesBefore);

        assertThat(rest.getForEntity("/api/resources/" + id + "/file", String.class).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void search_withoutKeyword_filtersByCourseAndType() {
        String token = tokenFor(User.Role.STUDENT);
        long courseId = createCourse();
        rest.postForObject("/api/resources", authed(token, Map.of("title", "exam", "fileUrl", "https://e.com/1",
                "resourceType", "PAST_EXAM", "courseId", courseId)), Map.class);
        rest.postForObject("/api/resources", authed(token, Map.of("title", "notes", "fileUrl", "https://e.com/2",
                "resourceType", "LECTURE_NOTES", "courseId", courseId)), Map.class);

        ResponseEntity<Map> resp = rest.getForEntity(
                "/api/resources/search?courseId=" + courseId + "&type=PAST_EXAM", Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((List<Map<String, Object>>) resp.getBody().get("content"))
                .extracting(r -> r.get("title")).containsExactly("exam");
    }

    @Test
    void search_byKeyword_matchesTitleCaseInsensitively() {
        String token = tokenFor(User.Role.STUDENT);
        long courseId = createCourse();
        rest.postForObject("/api/resources", authed(token, Map.of("title", "Dynamic Programming", "fileUrl", "https://e.com/3",
                "resourceType", "LECTURE_NOTES", "courseId", courseId)), Map.class);

        Map<?, ?> body = rest.getForObject("/api/resources/search?courseId=" + courseId + "&q=dynamic", Map.class);

        assertThat((List<?>) body.get("content")).hasSize(1);
    }

    @Test
    void search_withZeroPageSize_doesNotFail() {
        assertThat(rest.getForEntity("/api/resources/search?size=0", Map.class).getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }

    @Test
    void concurrentDownloads_areAllCounted() throws Exception {
        String token = tokenFor(User.Role.STUDENT);
        Map<String, Object> link = Map.of("title", "popular", "fileUrl", "https://e.com/p",
                "resourceType", "OTHER", "courseId", createCourse());
        Object id = rest.postForObject("/api/resources", authed(token, link), Map.class).get("id");

        ExecutorService pool = Executors.newFixedThreadPool(8);
        IntStream.range(0, 40).forEach(i -> pool.submit(() ->
                rest.postForObject("/api/resources/" + id + "/download", authed(token, null), Map.class)));
        pool.shutdown();
        assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        assertThat(rest.getForObject("/api/resources/" + id, Map.class).get("downloadCount")).isEqualTo(40);
    }
}
