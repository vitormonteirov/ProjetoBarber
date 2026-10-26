package dev.vitor.barbearia.ProjetoBarber.auth;

import dev.vitor.barbearia.ProjetoBarber.usuario.Role;

public record LoginResponseDTO(
    String token,
    String tipo,
    Long id,
    String nome,
    String email,
    Role role
) {
    public LoginResponseDTO(String token, Long id, String nome, String email, Role role) {
        this(token, "Bearer", id, nome, email, role);
    }
}
