package dev.vitor.barbearia.ProjetoBarber.barbeiro;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record BarbeiroDTO(
    @NotBlank(message = "O nome é obrigatório.") String nome,
    @NotBlank(message = "O e-mail é obrigatório.") @Email(message = "O formato de e-mail é inválido.") String email,
    @NotBlank(message = "O telefone é obrigatório.") @Size(min = 10, max = 15, message = "O telefone deve ter entre 10 e 15 números.") String telefone
) {}
