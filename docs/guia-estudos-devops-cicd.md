# 📘 GUIA COMPLETO: ARQUITETURA DEVOPS, CONTAINERS, CI/CD E GESTÃO DE AMBIENTES

Este documento serve como material de consulta e estudo permanente sobre todo o fluxo construído no projeto **AgendaPro**.

---

## 1. O Ciclo de Vida: Do Código ao Deploy

```
+-----------------------------------------------------------------------------------+
| 1. SEU COMPUTADOR (Localhost)                                                     |
|    - Desenvolvimento com Angular 19 e Spring Boot 3                               |
|    - Testes rápidos locais (npm start, mvn test, docker compose up)               |
|    - git commit & git push                                                        |
+----------------------------------------+------------------------------------------+
                                         | (Webhook git push)
                                         v
+-----------------------------------------------------------------------------------+
| 2. GITHUB ACTIONS (Runners na Nuvem do GitHub)                                   |
|    - Máquina virtual Linux Ubuntu limpa provisionada pelo GitHub                  |
|                                                                                   |
|    [FASE 1: QUALIDADE E COMPILAÇÃO (Em Qualquer Branch)]                         |
|    1. Setup do Java 21 / Node 20 com Cache inteligente                            |
|    2. Execução dos TESTES AUTOMATIZADOS (mvn verify / ng test)                    |
|       * Se 1 teste falhar -> A pipeline quebra imediatamente (Fail-Fast)          |
|    3. Compilação dos binários (.jar no Backend e /dist no Frontend)               |
|                                                                                   |
|    [FASE 2: CONSTRUÇÃO DA IMAGEM DOCKER (Apenas na Branch 'main')]                |
|    4. O Dockerfile entra em ação (Multi-Stage Build)                              |
|    5. A aplicação compilada é empacotada em uma Imagem Docker enxuta e segura     |
|    6. docker push envia a imagem para o armazém na nuvem do GitHub               |
+----------------------------------------+------------------------------------------+
                                         | (Armazenamento de Imagens)
                                         v
+-----------------------------------------------------------------------------------+
| 3. GITHUB CONTAINER REGISTRY (ghcr.io)                                            |
|    - É o "depósito / cofre privado" onde as imagens Docker ficam guardadas        |
|    - Endereço da imagem: ghcr.io/<usuario>/agendapro-backend:latest               |
|    - Versões imutáveis por commit: ghcr.io/<usuario>/agendapro-backend:sha-181ee36|
+----------------------------------------+------------------------------------------+
                                         | (Processo de Deploy)
                                         v
+-----------------------------------------------------------------------------------+
| 4. SERVIDORES / NUVEM DE EXECUÇÃO (Onde os Containers Realmente Rodam)            |
|                                                                                   |
|    - AMBIENTE DSV (Desenvolvimento):                                              |
|      * Deploy 100% AUTOMÁTICO assim que o código entra na 'main'                  |
|      * O servidor de DSV baixa a imagem mais recente e sobe os novos containers   |
|                                                                                   |
|    - AMBIENTE HML (Homologação):                                                  |
|      * Pipeline PAUSA e solicita APROVAÇÃO MANUAL do Tech Lead no GitHub          |
|      * Após aprovação, faz o deploy para validação de negócio                     |
|                                                                                   |
|    - AMBIENTE PRD (Produção):                                                     |
|      * Pipeline PAUSA e solicita APROVAÇÃO MANUAL                                 |
|      * Após aprovação, faz o deploy Zero-Downtime para os clientes finais         |
+-----------------------------------------------------------------------------------+
```

---

## 2. Para que serve o Dockerfile e o Multi-Stage Build?

O **Dockerfile** é o arquivo que ensina o Docker a empacotar o sistema operacional mínimo + a sua aplicação.

Utilizamos **Multi-Stage Build** (múltiplos estágios), que é a melhor prática mundial de engenharia:
1. **Estágio 1 (Build - Pesado)**: Contém o compilador (JDK 21 ou Node.js completo) e todas as ferramentas de desenvolvimento necessárias para gerar o `.jar` ou o bundle do Angular.
2. **Estágio 2 (Runtime - Leve e Seguro)**: Descarta todo o compilador, código-fonte e histórico de build. Copia apenas o resultado final (`.jar` ou HTML/JS estático) para uma imagem mínima (JRE Alpine ou Nginx Alpine).
3. **Resultado**: Imagens minúsculas (~25MB no frontend e ~150MB no backend), com tempo de inicialização ultrarrápido e superfície de ataque de segurança reduzida a quase zero.

