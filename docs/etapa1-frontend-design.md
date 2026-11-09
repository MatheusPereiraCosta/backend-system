# Documento de Arquitetura e Decisões — ETAPA 1: Protótipo Visual

## 1. Visão Geral da Arquitetura do Frontend

O frontend foi construído utilizando as recomendações mais recentes do ecossistema **Angular 19**:

* **Standalone Components**: Eliminação de `NgModules` em favor de componentes independentes e leves, permitindo tree-shaking otimizado e lazy loading granular.
* **Reatividade Híbrida (Signals + RxJS)**: O estado da aplicação (agendamentos, clientes, serviços) é mantido com `Signals` do Angular no nível de store de serviço, proporcionando re-renderização precisa e previsível. Operações assíncronas continuam utilizando `RxJS` (`Observable`), garantindo compatibilidade direta para quando a API REST for conectada na Etapa 4.
* **Isolamento de Contrato de Dados (Mock Layer)**: Todos os dados fictícios residem em [`core/services/mock/mock-data.ts`](../frontend/src/app/core/services/mock/mock-data.ts), sendo servidos por serviços injetáveis (`AppointmentService`, `CustomerService`, etc.). Isso significa que nenhuma tela possui dados hardcoded — a substituição pelo backend nas etapas seguintes será transparente para as views.

---

## 2. Padrões de Design e Identidade Visual

* **Paleta de Cores**: Baseada em tons Slate neutros com acentos `Indigo` (#4F46E5) para ações do sistema, `Emerald` (#10B981) para valores financeiros e status positivo, e `Amber` (#F59E0B) para alertas e atenção.
* **Tipografia**: Família *Inter* com hierarquia tipográfica estruturada para leitura ágil de valores e horários em dashboards.
* **Componentes**: PrimeNG 19 com tema moderno (`Aura`), integrando janelas modais (`p-dialog`), seletores e tabelas estilizadas via Tailwind CSS.
* **Gráficos**: ApexCharts com suporte a modo claro/escuro, gradientes suaves e visualização responsiva de métricas.

---

## 3. Fluxo de Navegação

```
                    [Aplicação Frontend]
                             │
            ┌────────────────┴────────────────┐
            ▼                                 ▼
   [Painel Administrativo]           [Página Pública]
       (/admin/...)                     (/agendar)
            │                                 │
   ├─► Dashboard Executivo            ├─► 1. Escolher Serviço
   ├─► Agenda Diária & Semanal        ├─► 2. Escolher Profissional
   ├─► Gestão de Clientes             ├─► 3. Escolher Data & Horário
   ├─► Serviços & Preços              └─► 4. Confirmação & Resumo
   ├─► Equipe & Horários
   └─► Retenção & Reativação
```
