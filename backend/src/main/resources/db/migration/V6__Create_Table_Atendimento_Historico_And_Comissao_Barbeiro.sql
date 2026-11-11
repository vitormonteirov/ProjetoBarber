-- Percentual de comissão configurável por barbeiro (0 a 100%)
ALTER TABLE barbeiro ADD COLUMN percentual_comissao DECIMAL(5, 2) NOT NULL DEFAULT 50.00;

-- Histórico imutável de atendimentos concluídos.
-- Não utiliza chaves estrangeiras para barbeiro/servico/cliente de propósito:
-- mesmo que um barbeiro ou serviço deixe de existir, o histórico e o caixa permanecem íntegros.
CREATE TABLE atendimento_historico (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    agendamento_id BIGINT NOT NULL,
    barbeiro_id BIGINT NOT NULL,
    barbeiro_nome VARCHAR(255) NOT NULL,
    servico_id BIGINT NULL,
    servico_nome VARCHAR(255) NOT NULL,
    cliente_id BIGINT NULL,
    cliente_nome VARCHAR(255) NULL,
    cliente_nao_usuario BOOLEAN NOT NULL DEFAULT FALSE,
    data_hora_atendimento DATETIME NOT NULL,
    data_hora_conclusao DATETIME NOT NULL,
    valor_tabela DECIMAL(10, 2) NULL,
    valor_pago DECIMAL(10, 2) NOT NULL,
    forma_pagamento VARCHAR(30) NOT NULL,
    percentual_comissao DECIMAL(5, 2) NOT NULL,
    valor_comissao DECIMAL(10, 2) NOT NULL,
    concluido_por VARCHAR(255) NOT NULL,
    CONSTRAINT uk_historico_agendamento UNIQUE (agendamento_id)
);

CREATE INDEX idx_historico_data ON atendimento_historico (data_hora_atendimento);
CREATE INDEX idx_historico_barbeiro ON atendimento_historico (barbeiro_id, data_hora_atendimento);
