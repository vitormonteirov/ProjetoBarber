package dev.vitor.barbearia.ProjetoBarber.barbeiro;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record BloqueioAgendaDTO(
        Long barbeiroId,

        @NotNull(message = "A data/hora de início do bloqueio é obrigatória.")
        LocalDateTime dataHoraInicio,

        @NotNull(message = "A data/hora de término do bloqueio é obrigatória.")
        LocalDateTime dataHoraFim,

        @NotBlank(message = "Informe o motivo do bloqueio/folga.")
        @Size(max = 255, message = "O motivo deve ter no máximo 255 caracteres.")
        String motivo
) {}
