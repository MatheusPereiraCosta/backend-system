# Documento de Arquitetura e Decisões — ETAPA 2: Backend & Banco de Dados

## 1. Visão Geral da Stack do Backend

* **Linguagem & Framework**: Java 21 LTS + Spring Boot 3.4.3
* **Banco de Dados Principal**: PostgreSQL (com suporte a UUIDs nativos, índices compostos e constraints relacionais)
* **Gerenciador de Migrations**: Flyway (garantindo que qualquer alteração de schema seja idempotente e versionada)
* **Camada ORM / Persistência**: Spring Data JPA + Hibernate 6 com validação estrita de schema (`ddl-auto: validate`) e `open-in-view: false` para performance
* **Observabilidade**: Spring Boot Actuator (`/api/actuator/health`, `/metrics`)

---

## 2. Estrutura do Modelo Relacional e Decisões de Modelagem

### 2.1. Isolamento Multi-tenant por Estabelecimento (`establishments`)
Todas as tabelas (`customers`, `professionals`, `services`, `appointments`) possuem chave estrangeira obrigatória referenciando `establishment_id`. Isso garante isolamento estrito de dados entre diferentes estabelecimentos.

### 2.2. Identificação de Clientes por Telefone
A tabela `customers` possui uma constraint única composta:
```sql
CONSTRAINT uk_customer_phone_establishment UNIQUE (establishment_id, phone)
```
Isso permite que um cliente agendando pela página pública seja identificado automaticamente pelo número do WhatsApp sem necessidade de login com senha.

### 2.3. Prevenção de Conflitos e Índices de Performance
Para que a agenda diária e o cálculo de disponibilidade de horários ocorram com latência mínima, foi criado o índice composto:
```sql
CREATE INDEX idx_appointments_schedule ON appointments(establishment_id, professional_id, appointment_date, status);
```

---

## 3. Histórico de Migrations Flyway

| Versão | Arquivo | Descrição |
|---|---|---|
| **V1** | `V1__create_initial_schema.sql` | Criação de todas as tabelas, foreign keys, constraints e índices de alta performance. |
| **V2** | `V2__seed_development_data.sql` | Carga inicial com dados realistas da *Barbearia & Studio Dom Pedro*, 3 profissionais, catálogo de serviços e agendamentos. |
