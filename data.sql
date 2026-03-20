-- ============================================================
-- Course App — Database Schema & Seed Data
-- PostgreSQL 14+
-- ============================================================

-- ── Extensions ───────────────────────────────────────────────
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ── Tables ───────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS users (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name       VARCHAR(150) NOT NULL,
    username   VARCHAR(80)  NOT NULL UNIQUE,
    email      VARCHAR(200) NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    role       VARCHAR(20)  NOT NULL DEFAULT 'student',
    bio        TEXT,
    photo      VARCHAR(500),
    is_active  BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id            UUID  PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID  NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    auth_token    TEXT  NOT NULL,
    refresh_token TEXT  NOT NULL,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS categories (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    icon        VARCHAR(100),
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS courses (
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    instructor_id  UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    category_id    UUID         REFERENCES categories(id) ON DELETE SET NULL,
    title          VARCHAR(200) NOT NULL,
    slug           VARCHAR(250) NOT NULL UNIQUE,
    description    TEXT         NOT NULL,
    thumbnail      VARCHAR(500),
    level          VARCHAR(20)  NOT NULL DEFAULT 'beginner',
    language       VARCHAR(50)  NOT NULL DEFAULT 'Indonesia',
    price          BIGINT       NOT NULL DEFAULT 0,
    discount_price BIGINT,
    status         VARCHAR(20)  NOT NULL DEFAULT 'draft',
    total_duration INTEGER      NOT NULL DEFAULT 0,
    total_lessons  INTEGER      NOT NULL DEFAULT 0,
    total_students INTEGER      NOT NULL DEFAULT 0,
    avg_rating     DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    total_reviews  INTEGER      NOT NULL DEFAULT 0,
    created_at     TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS lessons (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id    UUID         NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    title        VARCHAR(200) NOT NULL,
    description  TEXT,
    content_type VARCHAR(20)  NOT NULL DEFAULT 'video',
    content_url  TEXT,
    content      TEXT,
    duration     INTEGER      NOT NULL DEFAULT 0,
    order_index  INTEGER      NOT NULL DEFAULT 0,
    is_free      BOOLEAN      NOT NULL DEFAULT FALSE,
    is_published BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS enrollments (
    id           UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    course_id    UUID      NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    status       VARCHAR(20) NOT NULL DEFAULT 'active',
    progress     INTEGER   NOT NULL DEFAULT 0,
    paid_amount  BIGINT    NOT NULL DEFAULT 0,
    enrolled_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMP,
    updated_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, course_id)
);

CREATE TABLE IF NOT EXISTS lesson_progress (
    id              UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    lesson_id       UUID      NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,
    course_id       UUID      NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    is_completed    BOOLEAN   NOT NULL DEFAULT FALSE,
    watched_seconds INTEGER   NOT NULL DEFAULT 0,
    completed_at    TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, lesson_id)
);

CREATE TABLE IF NOT EXISTS reviews (
    id         UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    course_id  UUID      NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    rating     INTEGER   NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment    TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, course_id)
);

-- ── Indexes (performa query) ──────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_courses_instructor ON courses(instructor_id);
CREATE INDEX IF NOT EXISTS idx_courses_category   ON courses(category_id);
CREATE INDEX IF NOT EXISTS idx_courses_status     ON courses(status);
CREATE INDEX IF NOT EXISTS idx_courses_slug       ON courses(slug);
CREATE INDEX IF NOT EXISTS idx_lessons_course     ON lessons(course_id);
CREATE INDEX IF NOT EXISTS idx_lessons_order      ON lessons(course_id, order_index);
CREATE INDEX IF NOT EXISTS idx_enrollments_user   ON enrollments(user_id);
CREATE INDEX IF NOT EXISTS idx_enrollments_course ON enrollments(course_id);
CREATE INDEX IF NOT EXISTS idx_progress_user      ON lesson_progress(user_id, course_id);
CREATE INDEX IF NOT EXISTS idx_reviews_course     ON reviews(course_id);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user ON refresh_tokens(user_id);

-- ── Seed Data ─────────────────────────────────────────────────
-- Password untuk semua akun seed: "Password123"
-- Hash bcrypt di atas sudah pre-computed

INSERT INTO users (id, name, username, email, password, role, bio) VALUES
(
    'a1111111-0000-0000-0000-000000000001',
    'Budi Instructor',
    'budi_instructor',
    'budi@example.com',
    '$2a$12$LMqAtZHRopGzRb0xN9tAl.qMQ2.TF.UEPCzY3FRfEdMDIqf7a7Lue',
    'instructor',
    'Instruktur berpengalaman di bidang Web dan Mobile Development.'
),
(
    'a2222222-0000-0000-0000-000000000002',
    'Siti Student',
    'siti_student',
    'siti@example.com',
    '$2a$12$LMqAtZHRopGzRb0xN9tAl.qMQ2.TF.UEPCzY3FRfEdMDIqf7a7Lue',
    'student',
    'Mahasiswa yang sedang belajar programming.'
)
ON CONFLICT (username) DO NOTHING;

INSERT INTO categories (id, name, description, icon) VALUES
('c1111111-0000-0000-0000-000000000001', 'Mobile Development', 'Pelajari pengembangan aplikasi mobile Android dan iOS', '📱'),
('c2222222-0000-0000-0000-000000000002', 'Web Development',    'Frontend, backend, dan fullstack web development',      '🌐'),
('c3333333-0000-0000-0000-000000000003', 'Data Science',       'Analisis data, machine learning, dan AI',               '📊'),
('c4444444-0000-0000-0000-000000000004', 'UI/UX Design',       'Desain antarmuka dan pengalaman pengguna yang baik',     '🎨'),
('c5555555-0000-0000-0000-000000000005', 'Cloud & DevOps',     'Cloud computing, CI/CD, Docker, dan Kubernetes',         '☁️')
ON CONFLICT (name) DO NOTHING;
