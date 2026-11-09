# Documentação Técnica — ETAPA 3: APIs REST & Regras de Negócio

## 1. Endpoints Disponíveis na API

A base URL é `/api`.

### 🏢 Estabelecimentos (`/api/establishments`)
* `GET /{id}` — Detalhes do estabelecimento por ID
* `GET /slug/{slug}` — Busca pública do estabelecimento para carregar a página de agendamento (`/barbearia-dom-pedro`)
* `PUT /{id}` — Atualização cadastral do estabelecimento

### ✂️ Serviços (`/api/establishments/{establishmentId}/services`)
* `GET /` — Lista todos os serviços (filtro opcional `?activeOnly=true`)
* `GET /{id}` — Detalhes de um serviço específico
* `POST /` — Criação de novo serviço com validação de preço e duração
* `PUT /{id}` — Atualização do serviço
* `DELETE /{id}` — Remoção de serviço

### 💈 Profissionais (`/api/establishments/{establishmentId}/professionals`)
* `GET /` — Lista todos os profissionais e seus horários de expediente
* `GET /by-service/{serviceId}` — Lista os profissionais habilitados para determinado serviço
* `GET /{id}` — Detalhes do profissional
* `POST /` — Cadastro de profissional com vínculos de serviços e horários
* `PUT /{id}` — Atualização de dados cadastrais
* `DELETE /{id}` — Exclusão lógica/física

### 👥 Clientes (`/api/establishments/{establishmentId}/customers`)
* `GET /` — Lista paginada com suporte a busca (`?search=lucas`) e filtro de status (`?status=VIP`)
* `GET /inactive` — Lista inteligente de clientes inativos para retenção (`?daysThreshold=30`)
* `GET /{id}` — Histórico e perfil do cliente
* `POST /` — Cadastro manual de cliente
* `PUT /{id}` — Atualização de dados e preferências
* `DELETE /{id}` — Remoção de cliente

### ⏰ Engine de Disponibilidade (`/api/establishments/{establishmentId}/availability`)
* `GET /?professionalId={id}&serviceId={id}&date={YYYY-MM-DD}` — Calcula os slots livres do profissional, descontando intervalos e agendamentos existentes.

### 📅 Agendamentos (`/api/establishments/{establishmentId}/appointments`)
* `GET /?date={YYYY-MM-DD}` — Lista os agendamentos do dia
* `GET /?startDate={D1}&endDate={D2}` — Lista agendamentos por intervalo de datas
* `GET /?professionalId={id}&date={YYYY-MM-DD}` — Lista agendamentos do profissional no dia
* `POST /` — **Criação de agendamento com validação estrita de concorrência**. Se o horário estiver ocupado, retorna `409 Conflict`.
* `PATCH /{id}/status` — Transição de status (`CONFIRMED`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`).
* `DELETE /{id}` — Cancelamento/remoção de agendamento.

### 📊 Dashboard & Métricas (`/api/establishments/{establishmentId}/dashboard`)
* `GET /` — Retorna faturamento diário, mensal, contagens de clientes, evolução semanal (7 dias) e distribuição de status.
