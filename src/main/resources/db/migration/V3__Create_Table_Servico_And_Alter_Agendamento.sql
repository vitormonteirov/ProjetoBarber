CREATE TABLE servico (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    descricao VARCHAR(255),
    preco DECIMAL(10, 2) NOT NULL,
    duracao_minutos INT NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE
);

ALTER TABLE barbeiro ADD COLUMN ativo BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE cliente ADD COLUMN ativo BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE agendamento ADD COLUMN servico_id BIGINT;
ALTER TABLE agendamento ADD COLUMN data_hora_fim DATETIME;
ALTER TABLE agendamento ADD COLUMN preco DECIMAL(10, 2);

ALTER TABLE agendamento ADD CONSTRAINT fk_agendamento_servico FOREIGN KEY (servico_id) REFERENCES servico(id);

CREATE INDEX idx_agendamento_barbeiro_data ON agendamento (barbeiro_id, data_hora, status);
CREATE INDEX idx_agendamento_cliente_data ON agendamento (cliente_id, data_hora, status);
