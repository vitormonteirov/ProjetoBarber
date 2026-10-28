package dev.vitor.barbearia.ProjetoBarber.agendamento;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record AgendamentoDTO(
    Long clienteId,
    @NotNull(message = "O ID do barbeiro é obrigatório.") Long barbeiroId,
    Long servicoId,
    @NotNull(message = "A data e hora do agendamento são obrigatórias.")
    @Future(message = "O agendamento deve ser para um horário no futuro.") LocalDateTime dataHora,
    String nomeNaoUsuario,
    String telefoneNaoUsuario
) {
    public AgendamentoDTO(Long clienteId, Long barbeiroId, LocalDateTime dataHora) {
        this(clienteId, barbeiroId, null, dataHora, null, null);
    }

    public AgendamentoDTO(Long clienteId, Long barbeiroId, Long servicoId, LocalDateTime dataHora) {
        this(clienteId, barbeiroId, servicoId, dataHora, null, null);
    }
}
