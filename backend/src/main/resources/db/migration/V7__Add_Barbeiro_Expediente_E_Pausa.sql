ALTER TABLE barbeiro ADD COLUMN horario_inicio_expediente TIME NOT NULL DEFAULT '08:00:00';
ALTER TABLE barbeiro ADD COLUMN horario_fim_expediente TIME NOT NULL DEFAULT '19:00:00';
ALTER TABLE barbeiro ADD COLUMN horario_inicio_pausa TIME NULL DEFAULT '12:00:00';
ALTER TABLE barbeiro ADD COLUMN horario_fim_pausa TIME NULL DEFAULT '13:00:00';
