package dev.vitor.barbearia.ProjetoBarber.barbeiro;

public record BarbeiroResponseDTO(Long id, String nome, String email, String telefone, Boolean ativo) {
    public BarbeiroResponseDTO(Long id, String nome, String email, String telefone) {
        this(id, nome, email, telefone, true);
    }
}
