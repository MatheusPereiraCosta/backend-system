# ETAPA 5 — DOCKER E AMBIENTE LOCAL

Este documento detalha o funcionamento, arquitetura, boas práticas e conceitos aplicados na conteinerização da plataforma **AgendaPro** usando Docker, Dockerfiles Multi-stage e Docker Compose.

---

## 1. Visão Geral da Arquitetura de Containers

A arquitetura conteinerizada isola o banco de dados, o backend e o frontend em containers independentes e interconectados por uma rede interna segura do tipo `bridge`.

```
                    USUÁRIO / BROWSER
                           |
                           | Porta 80 (HTTP)
                           v
          +----------------------------------+
          |   Container: agendapro-frontend  |
          |   (Nginx 1.27 + Angular 19 SPA)  |
          +----------------------------------+
             |                           |
             | / (Rotas SPA)             | /api/ & /actuator/
             v                           v  (Proxy Reverso Nginx)
      [index.html & JS]                  |
                                         v
                         +---------------------------------+
                         |   Container: agendapro-backend  |
                         |   (Spring Boot 3 + Java 21 JRE) |
                         +---------------------------------+
                                         |
                                         | Porta 5432 (JDBC)
                                         v
                         +---------------------------------+
                         |   Container: agendapro-postgres |
                         |   (PostgreSQL 16 Alpine)        |
                         +---------------------------------+
                                         |
                                         v
                             [Volume: postgres_data]
```

---

## 2. Explicação dos Componentes e Conceitos

### 2.1 Multi-Stage Builds (Compilação em Múltiplos Estágios)

#### **Frontend (`frontend/Dockerfile`)**:
* **Estágio 1 (`build`)**: Utiliza `node:20-alpine` apenas para baixar as dependências (`npm ci`) e compilar a aplicação Angular em modo de produção (`npm run build`).
* **Estágio 2 (`runtime`)**: Utiliza `nginx:1.27-alpine` (~23 MB). Copia apenas os arquivos estáticos gerados em `/dist/frontend/browser` para `/usr/share/nginx/html`.
* **Benefício**: A imagem final não contém Node.js, npm ou código fonte desnecessário, tornando-a **ultraleve, rápida para download e muito mais segura**.

#### **Backend (`backend/Dockerfile`)**:
* **Estágio 1 (`build`)**: Utiliza `maven:3.9.9-eclipse-temurin-21-alpine` para compilar o código Java e empacotar o executável `app.jar`.
* **Estágio 2 (`runtime`)**: Utiliza `eclipse-temurin:21-jre-alpine` (apenas o JRE, sem JDK/Maven).
* **Segurança com Usuário Não-Root**: O container cria um usuário de sistema `agendapro` e roda a aplicação sem privilégios de `root`.
* **Flags de JVM Container-Aware**: `-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0` instrui o garbage collector e a JVM a respeitarem os limites de memória da cgroup do Docker.

---

### 2.2 Redes, Portas e Proxy Reverso

| Container | Imagem | Porta Exposta (Host) | Porta Interna (Container) | Finalidade |
| :--- | :--- | :--- | :--- | :--- |
| `agendapro-postgres` | `postgres:16-alpine` | `5432` | `5432` | Banco de dados relacional |
| `agendapro-backend` | `agendapro-backend:latest` | `8080` | `8080` | API REST Spring Boot |
| `agendapro-frontend` | `agendapro-frontend:latest` | `80` | `80` | Servidor Web Nginx e SPA |

#### **Eliminação do problema de CORS via Nginx**:
O Nginx atua como **Proxy Reverso**. Quando o navegador faz uma requisição para `/api/...`, o próprio Nginx encaminha internamente para `http://backend:8080/api/`. Dessa forma, o navegador enxerga tanto a interface quanto a API sob a mesma origem (`http://localhost`), eliminando completamente conflitos de CORS e necessidade de expor o backend publicamente na nuvem!

---

### 2.3 Volumes Persistentes

* **Volume `postgres_data`**: Mapeado para `/var/lib/postgresql/data`. Garante que dados de clientes, serviços e agendamentos sobrevivam mesmo se os containers forem destruídos (`docker compose down`) ou atualizados.

---

### 2.4 Healthchecks e Ordem de Inicialização (`depends_on`)

O Docker Compose tradicional iniciava containers em paralelo sem saber se o serviço interno estava pronto para aceitar conexões. O `docker-compose.yml` da AgendaPro utiliza **Healthchecks reais**:

1. **PostgreSQL**: Executa `pg_isready` a cada 10s.
2. **Backend**: Só inicia quando `postgres` estiver com status `service_healthy`. Ao subir, o Spring Boot roda as migrations Flyway e disponibiliza o `/actuator/health`.
3. **Frontend**: Só inicia quando `backend` estiver `service_healthy`.

---

## 3. Guia de Execução

### 3.1 Subir todo o ambiente com 1 comando
```bash
docker compose up --build
```
*Ou em segundo plano (modo detached):*
```bash
docker compose up -d --build
```

### 3.2 Verificar o status e integridade dos containers
```bash
docker compose ps
```

*Saída esperada:*
```text
NAME                 IMAGE                       COMMAND                  SERVICE    STATUS                    PORTS
agendapro-postgres   postgres:16-alpine          "docker-entrypoint.s…"   postgres   Up (healthy)              0.0.0.0:5432->5432/tcp
agendapro-backend    agendapro-backend:latest    "sh -c 'java $JAVA_O…"   backend    Up (healthy)              0.0.0.0:8080->8080/tcp
agendapro-frontend   agendapro-frontend:latest   "/docker-entrypoint.…"   frontend   Up (healthy)              0.0.0.0:80->80/tcp
```

### 3.3 Acessar as aplicações
* **Aplicação Web (AgendaPro)**: `http://localhost`
* **API REST Backend**: `http://localhost:8080/api/establishments/11111111-1111-1111-1111-111111111111/dashboard`
* **Healthcheck do Backend**: `http://localhost:8080/actuator/health`

### 3.4 Parar os containers mantendo os dados
```bash
docker compose down
```

### 3.5 Parar e limpar volumes (reset completo)
```bash
docker compose down -v
```
