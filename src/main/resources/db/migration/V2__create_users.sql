-- =============================================================================
-- Migration V2: Usuários (donos de estabelecimento)
-- =============================================================================
-- O dono cria a conta primeiro (establishment_id nulo) e depois cadastra o
-- estabelecimento no painel, quando o vínculo é preenchido.
CREATE TABLE users (
    id UUID PRIMARY KEY,
    establishment_id UUID,
    name VARCHAR(150) NOT NULL,
    email VARCHAR(150) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'ADMIN',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_establishment FOREIGN KEY (establishment_id) REFERENCES establishments(id) ON DELETE SET NULL,
    CONSTRAINT uk_user_email UNIQUE (email),
    CONSTRAINT chk_user_role CHECK (role IN ('ADMIN', 'PROFESSIONAL'))
);

CREATE INDEX idx_users_establishment ON users(establishment_id);
