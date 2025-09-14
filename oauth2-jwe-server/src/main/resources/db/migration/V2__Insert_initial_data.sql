-- Insert initial data for OAuth2 JWE Server

-- Insert default roles
INSERT INTO roles (role_name, descricao) VALUES
('ADMIN', 'Administrator role with full access'),
('USER', 'Standard user role'),
('API_READ', 'API read access role'),
('API_WRITE', 'API write access role');

-- Insert default admin user (password: admin123 - bcrypt encoded)
INSERT INTO usuarios (nome, email, login, senha, ativo) VALUES
('Administrator', 'admin@example.com', 'admin', '$2a$10$MIkW8zqHnVqjcs/JNmYqNOWHS6B7AqjCs4zF4JvLqKqJvL8rJ8Kqm', TRUE),
('Test User', 'user@example.com', 'user', '$2a$10$MIkW8zqHnVqjcs/JNmYqNOWHS6B7AqjCs4zF4JvLqKqJvL8rJ8Kqm', TRUE);

-- Assign roles to users
INSERT INTO usuario_roles (usuario_id, role_id) VALUES
(1, 1), -- admin user gets ADMIN role
(1, 3), -- admin user gets API_READ role
(1, 4), -- admin user gets API_WRITE role
(2, 2), -- test user gets USER role
(2, 3); -- test user gets API_READ role

-- Insert default OAuth2 client
INSERT INTO oauth2_clients (client_id, client_secret, client_name, scopes, grant_types, redirect_uris, ativo) VALUES
('oauth2-client', '$2a$10$MIkW8zqHnVqjcs/JNmYqNOWHS6B7AqjCs4zF4JvLqKqJvL8rJ8Kqm', 'Default OAuth2 Client', 'read,write,admin', 'password,refresh_token,client_credentials', 'http://localhost:8080/callback', TRUE),
('api-client', '$2a$10$MIkW8zqHnVqjcs/JNmYqNOWHS6B7AqjCs4zF4JvLqKqJvL8rJ8Kqm', 'API Client', 'read,write', 'client_credentials', '', TRUE);