CREATE EXTENSION IF NOT EXISTS pg_trgm;

ALTER TABLE resources
    ADD COLUMN storage_key       VARCHAR(64),
    ADD COLUMN original_filename VARCHAR(255),
    ALTER COLUMN file_type TYPE VARCHAR(150);

-- Replaced by composite indexes below. idx_courses_code duplicated the UNIQUE constraint,
-- and no query used the tsvector index.
DROP INDEX idx_resources_course_id;
DROP INDEX idx_resources_uploader_id;
DROP INDEX idx_resources_title_trgm;
DROP INDEX idx_resources_type;
DROP INDEX idx_courses_code;

-- Each index serves a filter plus the newest-first sort, so paged reads stop after one page.
CREATE INDEX idx_resources_created             ON resources (created_at DESC);
CREATE INDEX idx_resources_course_created      ON resources (course_id, created_at DESC);
CREATE INDEX idx_resources_course_type_created ON resources (course_id, resource_type, created_at DESC);
CREATE INDEX idx_resources_type_created        ON resources (resource_type, created_at DESC);
CREATE INDEX idx_resources_uploader_created    ON resources (uploader_id, created_at DESC);
CREATE INDEX idx_courses_department_code       ON courses (department, code);

-- Support the case-insensitive LIKE '%keyword%' search.
CREATE INDEX idx_resources_title_trgm       ON resources USING gin (lower(title) gin_trgm_ops);
CREATE INDEX idx_resources_description_trgm ON resources USING gin (lower(description) gin_trgm_ops);
