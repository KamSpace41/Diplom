
DELETE FROM auth_tokens WHERE EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'auth_tokens');
DELETE FROM files WHERE EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'files');
DELETE FROM users WHERE EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'users');


ALTER SEQUENCE IF EXISTS users_id_seq RESTART WITH 1;


INSERT INTO users (username, password, email, full_name, created_at, is_active)
SELECT 'admin', '$2a$10$RoIT1Rpo/Z/wvXz32rvWwObamgVira3hcnH3d0cGvyIU2xSffoK7m',
       'admin@example.com', 'Administrator', CURRENT_TIMESTAMP, TRUE
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'admin');


INSERT INTO users (username, password, email, full_name, created_at, is_active)
SELECT 'user', '$2a$10$RoIT1Rpo/Z/wvXz32rvWwObamgVira3hcnH3d0cGvyIU2xSffoK7m',
       'user@example.com', 'Regular User', CURRENT_TIMESTAMP, TRUE
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'user');


INSERT INTO users (username, password, email, full_name, created_at, is_active)
SELECT 'testuser', '$2a$10$RoIT1Rpo/Z/wvXz32rvWwObamgVira3hcnH3d0cGvyIU2xSffoK7m',
       'test@example.com', 'Test User', CURRENT_TIMESTAMP, TRUE
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'testuser');