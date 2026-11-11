package dev.vitor.barbearia.ProjetoBarber.agendamento;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

public record AgendamentoDTO(
    Long clienteId,
    @NotNull(message = "O ID do barbeiro é obrigatório.") Long barbeiroId,
    Long servicoId,
    List<Long> servicoIds,
    @NotNull(message = "A data e hora do agendamento são obrigatórias.")
    @Future(message = "O agendamento deve ser para um horário no futuro.") LocalDateTime dataHora,
    String nomeNaoUsuario,
    String telefoneNaoUsuario
) {
    public AgendamentoDTO(
            Long clienteId,
            Long barbeiroId,
            Long servicoId,
            LocalDateTime dataHora,
            String nomeNaoUsuario,
            String telefoneNaoUsuario) {
        this(clienteId, barbeiroId, servicoId, null, dataHora, nomeNaoUsuario, telefoneNaoUsuario);
    }

    public AgendamentoDTO(Long clienteId, Long barbeiroId, LocalDateTime dataHora) {
        this(clienteId, barbeiroId, null, null, dataHora, null, null);
    }

    public AgendamentoDTO(Long clienteId, Long barbeiroId, Long servicoId, LocalDateTime dataHora) {
        this(clienteId, barbeiroId, servicoId, null, dataHora, null, null);
    }
}
