CREATE DATABASE oauth2_db;
CREATE USER 'oauth2_user'@'localhost' IDENTIFIED BY 'oauth2_pass';
GRANT ALL PRIVILEGES ON oauth2_db.* TO 'oauth2_user'@'localhost';