-- =============================================================================
-- Migration V2: Massa de Dados Inicial para Desenvolvimento (Seed Data)
-- =============================================================================

-- 1. Inserir Estabelecimento Padrão
INSERT INTO establishments (
    id, name, slug, tagline, description, category, phone, whatsapp,
    street, number, neighborhood, city, state, zip_code,
    logo_url, banner_url, rating, total_reviews
) VALUES (
    'a0000000-0000-0000-0000-000000000001',
    'Barbearia & Studio Dom Pedro',
    'barbearia-dom-pedro',
    'Estilo, precisão e tradição no coração da cidade',
    'Especialistas em cortes clássicos, modernos, barba na toalha quente e cuidados masculinos premium. Ambiente climatizado com café expresso e cerveja artesanal.',
    'barbearia',
    '(11) 3456-7890',
    '(11) 98765-4321',
    'Av. Paulista',
    '1200',
    'Bela Vista',
    'São Paulo',
    'SP',
    '01310-100',
    'https://images.unsplash.com/photo-1503951914875-452162b0f3f1?auto=format&fit=crop&w=200&q=80',
    'https://images.unsplash.com/photo-1585747860715-2ba37e788b70?auto=format&fit=crop&w=1200&q=80',
    4.90,
    328
);

-- 2. Inserir Serviços
INSERT INTO services (id, establishment_id, name, category, description, price, duration_minutes, active, popular) VALUES
('b0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001', 'Corte Cabelo Executivo', 'Cabelo', 'Corte personalizado na tesoura ou máquina, com lavagem refrescante e pomada matte.', 65.00, 45, true, true),
('b0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000001', 'Barba Terapia com Toalha Quente', 'Barba', 'Modelagem da barba com navalhete descartável, óleos essenciais e toalha quente relaxante.', 55.00, 40, true, true),
('b0000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000001', 'Combo Dom Pedro (Corte + Barba)', 'Combos', 'Experiência completa com corte de cabelo, barba terapia completa e massagem capilar.', 110.00, 80, true, true),
('b0000000-0000-0000-0000-000000000004', 'a0000000-0000-0000-0000-000000000001', 'Camuflagem de Fios Brancos', 'Tratamentos', 'Tonalização natural para disfarçar cabelos ou barba grisalha sem aspecto artificial.', 75.00, 30, true, false),
('b0000000-0000-0000-0000-000000000005', 'a0000000-0000-0000-0000-000000000001', 'Limpeza de Pele Express & Argiloterapia', 'Estética', 'Remoção de impurezas superficiais e aplicação de máscara facial calmante.', 90.00, 50, true, false),
('b0000000-0000-0000-0000-000000000006', 'a0000000-0000-0000-0000-000000000001', 'Design de Sobrancelha Masculina', 'Estética', 'Alinhamento natural com pinça ou navalha mantendo a anatomia masculina.', 30.00, 20, true, false);

-- 3. Inserir Profissionais
INSERT INTO professionals (id, establishment_id, name, role, email, phone, avatar_url, bio, rating, total_appointments, active) VALUES
('c0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001', 'Carlos "Navalha" Mendes', 'Master Barber & Visagista', 'carlos.mendes@agendapro.com', '(11) 98111-2233', 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=200&q=80', 'Mais de 10 anos de experiência em cortes clássicos e visagismo.', 4.95, 1420, true),
('c0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000001', 'Rafael Silveira', 'Barbeiro Especialista em Fade', 'rafael.silveira@agendapro.com', '(11) 98222-3344', 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=200&q=80', 'Especialista em degrade navalhado e tendências contemporâneas.', 4.88, 980, true),
('c0000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000001', 'Dra. Beatriz Fontana', 'Terapeuta Capilar & Esteticista', 'beatriz.fontana@agendapro.com', '(11) 98333-4455', 'https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?auto=format&fit=crop&w=200&q=80', 'Foco em saúde do couro cabeludo e tratamentos faciais.', 4.98, 650, true);

-- 4. Inserir Especialidades dos Profissionais
INSERT INTO professional_services (professional_id, service_id) VALUES
('c0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000001'),
('c0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000002'),
('c0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000003'),
('c0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000004'),
('c0000000-0000-0000-0000-000000000002', 'b0000000-0000-0000-0000-000000000001'),
('c0000000-0000-0000-0000-000000000002', 'b0000000-0000-0000-0000-000000000002'),
('c0000000-0000-0000-0000-000000000002', 'b0000000-0000-0000-0000-000000000003'),
('c0000000-0000-0000-0000-000000000003', 'b0000000-0000-0000-0000-000000000004'),
('c0000000-0000-0000-0000-000000000003', 'b0000000-0000-0000-0000-000000000005'),
('c0000000-0000-0000-0000-000000000003', 'b0000000-0000-0000-0000-000000000006');

-- 5. Inserir Clientes
INSERT INTO customers (id, establishment_id, name, phone, email, avatar_url, status, notes, last_visit_at) VALUES
('d0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001', 'Lucas Gabriel Albuquerque', '(11) 99123-4567', 'lucas.albuquerque@gmail.com', 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=150&q=80', 'VIP', 'Prefere corte com tesoura nas laterais.', CURRENT_TIMESTAMP - INTERVAL '7 days'),
('d0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000001', 'Guilherme Rocha Lima', '(11) 99234-5678', 'guilherme.rocha@outlook.com', 'https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?auto=format&fit=crop&w=150&q=80', 'REGULAR', 'Costuma agendar sempre às sextas-feiras.', CURRENT_TIMESTAMP - INTERVAL '15 days'),
('d0000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000001', 'Felipe Santana Martins', '(11) 99345-6789', 'felipe.martins@hotmail.com', 'https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?auto=format&fit=crop&w=150&q=80', 'INACTIVE', 'Cliente que não retorna há mais de 45 dias.', CURRENT_TIMESTAMP - INTERVAL '58 days'),
('d0000000-0000-0000-0000-000000000004', 'a0000000-0000-0000-0000-000000000001', 'Thiago Henrique Neves', '(11) 99456-7890', 'thiago.neves@gmail.com', 'https://images.unsplash.com/photo-1522075469751-3a6694fb2f61?auto=format&fit=crop&w=150&q=80', 'VIP', 'Faz sempre barba terapia + corte executivo.', CURRENT_TIMESTAMP - INTERVAL '1 day');

-- 6. Inserir Agendamentos de Demonstração
INSERT INTO appointments (
    id, establishment_id, customer_id, professional_id, service_id,
    appointment_date, start_time, end_time, price, duration_minutes, status, is_public_booking, notes
) VALUES
('e0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000003', CURRENT_DATE, '09:00:00', '10:20:00', 110.00, 80, 'COMPLETED', false, 'Cliente pontual.'),
('e0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000002', 'c0000000-0000-0000-0000-000000000002', 'b0000000-0000-0000-0000-000000000001', CURRENT_DATE, '10:30:00', '11:15:00', 65.00, 45, 'IN_PROGRESS', false, 'Degrade baixo nas laterais.'),
('e0000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000004', 'c0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000002', CURRENT_DATE, '14:00:00', '14:40:00', 55.00, 40, 'CONFIRMED', false, 'Barba terapia com toalha quente.');
