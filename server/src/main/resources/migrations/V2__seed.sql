-- ============================================================
-- V2__seedData.sql
-- Seed data for initial testing
-- ============================================================

-- USERS
-- default password is:
-- qwerty
INSERT INTO "user" (name, nickname, email, password, role) VALUES
('Ivan Petrenko', 'ivan', 'ivan@example.com', '65e84be33532fb784c48129675f9eff3a682b27168c0ea744b2cf58ee02337c5', 'manager'),
('Olena Shevchenko', 'olena', 'olena@example.com', '65e84be33532fb784c48129675f9eff3a682b27168c0ea744b2cf58ee02337c5', 'team_leader'),
('Andrii Kovalenko', 'andrii', 'andrii@example.com', '65e84be33532fb784c48129675f9eff3a682b27168c0ea744b2cf58ee02337c5', 'worker'),
('Maria Bondar', 'maria', 'maria@example.com', '65e84be33532fb784c48129675f9eff3a682b27168c0ea744b2cf58ee02337c5', 'worker'),
('Taras Melnyk', 'taras', 'taras@example.com', '65e84be33532fb784c48129675f9eff3a682b27168c0ea744b2cf58ee02337c5', 'worker');

-- PROJECTS
INSERT INTO project (title, description, deadline, status) VALUES
('CRM System', 'Internal CRM development', NOW() + INTERVAL '30 days', 'active'),
('Website Redesign', 'Update company website', NOW() + INTERVAL '20 days', 'active'),
('Mobile App', 'iOS and Android app', NOW() + INTERVAL '60 days', 'active'),
('Legacy Migration', 'Migrate old system', NOW() + INTERVAL '10 days', 'completed'),
('Analytics Tool', 'Build analytics dashboard', NOW() + INTERVAL '40 days', 'active');

-- USER_PROJECT
INSERT INTO user_project (user_id, project_id, role) VALUES
(1, 1, 'master'),
(2, 1, 'slave'),
(3, 1, 'slave'),
(2, 2, 'master'),
(4, 2, 'slave'),
(5, 3, 'slave'),
(1, 3, 'master');

-- TASKS
INSERT INTO task (project_id, created_by, assigned_to, title, description, priority, status, deadline) VALUES
(1, 1, 3, 'Setup DB', 'Create PostgreSQL schema', 3, 'in_progress', NOW() + INTERVAL '5 days'),
(1, 2, 4, 'API Development', 'Develop REST API', 4, 'assigned', NOW() + INTERVAL '10 days'),
(2, 2, 4, 'Design UI', 'Create Figma layouts', 2, 'in_review', NOW() + INTERVAL '7 days'),
(3, 1, 5, 'Auth Module', 'Implement authentication', 4, 'backlog', NOW() + INTERVAL '15 days'),
(3, 1, NULL, 'Push Notifications', 'Implement notifications', 1, 'backlog', NOW() + INTERVAL '20 days');

-- NOTIFICATIONS
INSERT INTO notification (message) VALUES
('New task assigned'),
('Project deadline updated'),
('Task status changed'),
('New comment added'),
('User joined project');

-- USER_NOTIFICATION
INSERT INTO user_notification (user_id, notification_id, is_read) VALUES
(1, 1, FALSE),
(2, 2, TRUE),
(3, 3, FALSE),
(4, 4, FALSE),
(5, 5, TRUE);
