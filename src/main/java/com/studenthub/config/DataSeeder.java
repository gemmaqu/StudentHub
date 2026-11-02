package com.studenthub.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Loads synthetic data for local benchmarking. Runs only under the "seed" profile, and only once. */
@Component
@Profile("seed")
public class DataSeeder implements ApplicationRunner {

    private static final int USERS = 50;

    private final JdbcTemplate jdbc;
    private final int courses;
    private final int resources;

    public DataSeeder(JdbcTemplate jdbc,
                      @Value("${studenthub.seed.courses}") int courses,
                      @Value("${studenthub.seed.resources}") int resources) {
        this.jdbc = jdbc;
        this.courses = courses;
        this.resources = resources;
    }

    @Override
    public void run(ApplicationArguments args) {
        seed();
    }

    public void seed() {
        Integer existing = jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE username LIKE 'seed-user-%'", Integer.class);
        if (existing != null && existing > 0) return;

        // '!' is not a valid BCrypt hash, so seeded accounts cannot log in.
        jdbc.update("""
                INSERT INTO users (username, email, password, full_name)
                SELECT 'seed-user-' || i, 'seed-user-' || i || '@example.com', '!', 'Seed User ' || i
                FROM generate_series(1, ?) i
                """, USERS);
        jdbc.update("""
                INSERT INTO courses (code, name, description, department)
                SELECT 'SEED-' || i,
                       'Course ' || i,
                       'Synthetic course ' || i,
                       (ARRAY['Computer Science','Mathematics','Physics','Economics','Biology'])[1 + i % 5]
                FROM generate_series(1, ?) i
                """, courses);
        jdbc.update("""
                INSERT INTO resources (title, description, file_url, file_type, file_size, resource_type,
                                       course_id, uploader_id, created_at, updated_at)
                SELECT (ARRAY['Lecture notes','Past exam','Assignment','Textbook excerpt','Video'])[1 + i % 5]
                           || ' on ' ||
                           (ARRAY['graphs','dynamic programming','recursion','databases','networks',
                                  'operating systems','linear algebra','probability'])[1 + i % 8]
                           || ' #' || i,
                       'Synthetic resource ' || i,
                       'https://example.com/files/' || i,
                       'application/pdf',
                       1024 + i,
                       (ARRAY['LECTURE_NOTES','PAST_EXAM','ASSIGNMENT','TEXTBOOK','VIDEO_LINK'])[1 + i % 5],
                       c.ids[1 + i % array_length(c.ids, 1)],
                       u.ids[1 + i % array_length(u.ids, 1)],
                       NOW() - i * INTERVAL '1 minute',
                       NOW()
                FROM generate_series(1, ?) i,
                     (SELECT array_agg(id) AS ids FROM courses WHERE code LIKE 'SEED-%') c,
                     (SELECT array_agg(id) AS ids FROM users WHERE username LIKE 'seed-user-%') u
                """, resources);
        jdbc.execute("ANALYZE");
    }
}