---

## 3. Como o Frontend e o Backend sabem as URLs de cada Ambiente SEM mexer no Git?

Essa é uma das regras mais fundamentais de arquitetura de software (**The Twelve-Factor App - Fator III: Configurações**):  
👉 **"O mesmo código compilado e a mesma imagem Docker devem rodar em Desenvolvimento, Homologação e Produção sem precisar alterar uma única linha de código ou recompilar."**

Como resolvemos isso em cada lado:

### A. No Backend (Spring Boot): Variáveis de Ambiente e Profiles

No código do backend, nunca fixamos senhas ou URLs de banco. O arquivo [`application.yml`](backend/src/main/resources/application.yml) lê as variáveis injetadas pelo ambiente onde o container está rodando:

```yaml
spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/agendapro_db}
    username: ${SPRING_DATASOURCE_USERNAME:agendapro_user}
    password: ${SPRING_DATASOURCE_PASSWORD:agendapro_secret_password}
```

* **No seu computador local**: O Spring usa os valores default (`localhost`).
* **No Docker Compose local**: O Compose injeta `SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/agendapro_db`.
* **No servidor de DSV na AWS**: A AWS injeta a URL do banco de desenvolvimento: `jdbc:postgresql://rds-dsv.aws.com:5432/db_dsv`.
* **No servidor de PRD na AWS**: A AWS injeta a URL do banco de produção seguro: `jdbc:postgresql://rds-prd.aws.com:5432/db_prd`.

> **Você não precisa dar commit nem gerar uma nova imagem Docker para mudar de ambiente!** Apenas as variáveis de ambiente mudam no servidor de destino.

---

### B. No Frontend (Angular): Proxy Reverso Nginx e URL Relativa (`/api`)

Tradicionalmente, desenvolvedores iniciantes tentavam recompilar o Angular para cada ambiente (`http://api-dsv.com`, `http://api-prd.com`). Isso é uma má prática porque exige gerar imagens Docker diferentes para cada ambiente.

**Como resolvemos profissionalmente:**
1. No Angular, todas as chamadas HTTP usam a URL relativa `/api` (configurado em [`environment.ts`](frontend/src/environments/environment.ts)):
   ```typescript
   export const environment = {
     production: true,
     apiUrl: '/api' // URL relativa!
   };
   ```
2. Quando o usuário clica no site, o navegador chama `https://meudominio.com.br/api/appointments`.
3. O container do **Nginx** intercepta o `/api/` e faz o **Proxy Reverso** para o backend:
   ```nginx
   location /api/ {
       proxy_pass http://backend:8080/api/;
   }
   ```
4. **Vantagens dessa arquitetura:**
   * A mesma imagem Docker do frontend roda em qualquer domínio ou ambiente sem alteração.
   * **Zero problemas de CORS**: Para o navegador, o frontend e a API estão no mesmíssimo endereço e porta.
   * O backend fica escondido na rede privada, sem exposição desnecessária na internet pública.

---

## 4. Em que passo estamos no Roadmap Geral?

| Etapa | Descrição | Status |
| :---: | :--- | :---: |
| **Etapa 1** | Protótipo Visual Completo (Angular 19, PrimeNG, Tailwind, ApexCharts) | ✅ **Concluída** |
| **Etapa 2** | Modelagem do Banco de Dados Relacional e Migrations Flyway | ✅ **Concluída** |
| **Etapa 3** | Backend Spring Boot 3, APIs REST e Regras de Concorrência de Horários | ✅ **Concluída** |
| **Etapa 4** | Integração Frontend + Backend com Fallback Híbrido | ✅ **Concluída** |
| **Etapa 5** | Docker, Dockerfiles Multi-stage, Proxy Nginx e Docker Compose | ✅ **Concluída** |
| **Etapa 6** | CI/CD com GitHub Actions, Registry GHCR e Aprovação de Ambientes (DSV/HML/PRD) | ✅ **Concluída** |
| **Etapa 7** | **Deploy na AWS (Cloud Computing, RDS, ECR, S3, ECS/App Runner)** | ⏳ **PRÓXIMA ETAPA** |
| **Etapa 8** | Kubernetes Local (Kind) e Orquestração de Clusters | 📋 Pendente |
| **Etapa 9** | Terraform (Infraestrutura como Código - IaC) | 📋 Pendente |
| **Etapa 10**| Observabilidade e Monitoramento (Prometheus, Grafana, Actuator) | 📋 Pendente |
