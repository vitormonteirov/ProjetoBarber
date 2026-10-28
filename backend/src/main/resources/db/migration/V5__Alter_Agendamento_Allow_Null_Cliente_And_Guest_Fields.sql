ALTER TABLE agendamento MODIFY COLUMN cliente_id BIGINT NULL;
ALTER TABLE agendamento ADD COLUMN nome_nao_usuario VARCHAR(255);
ALTER TABLE agendamento ADD COLUMN telefone_nao_usuario VARCHAR(50);
