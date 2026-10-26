package dev.vitor.barbearia.ProjetoBarber.gestor;

import java.math.BigDecimal;

public record ComissaoBarbeiroDTO(
    Long barbeiroId,
    String barbeiroNome,
    Long totalAtendimentos,
    BigDecimal faturamentoGerado,
    BigDecimal valorComissao,
    BigDecimal percentualComissao
) {}
