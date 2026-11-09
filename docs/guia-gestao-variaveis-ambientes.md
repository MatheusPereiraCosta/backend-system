# 🔐 GUIA COMPLETO: GESTÃO DE VARIÁVEIS DE AMBIENTE E SEGURANÇA (DSV, HML, PRD)

Este documento explica como funciona a separação de configurações por ambiente no Backend e no Frontend, como as variáveis são injetadas no container e as diferenças entre fixar valores no código versus usar variáveis de ambiente seguras.

---

## 1. Abordagem 1: Valores Separados por Arquivo no Próprio Projeto

Nesta abordagem, criamos arquivos dedicados para cada ambiente diretamente no código-fonte.

### A. No Backend (Spring Boot Profiles)

O Spring Boot permite criar arquivos de configuração específicos para cada ambiente na pasta `src/main/resources/`:

* `application-dsv.yml` (Desenvolvimento)
  ```yaml
  spring:
    datasource:
      url: jdbc:postgresql://servidor-dsv.meudominio.com:5432/agendapro_dsv
      username: user_dsv
      password: senha_dsv_123
    jpa:
      show-sql: true # Mostra queries no terminal para debug
  ```

* `application-hml.yml` (Homologação)
  ```yaml
  spring:
    datasource:
      url: jdbc:postgresql://servidor-hml.meudominio.com:5432/agendapro_hml
      username: user_hml
      password: senha_hml_456
    jpa:
      show-sql: false
  ```

* `application-prd.yml` (Produção)
  ```yaml
  spring:
    datasource:
      url: jdbc:postgresql://servidor-prd.meudominio.com:5432/agendapro_prd
      username: admin_prd
      password: senha_ultra_secreta_789
    jpa:
      show-sql: false
  ```

#### Como a aplicação sabe qual arquivo carregar?
Ao iniciar o container ou o `.jar`, você informa a propriedade `spring.profiles.active`:
```bash
# Para rodar em desenvolvimento:
java -jar app.jar --spring.profiles.active=dsv

# Para rodar em homologação:
java -jar app.jar --spring.profiles.active=hml

# Para rodar em produção:
java -jar app.jar --spring.profiles.active=prd
```
O Spring Boot carrega automaticamente apenas o arquivo correspondente ao profile ativo.

---

### B. No Frontend (Angular Environments)

No Angular, a separação é feita dentro de `src/environments/`:
* `src/environments/environment.development.ts`:
  ```typescript
  export const environment = {
    production: false,
    apiUrl: 'https://api-dsv.meudominio.com/api'
  };
  ```
* `src/environments/environment.homologation.ts`:
  ```typescript
  export const environment = {
    production: false,
    apiUrl: 'https://api-hml.meudominio.com/api'
  };
  ```
* `src/environments/environment.ts` (Produção):
  ```typescript
  export const environment = {
    production: true,
    apiUrl: 'https://api.meudominio.com/api'
  };
  ```

No momento do build, o Angular CLI substitui os arquivos de acordo com o parâmetro:
```bash
# Compilar para homologação:
ng build --configuration=homologation

# Compilar para produção:
ng build --configuration=production
```

---

## 2. Por que a Indústria Evita Senhas e Credenciais Direto no Git?

Deixar senhas gravadas no código funciona tecnicamente, porém traz riscos críticos:

| Risco | O que acontece na prática? |
| :--- | :--- |
| **1. Vazamento Público** | Se o repositório for público ou um colaborador tiver acesso ao código, ele terá acesso total ao banco com os dados dos clientes da barbearia. Bots vasculham o GitHub continuamente buscando senhas expostas. |
| **2. Rigidez Operacional** | Se a senha do banco precisar ser alterada por segurança ou rotação periódica, você é obrigado a: alterar o código $\rightarrow$ commitar $\rightarrow$ rodar todo o CI/CD $\rightarrow$ gerar nova imagem Docker só por causa de uma senha. |
| **3. Violação de Conformidade (LGPD)** | Em sistemas comerciais reais, auditorias de segurança reprovam imediatamente aplicações com credenciais de produção salvas em controle de versão. |

---

## 3. Abordagem 2: A Melhor Prática (O "Meio-Termo Inteligente")

A melhor prática mundial (**Twelve-Factor App - Fator III: Configurações**) combina as duas técnicas:

1. **Configurações Estruturais e Comportamento**: Ficam nos arquivos de profile (`application-dsv.yml`, `application-prd.yml`) no Git:
   * Nível de logs (`DEBUG`, `INFO`, `WARN`).
   * Ativar/desativar exibição de queries SQL (`show-sql`).
   * Configuração de pools de conexão e timeouts.
2. **Dados Sensíveis e Credenciais**: São preenchidos via **Variáveis de Ambiente** injetadas dinamicamente pela infraestrutura de nuvem:

```yaml
# application-prd.yml
spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL}
    username: ${SPRING_DATASOURCE_USERNAME}
    password: ${SPRING_DATASOURCE_PASSWORD} # Vem da nuvem de forma 100% segura!
  jpa:
    show-sql: false
```

---

## 4. Quem Injeta as Variáveis em Cada Cenário?

```
+-----------------------------------------------------------------------------------------+
| LOCAL (Seu Computador / Docker Compose)                                                 |
| - Quem injeta: O arquivo .env ou docker-compose.yml local.                              |
| - Exemplo: SPRING_DATASOURCE_PASSWORD=minha_senha_local                                 |
+-----------------------------------------------------------------------------------------+

+-----------------------------------------------------------------------------------------+
| GITHUB ACTIONS (Durante o CI/CD)                                                        |
| - Quem injeta: Os GitHub Secrets & Environments configurados no repositório.           |
| - Finalidade: Usado apenas para autenticar na nuvem e disparar o deploy.                |
+-----------------------------------------------------------------------------------------+

+-----------------------------------------------------------------------------------------+
| CLOUD / AWS (Em Produção, DSV ou HML)                                                   |
| - Quem injeta: O serviço de computação da nuvem (AWS ECS / App Runner / Kubernetes).    |
| - Onde fica guardado: AWS Secrets Manager ou AWS Systems Manager Parameter Store.       |
| - Como funciona: Ao ligar o container, a AWS busca a senha no cofre e entrega na       |
|   memória da aplicação. Nenhum arquivo no Git precisa conter a senha!                  |
+-----------------------------------------------------------------------------------------+
```

---

## 5. Resumo Prático para o Desenvolvedor

1. **Posso começar colocando os arquivos de profile separados por ambiente?**  
   👉 **Sim!** É ótimo para organizar as diferenças de cada ambiente (ex: logs detalhados em DSV vs logs enxutos em PRD).
2. **Como protejo o banco de dados de produção?**  
   👉 Mantendo a senha do banco como `${DB_PASSWORD}` para que o servidor da nuvem forneça a senha em tempo de execução.
3. **Preciso alterar código para mudar de ambiente?**  
   👉 **Não!** A mesma imagem Docker compilada roda em DSV, HML ou PRD apenas recebendo as variáveis do ambiente onde foi iniciada.
