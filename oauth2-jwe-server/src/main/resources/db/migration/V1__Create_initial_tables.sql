-- Create database schema for OAuth2 JWE Server

-- Users table
CREATE TABLE usuarios (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    login VARCHAR(100) NOT NULL UNIQUE,
    senha VARCHAR(255) NOT NULL,
    ativo BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_login (login),
    INDEX idx_email (email),
    INDEX idx_ativo (ativo)
);

-- Roles table
CREATE TABLE roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_name VARCHAR(100) NOT NULL UNIQUE,
    descricao VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_role_name (role_name)
);

-- User roles relationship table
CREATE TABLE usuario_roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    UNIQUE KEY uk_usuario_role (usuario_id, role_id),
    INDEX idx_usuario_id (usuario_id),
    INDEX idx_role_id (role_id)
);

-- OAuth2 clients table
CREATE TABLE oauth2_clients (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    client_id VARCHAR(255) NOT NULL UNIQUE,
    client_secret VARCHAR(255) NOT NULL,
    client_name VARCHAR(255),
    scopes TEXT,
    grant_types TEXT,
    redirect_uris TEXT,
    ativo BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_client_id (client_id),
    INDEX idx_ativo (ativo)
);

-- Access tokens table
CREATE TABLE access_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    token_id VARCHAR(255) NOT NULL UNIQUE,
    token_value TEXT NOT NULL,
    refresh_token VARCHAR(255),
    usuario_id BIGINT NOT NULL,
    client_id VARCHAR(255) NOT NULL,
    scopes TEXT,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    revoked BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE,
    INDEX idx_token_id (token_id),
    INDEX idx_refresh_token (refresh_token),
    INDEX idx_usuario_id (usuario_id),
    INDEX idx_client_id (client_id),
    INDEX idx_expires_at (expires_at),
    INDEX idx_revoked (revoked)
);

-- Refresh tokens table
CREATE TABLE refresh_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    token_id VARCHAR(255) NOT NULL UNIQUE,
    token_value VARCHAR(255) NOT NULL UNIQUE,
    access_token_id BIGINT NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    revoked BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (access_token_id) REFERENCES access_tokens(id) ON DELETE CASCADE,
    INDEX idx_token_id (token_id),
    INDEX idx_token_value (token_value),
    INDEX idx_access_token_id (access_token_id),
    INDEX idx_expires_at (expires_at),
    INDEX idx_revoked (revoked)
);

-- Rate limiting table
CREATE TABLE rate_limit_buckets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    client_identifier VARCHAR(255) NOT NULL,
    bucket_key VARCHAR(255) NOT NULL,
    tokens_remaining INT NOT NULL,
    last_refill TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_client_bucket (client_identifier, bucket_key),
    INDEX idx_client_identifier (client_identifier),
    INDEX idx_last_refill (last_refill)
);