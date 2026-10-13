package dev.vitor.barbearia.ProjetoBarber.cliente;

public record ClienteResponseDTO(Long id, String nome, String telefone, String email, Boolean ativo) {
    public ClienteResponseDTO(Long id, String nome, String telefone, String email) {
        this(id, nome, telefone, email, true);
    }
}
