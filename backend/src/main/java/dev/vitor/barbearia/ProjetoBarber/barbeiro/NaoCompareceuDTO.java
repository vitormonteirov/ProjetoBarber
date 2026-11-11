package dev.vitor.barbearia.ProjetoBarber.barbeiro;

import jakarta.validation.constraints.Size;

public record NaoCompareceuDTO(
        @Size(max = 255, message = "A observação de ausência deve ter no máximo 255 caracteres.")
        String observacao
) {}
