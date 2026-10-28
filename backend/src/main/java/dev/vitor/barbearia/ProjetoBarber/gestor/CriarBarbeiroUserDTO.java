package dev.vitor.barbearia.ProjetoBarber.gestor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CriarBarbeiroUserDTO(
    @NotBlank(message = "O nome é obrigatório.")
    String nome,

    @NotBlank(message = "O e-mail é obrigatório.")
    @Email(message = "Formato de e-mail inválido.")
    String email,

    @NotBlank(message = "A senha inicial é obrigatória.")
    @Size(min = 6, message = "A senha deve conter pelo menos 6 caracteres.")
    String senha,

    @NotBlank(message = "O telefone é obrigatório.")
    String telefone
) {}
