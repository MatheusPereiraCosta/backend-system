# ETAPA 4 — INTEGRAÇÃO FRONTEND E BACKEND

Este documento descreve a arquitetura, padrões e fluxos de integração implementados para conectar o frontend Angular 19 com o backend Spring Boot 3 da plataforma **AgendaPro**.

---

## 1. Visão Geral da Arquitetura de Integração

A integração foi desenvolvida seguindo o padrão **Híbrido Resiliente (API-First com Fallback Transparente)**. Isso garante que a aplicação:
1. Conecte-se e execute todas as operações CRUD e regras de negócio diretamente no backend Spring Boot e banco de dados PostgreSQL quando o servidor estiver rodando.
2. Mantenha operabilidade plena e demonstátivel mesmo se o backend estiver desligado, recorrendo suavemente aos signals em memória com dados mockados.

```
+-------------------------------------------------------------+
|                      Angular 19 (Frontend)                  |
|  - Components & Pages (Dashboard, Schedule, Booking, etc.)  |
|  - Signals Reativos & RxJS Observables                      |
+------------------------------+------------------------------+
                               | (HTTP REST / JSON)
                               v
+-------------------------------------------------------------+
|                 Spring Boot 3.4.3 (Backend API)             |
|  - Controllers REST (/api/establishments/{id}/...)         |
|  - GlobalExceptionHandler (RFC 7807 ProblemDetail)         |
|  - Service Layer (Regras de Conflito de Horário & KPIs)    |
|  - Repositories (Spring Data JPA)                           |
+------------------------------+------------------------------+
                               | (JDBC HikariCP)
                               v
+-------------------------------------------------------------+
|                   PostgreSQL 16 (Database)                 |
|  - Flyway Migrations (Schema V1 & Seed V2)                  |
+-------------------------------------------------------------+
```

---

## 2. Configuração de Ambientes e HTTP Client

### `src/environments/environment.ts`
```typescript
export const environment = {
  production: true,
  apiUrl: '/api',
  defaultEstablishmentId: '11111111-1111-1111-1111-111111111111'
};
```

### `src/environments/environment.development.ts`
```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api',
  defaultEstablishmentId: '11111111-1111-1111-1111-111111111111'
};
```

### `app.config.ts`
Habilitação do cliente HTTP moderno do Angular com `fetch API`:
```typescript
export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes, withComponentInputBinding()),
    provideHttpClient(withFetch()),
    provideAnimationsAsync(),
    providePrimeNG({ theme: { preset: Aura } })
  ]
};
```

---

## 3. Serviços Integrados e Endpoints Consumidos

| Serviço Angular | Métodos Principais | Endpoint REST Backend | Fallback Local |
| :--- | :--- | :--- | :--- |
| **`AppointmentService`** | `getAppointments`, `createAppointment`, `updateStatus`, `deleteAppointment`, `getAvailableSlots` | `GET/POST /api/establishments/{id}/appointments`<br>`PATCH /api/establishments/{id}/appointments/{id}/status`<br>`GET /api/establishments/{id}/availability` | Detecção de conflitos de horário local e persistência em signal |
| **`CustomerService`** | `getCustomers`, `getInactiveCustomers`, `getCustomerById`, `addCustomer`, `updateCustomer`, `deleteCustomer` | `GET/POST /api/establishments/{id}/customers`<br>`GET /api/establishments/{id}/customers/inactive`<br>`PUT/DELETE /api/establishments/{id}/customers/{id}` | Busca local e paginação em signal |
| **`ProfessionalService`** | `getProfessionals`, `getActiveProfessionals`, `getProfessionalsByService`, `addProfessional`, `updateProfessional`, `deleteProfessional` | `GET/POST /api/establishments/{id}/professionals`<br>`GET /api/establishments/{id}/professionals/by-service/{serviceId}`<br>`PUT/DELETE /api/establishments/{id}/professionals/{id}` | Filtro local por status e especialidade |
| **`ServiceCatalogService`** | `getServices`, `getActiveServices`, `getServiceById`, `addService`, `updateService`, `deleteService` | `GET/POST /api/establishments/{id}/services`<br>`PUT/DELETE /api/establishments/{id}/services/{id}` | Catálogo em signal |
| **`BusinessService`** | `getBusinessInfo`, `updateBusinessInfo` | `GET/PUT /api/establishments/{id}` | Dados da barbearia / salão em signal |
| **`DashboardService`** | `getDashboardStats` | `GET /api/establishments/{id}/dashboard` | Agregação dinâmica de KPIs e gráficos |

---

## 4. Tratamento de Erros e Regras de Negócio

1. **Prevenção de Colisão e Overbooking**:
   * Quando o backend retorna `HTTP 409 Conflict`, o `AppointmentService` intercepta e repassa a mensagem com o motivo exato (`Horário indisponível`).
   * No modo fallback local, o serviço executa o mesmo algoritmo de verificação de sobreposição de intervalos:
   $$[start_1, end_1) \cap [start_2, end_2) \neq \emptyset$$
2. **Exclusões e Retornos Booleanos**:
   * Endpoints `DELETE` retornam `204 No Content` no backend, mapeados de forma reativa para `Observable<boolean>` no Angular através do operador `map(() => true)` garantindo tipagem estrita com TypeScript.

---

## 5. Como Testar a Integração Ponta a Ponta

### 1. Iniciar o Backend
```bash
cd backend
mvn spring-boot:run
```
*O backend subirá na porta 8080 conectando ao PostgreSQL local (ou H2 em perfil de teste).*

### 2. Iniciar o Frontend
```bash
cd frontend
npm start
```
*Acesse `http://localhost:4200`.*

### 3. Validação dos Fluxos
1. **Painel Admin**: Acesse `/dashboard` e veja os indicadores sincronizados com o banco de dados.
2. **Agenda Interativa**: Acesse `/agenda`, crie um agendamento e verifique o bloqueio em tempo real de horários concorrentes.
3. **Agendamento Público (Sem Senha)**: Acesse `/agendar`, selecione serviço, profissional, data, horário disponível e confirme com nome e WhatsApp.
