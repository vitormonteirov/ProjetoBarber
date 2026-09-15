package dev.vitor.barbearia.ProjetoBarber.agendamento;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

record AgendamentoDTO(
    @NotNull(message = "O ID do cliente é obrigatório.") Long clienteId,
    @NotNull(message = "O ID do barbeiro é obrigatório.") Long barbeiroId,
    @NotNull(message = "A data e hora do agendamento são obrigatórias.")
    @Future(message = "O agendamento deve ser para um horário no futuro.") LocalDateTime dataHora
) {}
