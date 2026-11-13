-- =====================================================
-- Sistemas Distribuidos - Trabalho 1
-- Reserva de Postos de Carregamento
-- Correr como sd_user na base de dados "carregamentos"
-- =====================================================

-- Apaga as tabelas se ja existirem (para poder correr o script varias vezes)
-- A ordem importa: primeiro as que tem chaves estrangeiras
DROP TABLE IF EXISTS historico;
DROP TABLE IF EXISTS reserva;
DROP TABLE IF EXISTS posto;
DROP TABLE IF EXISTS utilizador;

-- -----------------------------------------------------
-- UTILIZADOR (condutor)
-- -----------------------------------------------------
CREATE TABLE utilizador (
    id          SERIAL       PRIMARY KEY,
    nome        VARCHAR(100) NOT NULL,
    email       VARCHAR(100) NOT NULL UNIQUE,
    matricula   VARCHAR(10)  NOT NULL UNIQUE,
    tipo_ficha  VARCHAR(10)  NOT NULL
                CHECK (tipo_ficha IN ('TIPO2', 'CCS', 'CHADEMO'))
);

-- -----------------------------------------------------
-- POSTO (elemento gerido)
-- -----------------------------------------------------
CREATE TABLE posto (
    id           SERIAL       PRIMARY KEY,
    codigo       VARCHAR(20)  NOT NULL UNIQUE,
    localizacao  VARCHAR(100) NOT NULL,
    cidade       VARCHAR(50)  NOT NULL,
    potencia_kw  NUMERIC(6,1) NOT NULL CHECK (potencia_kw > 0),
    tipo_ficha   VARCHAR(10)  NOT NULL
                 CHECK (tipo_ficha IN ('TIPO2', 'CCS', 'CHADEMO')),
    preco_kwh    NUMERIC(6,3) NOT NULL CHECK (preco_kwh >= 0),

    -- estado operacional (maquina de estados)
    estado       VARCHAR(20)  NOT NULL DEFAULT 'DISPONIVEL'
                 CHECK (estado IN ('DISPONIVEL', 'RESERVADO',
                                   'EM_MANUTENCAO', 'DESATIVADO')),

    -- estado administrativo: "nao aprovado" = PENDENTE ou REJEITADO
    estado_admin VARCHAR(20)  NOT NULL DEFAULT 'PENDENTE'
                 CHECK (estado_admin IN ('PENDENTE', 'APROVADO', 'REJEITADO'))
);

-- -----------------------------------------------------
-- RESERVA (operacao principal)
-- -----------------------------------------------------
CREATE TABLE reserva (
    id             SERIAL    PRIMARY KEY,
    utilizador_id  INT       NOT NULL REFERENCES utilizador(id),
    posto_id       INT       NOT NULL REFERENCES posto(id),
    data_inicio    TIMESTAMP NOT NULL,
    data_fim       TIMESTAMP NOT NULL,
    estado         VARCHAR(20) NOT NULL DEFAULT 'ATIVA'
                   CHECK (estado IN ('ATIVA', 'CANCELADA', 'CONCLUIDA')),
    criada_em      TIMESTAMP NOT NULL DEFAULT NOW(),

    CHECK (data_fim > data_inicio)
);

-- acelera a pesquisa de sobreposicoes (reservas de um posto num periodo)
CREATE INDEX idx_reserva_posto_datas ON reserva (posto_id, data_inicio, data_fim);

-- -----------------------------------------------------
-- HISTORICO (operacoes e alteracoes de estado)
-- -----------------------------------------------------
CREATE TABLE historico (
    id               SERIAL      PRIMARY KEY,
    posto_id         INT         NOT NULL REFERENCES posto(id),
    reserva_id       INT         REFERENCES reserva(id),   -- NULL se nao for sobre uma reserva
    tipo             VARCHAR(30) NOT NULL,
                     -- ex: REGISTO, APROVACAO, REJEICAO, EDICAO, ESTADO_ALTERADO,
                     --     RESERVA_CRIADA, RESERVA_CANCELADA
    estado_anterior  VARCHAR(20),
    estado_novo      VARCHAR(20),
    descricao        TEXT,
    data_hora        TIMESTAMP   NOT NULL DEFAULT NOW()
);
