package dev.vitor.barbearia.ProjetoBarber.gestor;

import java.math.BigDecimal;
import java.util.List;

public record DashboardResponseDTO(
    Long totalAgendamentos,
    Long agendamentosConcluidos,
    Long agendamentosCancelados,
    Long agendamentosPendentes,
    BigDecimal faturamentoTotal,
    BigDecimal comissaoTotalBarbeiros,
    BigDecimal lucroLiquido,
    List<ComissaoBarbeiroDTO> comissoesPorBarbeiro
) {}
