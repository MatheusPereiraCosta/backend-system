-- =============================================================================
-- Migration V1: Criação do Schema Inicial do AgendaPro
-- =============================================================================

-- 1. Tabela: Estabelecimentos (Multi-tenant)
CREATE TABLE establishments (
    id UUID PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    slug VARCHAR(100) NOT NULL UNIQUE,
    tagline VARCHAR(255),
    description TEXT,
    category VARCHAR(50) NOT NULL,
    phone VARCHAR(30),
    whatsapp VARCHAR(30) NOT NULL,
    street VARCHAR(150),
    number VARCHAR(30),
    neighborhood VARCHAR(100),
    city VARCHAR(100),
    state VARCHAR(10),
    zip_code VARCHAR(20),
    logo_url VARCHAR(500),
    banner_url VARCHAR(500),
    rating NUMERIC(3, 2) NOT NULL DEFAULT 5.00,
    total_reviews INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. Tabela: Clientes
CREATE TABLE customers (
    id UUID PRIMARY KEY,
    establishment_id UUID NOT NULL,
    name VARCHAR(150) NOT NULL,
    phone VARCHAR(30) NOT NULL,
    email VARCHAR(150),
    avatar_url VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'REGULAR',
    notes TEXT,
    last_visit_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_customer_establishment FOREIGN KEY (establishment_id) REFERENCES establishments(id) ON DELETE CASCADE,
    CONSTRAINT uk_customer_phone_establishment UNIQUE (establishment_id, phone),
    CONSTRAINT chk_customer_status CHECK (status IN ('VIP', 'REGULAR', 'INACTIVE'))
);

-- 3. Tabela: Profissionais / Colaboradores
CREATE TABLE professionals (
    id UUID PRIMARY KEY,
    establishment_id UUID NOT NULL,
    name VARCHAR(150) NOT NULL,
    role VARCHAR(100) NOT NULL,
    email VARCHAR(150),
    phone VARCHAR(30) NOT NULL,
    avatar_url VARCHAR(500),
    bio TEXT,
    rating NUMERIC(3, 2) NOT NULL DEFAULT 5.00,
    total_appointments INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_professional_establishment FOREIGN KEY (establishment_id) REFERENCES establishments(id) ON DELETE CASCADE
);

-- 4. Tabela: Serviços
CREATE TABLE services (
    id UUID PRIMARY KEY,
    establishment_id UUID NOT NULL,
    name VARCHAR(150) NOT NULL,
    category VARCHAR(100) NOT NULL,
    description TEXT,
    price NUMERIC(10, 2) NOT NULL,
    duration_minutes INTEGER NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    popular BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_service_establishment FOREIGN KEY (establishment_id) REFERENCES establishments(id) ON DELETE CASCADE,
    CONSTRAINT chk_service_price CHECK (price >= 0),
    CONSTRAINT chk_service_duration CHECK (duration_minutes > 0)
);

-- 5. Tabela N:N: Especialidades dos Profissionais (Professional <-> Services)
CREATE TABLE professional_services (
    professional_id UUID NOT NULL,
    service_id UUID NOT NULL,
    PRIMARY KEY (professional_id, service_id),
    CONSTRAINT fk_pro_services_pro FOREIGN KEY (professional_id) REFERENCES professionals(id) ON DELETE CASCADE,
    CONSTRAINT fk_pro_services_service FOREIGN KEY (service_id) REFERENCES services(id) ON DELETE CASCADE
);

-- 6. Tabela: Horários de Trabalho dos Profissionais
CREATE TABLE professional_working_hours (
    id UUID PRIMARY KEY,
    professional_id UUID NOT NULL,
    day_of_week SMALLINT NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    break_start TIME,
    break_end TIME,
    CONSTRAINT fk_working_hours_pro FOREIGN KEY (professional_id) REFERENCES professionals(id) ON DELETE CASCADE,
    CONSTRAINT uk_pro_day_of_week UNIQUE (professional_id, day_of_week),
    CONSTRAINT chk_day_of_week CHECK (day_of_week BETWEEN 0 AND 6)
);

-- 7. Tabela: Agendamentos
CREATE TABLE appointments (
    id UUID PRIMARY KEY,
    establishment_id UUID NOT NULL,
    customer_id UUID NOT NULL,
    professional_id UUID NOT NULL,
    service_id UUID NOT NULL,
    appointment_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    price NUMERIC(10, 2) NOT NULL,
    duration_minutes INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED',
    is_public_booking BOOLEAN NOT NULL DEFAULT FALSE,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_appointment_establishment FOREIGN KEY (establishment_id) REFERENCES establishments(id) ON DELETE CASCADE,
    CONSTRAINT fk_appointment_customer FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE RESTRICT,
    CONSTRAINT fk_appointment_professional FOREIGN KEY (professional_id) REFERENCES professionals(id) ON DELETE RESTRICT,
    CONSTRAINT fk_appointment_service FOREIGN KEY (service_id) REFERENCES services(id) ON DELETE RESTRICT,
    CONSTRAINT chk_appointment_status CHECK (status IN ('PENDING', 'CONFIRMED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT chk_appointment_times CHECK (start_time < end_time)
);

-- =============================================================================
-- ÍNDICES DE ALTA PERFORMANCE
-- =============================================================================
CREATE INDEX idx_appointments_schedule ON appointments(establishment_id, professional_id, appointment_date, status);
CREATE INDEX idx_appointments_dashboard ON appointments(establishment_id, appointment_date, status);
CREATE INDEX idx_customers_establishment_search ON customers(establishment_id, name, phone);
CREATE INDEX idx_services_establishment ON services(establishment_id, category, active);
CREATE INDEX idx_establishments_slug ON establishments(slug);
