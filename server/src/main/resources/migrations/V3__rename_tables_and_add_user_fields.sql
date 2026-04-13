-- ============================================================
-- V3__rename_tables_and_add_user_fields.sql
-- ============================================================

-- RENAME TABLES

ALTER TABLE "user" RENAME TO users;
ALTER TABLE project RENAME TO projects;
ALTER TABLE user_project RENAME TO user_projects;
ALTER TABLE task RENAME TO tasks;
ALTER TABLE notification RENAME TO notifications;
ALTER TABLE user_notification RENAME TO users_notifications;

-- UPDATE FOREIGN KEYS

-- user_projects
ALTER TABLE user_projects
    DROP CONSTRAINT IF EXISTS user_project_user_id_fkey,
    ADD CONSTRAINT user_projects_user_id_fkey
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE user_projects
    DROP CONSTRAINT IF EXISTS user_project_project_id_fkey,
    ADD CONSTRAINT user_projects_project_id_fkey
        FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE;

-- tasks
ALTER TABLE tasks
    DROP CONSTRAINT IF EXISTS task_project_id_fkey,
    ADD CONSTRAINT tasks_project_id_fkey
        FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE;

ALTER TABLE tasks
    DROP CONSTRAINT IF EXISTS task_created_by_fkey,
    ADD CONSTRAINT tasks_created_by_fkey
        FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE RESTRICT;

ALTER TABLE tasks
    DROP CONSTRAINT IF EXISTS task_assigned_to_fkey,
    ADD CONSTRAINT tasks_assigned_to_fkey
        FOREIGN KEY (assigned_to) REFERENCES users(id) ON DELETE SET NULL;

-- user_notifications
ALTER TABLE users_notifications
    DROP CONSTRAINT IF EXISTS user_notification_user_id_fkey,
    ADD CONSTRAINT users_notifications_user_id_fkey
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE users_notifications
    DROP CONSTRAINT IF EXISTS user_notification_notification_id_fkey,
    ADD CONSTRAINT users_notifications_notification_id_fkey
        FOREIGN KEY (notification_id) REFERENCES notifications(id) ON DELETE CASCADE;

-- ADD NEW COLUMNS TO USERS
ALTER TABLE users
    ADD COLUMN skills TEXT;

-- RENAME COLUMN
ALTER TABLE users RENAME COLUMN password TO password_hash;