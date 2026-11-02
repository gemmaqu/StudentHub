package com.studenthub;

import com.studenthub.config.DataSeeder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class SchemaIndexIntegrationTest extends BaseIntegrationTest {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void compositeIndexesMatchTheReadQueries() {
        assertThat(indexDef("idx_resources_course_created")).contains("(course_id, created_at DESC)");
        assertThat(indexDef("idx_resources_course_type_created")).contains("(course_id, resource_type, created_at DESC)");
        assertThat(indexDef("idx_resources_uploader_created")).contains("(uploader_id, created_at DESC)");
        assertThat(indexDef("idx_courses_department_code")).contains("(department, code)");
    }

    @Test
    void courseAndTypeFilter_usesACompositeIndexOnSeededData() {
        new DataSeeder(jdbc, 200, 12_000).seed();
        Long courseId = jdbc.queryForObject("SELECT id FROM courses WHERE code = 'SEED-7'", Long.class);

        String plan = String.join("\n", jdbc.queryForList(
                "EXPLAIN SELECT * FROM resources WHERE course_id = " + courseId
                        + " AND resource_type = 'PAST_EXAM' ORDER BY created_at DESC LIMIT 20", String.class));

        assertThat(plan).contains("idx_resources_course");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM resources WHERE title LIKE '%#%'", Long.class))
                .isGreaterThanOrEqualTo(12_000);
    }

    private String indexDef(String name) {
        return jdbc.queryForList("SELECT indexdef FROM pg_indexes WHERE indexname = ?", String.class, name)
                .stream().findFirst().orElse("<missing " + name + ">");
    }
}
