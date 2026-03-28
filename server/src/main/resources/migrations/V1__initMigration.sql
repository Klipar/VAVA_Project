-- ============================================================
-- V1__initMigration.sql
-- Initial schema migration
-- ============================================================

-- ENUMS
CREATE TYPE user_role AS ENUM ('manager', 'team_leader', 'worker');
CREATE TYPE project_user_role AS ENUM ('master', 'slave');
CREATE TYPE project_status AS ENUM ('active', 'completed', 'archived');
CREATE TYPE task_status AS ENUM ('backlog', 'assigned', 'in_progress', 'ready', 'in_review', 'done');

-- TABLES
CREATE TABLE "user" (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    nickname VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role user_role NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE project (
    id SERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    deadline TIMESTAMPTZ,
    status project_status NOT NULL DEFAULT 'active'
);

CREATE TABLE user_project (
    user_id INT NOT NULL,
    project_id INT NOT NULL,
    role project_user_role NOT NULL,
    joined_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    left_at TIMESTAMPTZ,

    FOREIGN KEY (user_id) REFERENCES "user"(id) ON DELETE CASCADE,
    FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE CASCADE
);

CREATE TABLE task (
    id SERIAL PRIMARY KEY,
    project_id INT NOT NULL,
    created_by INT NOT NULL,
    assigned_to INT,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    priority SMALLINT NOT NULL DEFAULT 0 CHECK (priority BETWEEN 0 AND 4),
    status task_status NOT NULL DEFAULT 'backlog',
    deadline TIMESTAMPTZ,

    FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES "user"(id) ON DELETE RESTRICT,
    FOREIGN KEY (assigned_to)  REFERENCES "user"(id) ON DELETE SET NULL
);

CREATE TABLE notification (
    id SERIAL PRIMARY KEY,
    message TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);


CREATE TABLE user_notification (
    user_id INT NOT NULL,
    notification_id INT NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,

    FOREIGN KEY (user_id) REFERENCES "user"(id) ON DELETE CASCADE,
    FOREIGN KEY (notification_id) REFERENCES notification(id) ON DELETE CASCADE
);
