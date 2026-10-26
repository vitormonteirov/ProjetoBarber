CREATE TABLE usuario (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    senha VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE
);

ALTER TABLE agendamento ADD COLUMN nota_alteracao VARCHAR(500);

CREATE INDEX idx_usuario_email ON usuario (email);
