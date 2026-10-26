package dev.vitor.barbearia.ProjetoBarber.barbeiro;

import jakarta.validation.constraints.NotBlank;

public record CancelarAgendamentoBarbeiroDTO(
    @NotBlank(message = "A nota de alteração/motivo do cancelamento é obrigatória.")
    String motivo
) {}
