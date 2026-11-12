# AgendaPro — Backend

API REST do AgendaPro, plataforma de gestão e agendamento para negócios locais (barbearias, salões, clínicas).

## Stack

- Java 21 + Spring Boot 3
- PostgreSQL + Flyway (migrations)
- Spring Security + JWT
- Maven

## Pré-requisitos

- Java 21
- Maven 3.9+
- PostgreSQL rodando (ou Docker)

## Configuração

Variáveis de ambiente:

| Variável | Descrição | Padrão |
|---|---|---|
| `JWT_SECRET` | Chave que assina os tokens (mín. 32 caracteres). **Obrigatória** em produção. | — |
| `DB_HOST` | Host do banco | `localhost` |
| `DB_PORT` | Porta do banco | `5432` |
| `DB_NAME` | Nome do banco | `agendapro` |
| `DB_USER` / `DB_PASSWORD` | Credenciais do banco | veja `application.yml` |
| `SPRING_PROFILES_ACTIVE` | `prod` (banco vazio) ou `dev` (com dados de exemplo) | — |

Gere um segredo JWT próprio com:

```bash
openssl rand -base64 48
```

> Nunca versione segredos reais. Use um arquivo `.env` local (ignorado pelo git).

## Executando

```bash
# desenvolvimento (schema + dados de exemplo)
SPRING_PROFILES_ACTIVE=dev mvn spring-boot:run

# testes
mvn test

# build do JAR
mvn clean package
```

A API sobe em `http://localhost:8080/api`.

## Endpoints principais

| Método | Rota | Descrição |
|---|---|---|
| POST | `/api/auth/register` | Cadastro de usuário |
| POST | `/api/auth/login` | Login (retorna token JWT) |
| GET | `/api/auth/me` | Usuário autenticado |

Os demais recursos (estabelecimento, profissionais, serviços, clientes, agendamentos, disponibilidade e dashboard) exigem o header `Authorization: Bearer <token>`.

## Docker

```bash
docker build -t agendapro-backend .
```

Para subir a stack completa (banco + backend + frontend), use o `docker-compose.yml` da raiz do projeto.

## Documentação

Detalhes de arquitetura, APIs e CI/CD estão na pasta [`docs/`](docs/).
