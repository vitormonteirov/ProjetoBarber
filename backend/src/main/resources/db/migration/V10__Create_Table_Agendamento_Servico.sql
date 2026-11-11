CREATE TABLE agendamento_servico (
    agendamento_id BIGINT NOT NULL,
    servico_id BIGINT NOT NULL,
    PRIMARY KEY (agendamento_id, servico_id),
    CONSTRAINT fk_agendamento_servico_agendamento FOREIGN KEY (agendamento_id) REFERENCES agendamento(id) ON DELETE CASCADE,
    CONSTRAINT fk_agendamento_servico_servico FOREIGN KEY (servico_id) REFERENCES servico(id)
);

CREATE INDEX idx_agendamento_servico_servico_id ON agendamento_servico(servico_id);
