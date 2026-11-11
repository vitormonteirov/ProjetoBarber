package dev.vitor.barbearia.ProjetoBarber.gestor;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

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
    String telefone,

    @DecimalMin(value = "0.00", message = "O percentual de comissão não pode ser menor que 0%.")
    @DecimalMax(value = "100.00", message = "O percentual de comissão não pode ultrapassar 100%.")
    BigDecimal percentualComissao
) {
    public CriarBarbeiroUserDTO(String nome, String email, String senha, String telefone) {
        this(nome, email, senha, telefone, null);
    }
}
