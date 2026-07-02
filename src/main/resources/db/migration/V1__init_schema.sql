CREATE TABLE users (
    id          BIGSERIAL PRIMARY KEY,
    username    VARCHAR(50)  NOT NULL UNIQUE,
    email       VARCHAR(255) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    full_name   VARCHAR(150),
    role        VARCHAR(20)  NOT NULL DEFAULT 'STUDENT',
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE courses (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(20)  NOT NULL UNIQUE,
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    department  VARCHAR(100),
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE resources (
    id           BIGSERIAL PRIMARY KEY,
    title        VARCHAR(255)  NOT NULL,
    description  TEXT,
    file_url     VARCHAR(1024) NOT NULL,
    file_type    VARCHAR(50),
    file_size    BIGINT,
    resource_type VARCHAR(50) NOT NULL,
    course_id    BIGINT        NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    uploader_id  BIGINT        NOT NULL REFERENCES users(id),
    download_count BIGINT      NOT NULL DEFAULT 0,
    created_at   TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_resources_course_id   ON resources(course_id);
CREATE INDEX idx_resources_uploader_id ON resources(uploader_id);
CREATE INDEX idx_resources_title_trgm  ON resources USING gin(to_tsvector('english', title || ' ' || COALESCE(description, '')));
CREATE INDEX idx_resources_type        ON resources(resource_type);
CREATE INDEX idx_courses_code          ON courses(code);
