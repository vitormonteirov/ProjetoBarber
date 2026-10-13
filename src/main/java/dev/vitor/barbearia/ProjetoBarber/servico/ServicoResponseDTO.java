package dev.vitor.barbearia.ProjetoBarber.servico;

import java.math.BigDecimal;

public record ServicoResponseDTO(
    Long id,
    String nome,
    String descricao,
    BigDecimal preco,
    Integer duracaoMinutos,
    Boolean ativo
) {}
