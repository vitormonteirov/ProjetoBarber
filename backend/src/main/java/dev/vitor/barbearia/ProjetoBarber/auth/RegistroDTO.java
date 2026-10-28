package dev.vitor.barbearia.ProjetoBarber.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistroDTO(
    @NotBlank(message = "O nome é obrigatório.")
    String nome,

    @NotBlank(message = "O e-mail é obrigatório.")
    @Email(message = "Formato de e-mail inválido.")
    String email,

    @NotBlank(message = "A senha é obrigatória.")
    @Size(min = 6, message = "A senha deve conter pelo menos 6 caracteres.")
    String senha,

    String telefone
) {}
