package dev.vitor.barbearia.ProjetoBarber.barbeiro;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalTime;

public record BarbeiroDTO(
    @NotBlank(message = "O nome é obrigatório.") String nome,
    @NotBlank(message = "O e-mail é obrigatório.") @Email(message = "O formato de e-mail é inválido.") String email,
    @NotBlank(message = "O telefone é obrigatório.") @Size(min = 10, max = 15, message = "O telefone deve ter entre 10 e 15 números.") String telefone,
    LocalTime horarioInicioExpediente,
    LocalTime horarioFimExpediente,
    LocalTime horarioInicioPausa,
    LocalTime horarioFimPausa
) {
    public BarbeiroDTO(String nome, String email, String telefone) {
        this(nome, email, telefone, null, null, null, null);
    }
}
