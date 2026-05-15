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

-- DONE_TASKS
INSERT INTO task (project_id, created_by, assigned_to, title, description, priority, status, deadline) VALUES
(1, 1, 3, 'Database Indexing', 'Add indexes to improve query performance on users and tasks tables', 3, 'done', NOW() - INTERVAL '10 days'),
(1, 2, 4, 'JWT Authentication', 'Implement JWT token generation and validation', 4, 'done', NOW() - INTERVAL '8 days'),
(1, 1, 5, 'Unit Tests for API', 'Write JUnit tests for all REST endpoints', 3, 'done', NOW() - INTERVAL '6 days'),
(1, 2, 3, 'Docker Setup', 'Containerize the backend application with Docker', 2, 'done', NOW() - INTERVAL '14 days'),
(2, 2, 4, 'Landing Page', 'Build responsive landing page from Figma mockup', 3, 'done', NOW() - INTERVAL '5 days'),
(2, 2, 5, 'Component Library', 'Set up reusable UI component library', 2, 'done', NOW() - INTERVAL '9 days'),
(2, 1, 3, 'Dark Mode Support', 'Implement dark/light theme toggle', 1, 'done', NOW() - INTERVAL '3 days'),
(3, 1, 4, 'OAuth Integration', 'Add Google and GitHub OAuth login', 4, 'done', NOW() - INTERVAL '12 days'),
(3, 1, 5, 'Password Reset Flow', 'Implement forgot password email flow', 3, 'done', NOW() - INTERVAL '7 days'),
(3, 2, 3, 'Rate Limiting', 'Add rate limiting middleware to prevent abuse', 2, 'done', NOW() - INTERVAL '4 days'),
(1, 1, 4, 'CI/CD Pipeline', 'Configure GitHub Actions for automated deployment', 4, 'done', NOW() - INTERVAL '11 days'),
(2, 2, 3, 'Accessibility Audit', 'Fix WCAG 2.1 compliance issues across all pages', 2, 'done', NOW() - INTERVAL '2 days'),
(1, 1, 3, 'Schema Design', 'Design normalized database schema for all core entities', 4, 'done', NOW() - INTERVAL '30 days'),
(1, 1, 3, 'Migration Scripts', 'Write Flyway migration scripts for initial schema', 3, 'done', NOW() - INTERVAL '27 days'),
(1, 1, 3, 'Connection Pooling', 'Configure HikariCP connection pool for optimal performance', 3, 'done', NOW() - INTERVAL '24 days'),
(1, 1, 3, 'Query Optimization', 'Analyze and optimize slow queries using EXPLAIN ANALYZE', 4, 'done', NOW() - INTERVAL '21 days'),
(1, 1, 1, 'Requirements Gathering', 'Collect and document business requirements from stakeholders', 3, 'done', NOW() - INTERVAL '35 days'),
(1, 1, 1, 'Project Kickoff', 'Organize kickoff meeting and define team responsibilities', 2, 'done', NOW() - INTERVAL '32 days'),
(1, 2, 2, 'Code Review Standards', 'Define and document code review guidelines for the team', 3, 'done', NOW() - INTERVAL '33 days'),
(1, 2, 2, 'Architecture Design', 'Design microservice architecture and document component interactions', 4, 'done', NOW() - INTERVAL '29 days');

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
