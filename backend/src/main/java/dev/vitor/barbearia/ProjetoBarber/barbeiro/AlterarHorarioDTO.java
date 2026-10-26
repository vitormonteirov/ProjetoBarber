package dev.vitor.barbearia.ProjetoBarber.barbeiro;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record AlterarHorarioDTO(
    @NotNull(message = "A nova data e hora são obrigatórias.")
    @Future(message = "O novo horário deve ser futuro.")
    LocalDateTime novaDataHora,

    @NotBlank(message = "A nota de alteração é obrigatória para rastreabilidade com o cliente.")
    String notaAlteracao
) {}
