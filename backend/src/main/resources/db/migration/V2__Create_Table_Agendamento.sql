CREATE TABLE agendamento (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cliente_id BIGINT NOT NULL,
    barbeiro_id BIGINT NOT NULL,
    data_hora DATETIME NOT NULL,
    status VARCHAR(50) NOT NULL,
    CONSTRAINT fk_agendamento_cliente FOREIGN KEY (cliente_id) REFERENCES cliente(id),
    CONSTRAINT fk_agendamento_barbeiro FOREIGN KEY (barbeiro_id) REFERENCES barbeiro(id)
);
