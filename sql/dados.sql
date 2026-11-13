-- =====================================================
-- Dados de teste
-- Correr DEPOIS do schema.sql (tabelas vazias, ids comecam em 1)
-- =====================================================

-- Nota: os dados nao tem acentos de proposito.
-- A consola do Windows (cmd) tem problemas com acentos ao escrever/ler,
-- e assim o filtro por cidade funciona com "evora" ou "Evora".

-- -----------------------------------------------------
-- UTILIZADORES (ficam com id 1, 2, 3, 4)
-- -----------------------------------------------------
INSERT INTO utilizador (nome, email, matricula, tipo_ficha) VALUES
    ('Guilherme Pinto',  'guilherme.pinto@mail.pt',  'AA-12-BB', 'CCS'),      -- id 1
    ('Guilherme Santos', 'guilherme.santos@mail.pt', '12-CD-34', 'TIPO2'),    -- id 2
    ('Pedro Santos',     'pedro.santos@mail.pt',     '56-EF-78', 'CHADEMO'),  -- id 3
    ('Ana Ferreira',     'ana.ferreira@mail.pt',     'BC-90-DE', 'CCS');      -- id 4

-- -----------------------------------------------------
-- POSTOS (ficam com id 1 a 7)
-- -----------------------------------------------------
INSERT INTO posto (codigo, localizacao, cidade, potencia_kw, tipo_ficha, preco_kwh, estado, estado_admin) VALUES
    ('EVR-001', 'Parque do Rossio',          'Evora',  50,  'CCS',     0.35, 'RESERVADO',     'APROVADO'),  -- id 1
    ('EVR-002', 'Praca do Giraldo',          'Evora',  22,  'TIPO2',   0.25, 'DISPONIVEL',    'APROVADO'),  -- id 2
    ('EVR-003', 'Colegio do Espirito Santo', 'Evora',  150, 'CCS',     0.45, 'DISPONIVEL',    'APROVADO'),  -- id 3
    ('EVR-004', 'Evora Plaza',               'Evora',  50,  'CHADEMO', 0.38, 'EM_MANUTENCAO', 'APROVADO'),  -- id 4
    ('LSB-001', 'Parque das Nacoes',         'Lisboa', 150, 'CCS',     0.49, 'DISPONIVEL',    'APROVADO'),  -- id 5
    ('LSB-002', 'Avenida da Liberdade',      'Lisboa', 22,  'TIPO2',   0.29, 'DISPONIVEL',    'PENDENTE'),  -- id 6
    ('BJA-001', 'Centro de Beja',            'Beja',   50,  'CCS',     0.36, 'DISPONIVEL',    'PENDENTE');  -- id 7

-- -----------------------------------------------------
-- RESERVAS
-- CURRENT_DATE = hoje.  CURRENT_DATE + 1 = amanha.
-- CURRENT_DATE + 1 + TIME '14:00' = amanha as 14h
-- (assim as reservas ficam sempre no futuro, seja qual for o dia)
-- -----------------------------------------------------
INSERT INTO reserva (utilizador_id, posto_id, data_inicio, data_fim, estado) VALUES
    -- Guilherme Pinto: EVR-001, amanha das 14h as 15h
    (1, 1, CURRENT_DATE + 1 + TIME '14:00', CURRENT_DATE + 1 + TIME '15:00', 'ATIVA'),
    -- Ana Ferreira: EVR-001, amanha das 16h as 17h30
    (4, 1, CURRENT_DATE + 1 + TIME '16:00', CURRENT_DATE + 1 + TIME '17:30', 'ATIVA'),
    -- Guilherme Santos: EVR-002, ha 2 dias das 10h as 11h (ja acabou)
    (2, 2, CURRENT_DATE - 2 + TIME '10:00', CURRENT_DATE - 2 + TIME '11:00', 'CONCLUIDA');

-- -----------------------------------------------------
-- HISTORICO (conta como cada posto chegou ao estado atual)
-- -----------------------------------------------------
INSERT INTO historico (posto_id, reserva_id, tipo, estado_anterior, estado_novo, descricao, data_hora) VALUES
    -- registo dos 7 postos (ha 10 dias)
    (1, NULL, 'REGISTO', NULL, 'PENDENTE', 'Posto registado', CURRENT_DATE - 10 + TIME '09:00'),
    (2, NULL, 'REGISTO', NULL, 'PENDENTE', 'Posto registado', CURRENT_DATE - 10 + TIME '09:00'),
    (3, NULL, 'REGISTO', NULL, 'PENDENTE', 'Posto registado', CURRENT_DATE - 10 + TIME '09:00'),
    (4, NULL, 'REGISTO', NULL, 'PENDENTE', 'Posto registado', CURRENT_DATE - 10 + TIME '09:00'),
    (5, NULL, 'REGISTO', NULL, 'PENDENTE', 'Posto registado', CURRENT_DATE - 10 + TIME '09:00'),
    (6, NULL, 'REGISTO', NULL, 'PENDENTE', 'Posto registado', CURRENT_DATE - 1  + TIME '09:00'),
    (7, NULL, 'REGISTO', NULL, 'PENDENTE', 'Posto registado', CURRENT_DATE - 1  + TIME '09:00'),

    -- aprovacao dos postos 1 a 5 (ha 9 dias)
    (1, NULL, 'APROVACAO', 'PENDENTE', 'APROVADO', 'Posto aprovado', CURRENT_DATE - 9 + TIME '09:00'),
    (2, NULL, 'APROVACAO', 'PENDENTE', 'APROVADO', 'Posto aprovado', CURRENT_DATE - 9 + TIME '09:00'),
    (3, NULL, 'APROVACAO', 'PENDENTE', 'APROVADO', 'Posto aprovado', CURRENT_DATE - 9 + TIME '09:00'),
    (4, NULL, 'APROVACAO', 'PENDENTE', 'APROVADO', 'Posto aprovado', CURRENT_DATE - 9 + TIME '09:00'),
    (5, NULL, 'APROVACAO', 'PENDENTE', 'APROVADO', 'Posto aprovado', CURRENT_DATE - 9 + TIME '09:00'),

    -- posto 2: reserva #3 do Guilherme Santos (criada e depois concluida)
    (2, 3, 'RESERVA_CRIADA',    'DISPONIVEL', 'RESERVADO',  'Reserva #3 criada',    CURRENT_DATE - 3 + TIME '18:00'),
    (2, 3, 'RESERVA_CONCLUIDA', 'RESERVADO',  'DISPONIVEL', 'Reserva #3 concluida', CURRENT_DATE - 2 + TIME '11:00'),

    -- posto 4: foi para manutencao (ontem)
    (4, NULL, 'ESTADO_ALTERADO', 'DISPONIVEL', 'EM_MANUTENCAO', 'Manutencao preventiva', CURRENT_DATE - 1 + TIME '08:00'),

    -- posto 1: reservas #1 e #2 (hoje de manha)
    (1, 1, 'RESERVA_CRIADA', 'DISPONIVEL', 'RESERVADO', 'Reserva #1 criada', CURRENT_DATE + TIME '08:00'),
    (1, 2, 'RESERVA_CRIADA', 'RESERVADO',  'RESERVADO', 'Reserva #2 criada', CURRENT_DATE + TIME '08:30');
