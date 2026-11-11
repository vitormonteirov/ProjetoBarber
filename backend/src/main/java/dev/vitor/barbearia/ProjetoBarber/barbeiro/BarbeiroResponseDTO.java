package dev.vitor.barbearia.ProjetoBarber.barbeiro;

import java.math.BigDecimal;

public record BarbeiroResponseDTO(
        Long id,
        String nome,
        String email,
        String telefone,
        Boolean ativo,
        BigDecimal percentualComissao
) {
    public BarbeiroResponseDTO(Long id, String nome, String email, String telefone, Boolean ativo) {
        this(id, nome, email, telefone, ativo, BarbeiroModel.PERCENTUAL_COMISSAO_PADRAO);
    }

    public BarbeiroResponseDTO(Long id, String nome, String email, String telefone) {
        this(id, nome, email, telefone, true, BarbeiroModel.PERCENTUAL_COMISSAO_PADRAO);
    }
}
