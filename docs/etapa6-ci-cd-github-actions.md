# ETAPA 6 — CI/CD COM GITHUB ACTIONS

Este documento detalha o design, arquitetura, segurança e fluxo de execução das esteiras de Integração Contínua (CI) e Entrega Contínua (CD) criadas para a plataforma **AgendaPro** utilizando **GitHub Actions** e o **GitHub Container Registry (GHCR)**.

---

## 1. Visão Geral da Pipeline

A esteira de CI/CD garante que cada alteração de código enviada via `push` ou `pull request` seja automaticamente testada, validada e transformada em uma imagem Docker pronta para produção.

```
       [ Git Push / Pull Request ]
                   |
     +-------------+-------------+
     |                           |
     v                           v
+-----------------------+  +------------------------+
|   Job: Frontend CI    |  |    Job: Backend CI     |
|  - Node 20 Setup      |  |  - Java 21 Corretto    |
|  - npm ci (Cached)    |  |  - mvn verify (Tests)  |
|  - ng build (Prod)    |  |  - Flyway + JPA Tests  |
|  - Upload Artifact    |  |  - Upload JAR/Reports  |
+-----------+-----------+  +-----------+------------+
            |                          |
            | (Se branch == 'main')    | (Se branch == 'main')
            v                          v
+-----------------------+  +------------------------+
| Job: Frontend Docker  |  |  Job: Backend Docker   |
|  - Docker Buildx      |  |  - Docker Buildx       |
|  - Login GHCR         |  |  - Login GHCR          |
|  - Multi-stage Build  |  |  - Multi-stage Build   |
|  - Cache GHA Layer    |  |  - Cache GHA Layer     |
|  - Push Tag: latest   |  |  - Push Tag: latest    |
|  - Push Tag: sha-XXXX |  |  - Push Tag: sha-XXXX  |
+-----------------------+  +------------------------+
            |                          |
            +------------+-------------+
                         |
                         v
        [ GitHub Container Registry (ghcr.io) ]
         - ghcr.io/<usuario>/agendapro-frontend
         - ghcr.io/<usuario>/agendapro-backend
```

---

## 2. Explicação Técnica dos Conceitos

### 2.1 Triggers (Gatilhos de Execução)
* `push: branches: [ main, develop ]`: Executa a pipeline completa toda vez que um commit é integrado na branch principal ou de desenvolvimento.
* `pull_request: branches: [ main, develop ]`: Executa apenas os testes e builds de validação, bloqueando o merge caso haja testes falhando ou erros de compilação.
* `workflow_dispatch`: Permite disparar a pipeline manualmente pela interface web do GitHub.

### 2.2 Estratégias de Cache (Velocidade e Economia)
1. **Cache de Dependências Node.js / NPM**:
   `actions/setup-node@v4` com `cache: 'npm'` armazena em cache o diretório `~/.npm`, reduzindo o tempo de `npm ci` de minutos para segundos.
2. **Cache de Dependências Maven / Java**:
   `actions/setup-java@v4` com `cache: 'maven'` mantém em cache o repositório `~/.m2/repository`, evitando baixar os pacotes do Spring Boot em cada execução.
3. **Cache de Camadas Docker (GHA)**:
   Utilização do `cache-from: type=gha` e `cache-to: type=gha,mode=max` através do Buildx. Camadas inalteradas do `Dockerfile` (como instalação de pacotes Alpine e dependências) são reutilizadas instantaneamente.

### 2.3 Gestão de Segredos e Permissões (`GITHUB_TOKEN`)
Não é necessário criar ou expor credenciais manuais no GitHub Secrets para o registry. Utilizamos o token seguro e rotativo nativo do GitHub:
* `permissions: contents: read` (para ler o repositório).
* `permissions: packages: write` (para publicar as imagens no GitHub Container Registry).
* Autenticação via `docker/login-action@v3` usando `${{ secrets.GITHUB_TOKEN }}`.

### 2.4 Tagging Semântico e Rastreabilidade
Cada imagem gerada recebe múltiplas tags:
* `latest`: Aponta sempre para a versão mais recente da branch `main`.
* `sha-<commit_curto>` (ex: `sha-793f723`): Tag imutável que permite saber exatamente qual commit do Git gerou aquele container em produção e possibilita rollbacks instantâneos.

---

## 3. Arquivos de Workflow Criados

| Arquivo | Repositório | Responsabilidade |
| :--- | :--- | :--- |
| `frontend/.github/workflows/frontend-ci-cd.yml` | `frontend-system` | Pipeline do Angular: `npm ci`, `ng build`, `Docker buildx`, push no GHCR |
| `backend/.github/workflows/backend-ci-cd.yml` | `backend-system` | Pipeline do Spring Boot: `mvn verify`, surefire reports, `Docker buildx`, push no GHCR |
| `.github/workflows/main-ci-cd.yml` | Raiz / Monorepo | Validação de integridade global de frontend e backend |

---

## 4. Como Visualizar as Imagens Publicadas

Após o push na branch `main`:
1. Acesse o seu perfil no GitHub: `https://github.com/MatheusPereiraCosta?tab=packages`
2. Você verá os pacotes:
   * `agendapro-frontend`
   * `agendapro-backend`
3. Para baixar e rodar a imagem gerada pelo GitHub Actions em qualquer servidor:
   ```bash
   docker pull ghcr.io/matheuspereiracosta/agendapro-frontend:latest
   docker pull ghcr.io/matheuspereiracosta/agendapro-backend:latest
   ```
