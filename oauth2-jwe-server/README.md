# OAuth2 JWE Authentication Server

Um servidor de autenticação OAuth2 completo implementado em Java 21 com Spring Boot 3.5.5, utilizando JWE (JSON Web Encryption) para tokens seguros.

## 🚀 Características

- **OAuth2 completo** com suporte a múltiplos grant types
- **JWE (JSON Web Encryption)** para tokens seguros
- **Rate limiting** configurável (5 req/s por padrão)
- **Logging completo** de requests/responses
- **MySQL** com HikariCP para performance
- **Flyway** para migrações de banco
- **BCrypt** para hash de senhas
- **Monitoramento** com Spring Actuator

## 📋 Pré-requisitos

- Java 21
- Maven 3.8+
- MySQL 8.0+

## 🛠️ Configuração

### 1. Banco de Dados

```sql
CREATE DATABASE oauth2_db;
CREATE USER 'oauth2_user'@'localhost' IDENTIFIED BY 'oauth2_pass';
GRANT ALL PRIVILEGES ON oauth2_db.* TO 'oauth2_user'@'localhost';
FLUSH PRIVILEGES;
```

### 2. Configuração da Aplicação
   Edite application.yml conforme necessário:
```
spring:
datasource:
url: jdbc:mysql://localhost:3306/oauth2_db
username: oauth2_user
password: oauth2_pass
```

### 3. Executar a Aplicação

```
mvn clean install
mvn spring-boot:run
```
A aplicação estará disponível em http://localhost:8080

# 🔐 Endpoints da API

## Autenticação

#### Gerar Token (Password Grant) (http://localhost:8080/auth/oauth/v2/token-jew)

```
POST /auth/oauth/v2/token-jew
Content-Type: application/json

{
"grant_type": "password",
"username": "admin",
"password": "admin123",
"client_id": "oauth2-client",
"client_secret": "secret123",
"scope": "read write"
}
```
#### Refresh Token (Refresh) (http://localhost:8080/auth/oauth/v2/token-jew)
```
POST /auth/oauth/v2/token-jew
Content-Type: application/json

{
"grant_type": "refresh_token",
"refresh_token": "your-refresh-token",
"client_id": "oauth2-client",
"client_secret": "secret123"
}
```

#### Client Credentials (Client Credentials) (http://localhost:8080/auth/oauth/v2/token-jew)

```
POST /auth/oauth/v2/token-jew
Content-Type: application/json

{
"grant_type": "client_credentials",
"client_id": "api-client",
"client_secret": "secret123",
"scope": "read write"
}
```

#### Validar Token
```
POST /auth/oauth/v2/validate?token=your-jwe-token
```
#### Revogar Token
```
POST /auth/oauth/v2/revoke?token_id=token-id
```
#### Logout
```
POST /auth/oauth/v2/logout?user_id=1
```

## APIs Protegidas
### Perfil do Usuário
```
GET /api/v1/protected/profile
Authorization: Bearer your-jwe-token
```
### Dados Protegidos
```
GET /api/v1/protected/data
Authorization: Bearer your-jwe-token
```
### Criar Dados (Requer scope 'write')
```
POST /api/v1/protected/data
Authorization: Bearer your-jwe-token
Content-Type: application/json

{
"name": "Test Data",
"value": "Some value"
}
```
## Admin - Listar Usuários (Requer role 'ADMIN')
```
GET /api/v1/admin/users
Authorization: Bearer your-jwe-token
```
# Endpoints Públicos

### Health Check
```
GET /api/v1/public/health
```
### Actuator Health
```
GET /actuator/health
```
# 👥 Usuários Padrão
```
Login	Senha	        Roles
admin	admin123	ADMIN, API_READ, API_WRITE
user	admin123	USER, API_READ
```
# 🔧 Configurações

Rate Limiting
yaml
Copy
```
rate-limit:
    auth:
        requests-per-second: 5
        burst-capacity: 10
```

JWE Keys
yaml
Copy
```
jwe:
    encryption:
        key: "mySecretEncryptionKey123456789012" # 32 chars
    signing:
        key: "mySecretSigningKey1234567890123456" # 32 chars
```
# 📊 Monitoramento
## Health: http://localhost:8080/actuator/health
## Metrics: http://localhost:9001/actuator/metrics
## Info: http://localhost:9001/actuator/info

# 📝 Logs
## Aplicação: oauth2-server.log
## Requests: oauth2-requests.log

# 🔒 Segurança
### Tokens JWE com criptografia AES-256-GCM
### Rate limiting por cliente/IP
### Senhas com BCrypt
### Logs sanitizados (sem dados sensíveis)
### Validação de entrada com Bean Validation

## 🚀 Deploy
### Docker (Opcional)
### dockerfile
```
FROM openjdk:21-jdk-slim
COPY target/oauth2-jwe-server-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080 9001
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

### Variáveis de Ambiente

```
export DB_USERNAME=oauth2_user
export DB_PASSWORD=oauth2_pass
export JWE_ENCRYPTION_KEY=your-32-char-encryption-key
export JWE_SIGNING_KEY=your-32-char-signing-key
```

# 🧪 Testes

# Executar testes
```
mvn test
```

# Executar com coverage
```
mvn test jacoco:report
```

# 📚 Documentação Adicional
### Spring Security OAuth2
### Nimbus JOSE + JWT
### Flyway Documentation
### HikariCP
### 🤝 Contribuição

### 📄 Licença
Este projeto está sob a licença MIT. 

## Exemplos
### Gerar Token
```
curl -X POST http://localhost:8080/auth/oauth/v2/token-jew \
-H "Content-Type: application/x-www-form-urlencoded" \
-d "grant_type=password&username=admin&password=admin123&client_id=oauth2-client&client_secret=secret&scope=read,write&ttl=3600"
```
### Usar API:
```
curl -X GET http://localhost:8080/v1/printer/status \
-H "Authorization: Bearer {access_token}"
```
