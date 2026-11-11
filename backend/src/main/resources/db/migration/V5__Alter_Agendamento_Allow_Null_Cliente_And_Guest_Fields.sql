ALTER TABLE agendamento ALTER COLUMN cliente_id DROP NOT NULL;
ALTER TABLE agendamento ADD COLUMN nome_nao_usuario VARCHAR(255);
ALTER TABLE agendamento ADD COLUMN telefone_nao_usuario VARCHAR(50);
