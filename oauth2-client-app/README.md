# OAuth2 Client Application

Aplicação cliente que demonstra como usar tokens JWE do servidor OAuth2 para autenticação.

## 🚀 Como Executar

1. **Certifique-se que o servidor OAuth2 está rodando na porta 8080**

2. **Execute a aplicação cliente:**
```
mvn clean install
mvn spring-boot:run
```
A aplicação estará disponível em http://localhost:8081

# 📋 Endpoints Disponíveis
## Endpoint Principal
```
GET http://localhost:8081/api/hello
```
Resposta esperada:
```

{
"message": "autenticado",
"status": 200,
"payload": "hello"
}
```

# Outros Endpoints
## Perfil do Usuário
```
GET http://localhost:8081/api/profile
```
# Dados Protegidos
```
GET http://localhost:8081/api/data
```
# Criar Dados

```
POST http://localhost:8081/api/create-data
Content-Type: application/json

{
"name": "Test Data",
"value": "Some value"
}
```
# Informações do Token

```
GET http://localhost:8081/api/token-info
```
# Health Check

```
GET http://localhost:8081/api/health
```

# 🔧 Configuração
## A aplicação está configurada para:

## Porta: 8081
## Servidor OAuth2: http://localhost:8080
## Cliente: oauth2-client
## Usuário: admin
## Senha: admin123
## Scopes: read,write

# 🔄 Fluxo de Autenticação
## A aplicação solicita um token ao servidor OAuth2 usando credenciais
## O token JWE é armazenado em memória
## Todas as requisições para APIs protegidas usam este token
## O token é renovado automaticamente quando expira

# 📊 Monitoramento
## Health: http://localhost:8081/api/health
## Actuator: http://localhost:9002/actuator/health

# 🧪 Testando

# Teste o endpoint principal
```
curl http://localhost:8081/api/hello
```
# Teste com informações do token
```
curl http://localhost:8081/api/token-info
```
# Teste o health check
```
curl http://localhost:8081/api/health
```
## 🚀 Como Usar

# 1. **Primeiro, inicie o servidor OAuth2 na porta 8080**
# 2. **Em seguida, inicie esta aplicação cliente:**

```bash
cd oauth2-client-app
mvn clean install
mvn spring-boot:run
```
Teste o endpoint principal:
bash
Copy
```
curl http://localhost:8081/api/hello
```
Resposta esperada:
```
{
  "message": "autenticado",
  "status": 200,
  "payload": "hello"
}
```
# A aplicação cliente automaticamente:

### Solicita um token OAuth2 do servidor
### Valida o token
### Usa o token para fazer requisições autenticadas
### Renova o token quando necessário

# 🚀 Como Executar os Testes

# Executar todos os testes:

```
mvn clean test
```
# Executar apenas testes unitários:

```
mvn clean test -Dtest="*Test"
```
# Executar apenas testes de integração:

```
mvn clean test -Dtest="*IntegrationTest"
```
# Executar com relatório de cobertura:

```
mvn clean test jacoco:report
```
# Executar testes específicos:

### Apenas testes do serviço
```
mvn clean test -Dtest="OAuth2ClientServiceTest"
```
### Apenas testes do controlador
```
mvn clean test -Dtest="ClientControllerTest"
```

###  Apenas testes de integração
```
mvn clean test -Dtest="OAuth2ClientIntegrationTest"
``` 
# 📊 Relatórios de Cobertura
## Após executar mvn clean test jacoco:report, o relatório de cobertura estará disponível em:
### target/site/jacoco/index.html

# 🧪 Resumo dos Testes
##  Testes Unitários: 25+ testes cobrindo serviços, controladores e configurações
## Testes de Integração: Testes end-to-end com WireMock simulando o servidor OAuth2
## Cobertura: Testes cobrem cenários de sucesso, falha e casos extremos
## Mocking: Uso extensivo de Mockito para isolar componentes
## Assertions: AssertJ para assertions mais legíveis
