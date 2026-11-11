package dev.vitor.barbearia.ProjetoBarber.gestor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record RelatorioPeriodoDTO(
        LocalDate dataInicio,
        LocalDate dataFim,
        Long totalAtendimentosRealizados,
        Long agendamentosPendentesNoPeriodo,
        Long agendamentosCanceladosNoPeriodo,
        Long agendamentosNaoCompareceuNoPeriodo,
        BigDecimal caixaTotal,
        BigDecimal ticketMedio,
        BigDecimal comissaoTotal,
        BigDecimal lucroLiquido,
        List<CaixaPorFormaPagamentoDTO> porFormaPagamento,
        List<CaixaDiarioDTO> porDia,
        List<ResumoBarbeiroPeriodoDTO> porBarbeiro,
        List<ResumoServicoPeriodoDTO> porServico
) {
    public RelatorioPeriodoDTO(
            LocalDate dataInicio,
            LocalDate dataFim,
            Long totalAtendimentosRealizados,
            Long agendamentosPendentesNoPeriodo,
            Long agendamentosCanceladosNoPeriodo,
            BigDecimal caixaTotal,
            BigDecimal ticketMedio,
            BigDecimal comissaoTotal,
            BigDecimal lucroLiquido,
            List<CaixaPorFormaPagamentoDTO> porFormaPagamento,
            List<CaixaDiarioDTO> porDia,
            List<ResumoBarbeiroPeriodoDTO> porBarbeiro,
            List<ResumoServicoPeriodoDTO> porServico
    ) {
        this(
                dataInicio,
                dataFim,
                totalAtendimentosRealizados,
                agendamentosPendentesNoPeriodo,
                agendamentosCanceladosNoPeriodo,
                0L,
                caixaTotal,
                ticketMedio,
                comissaoTotal,
                lucroLiquido,
                porFormaPagamento,
                porDia,
                porBarbeiro,
                porServico
        );
    }
    public record CaixaPorFormaPagamentoDTO(
            String formaPagamento,
            Long quantidade,
            BigDecimal totalPago,
            BigDecimal totalComissao
    ) {}

    public record CaixaDiarioDTO(
            LocalDate data,
            Long quantidade,
            BigDecimal totalPago
    ) {}

    public record ResumoBarbeiroPeriodoDTO(
            Long barbeiroId,
            String barbeiroNome,
            Boolean barbeiroAtivo,
            BigDecimal percentualComissaoAtual,
            Long totalAtendimentos,
            BigDecimal totalPago,
            BigDecimal totalComissao,
            List<ResumoServicoPeriodoDTO> servicos
    ) {}

    public record ResumoServicoPeriodoDTO(
            Long servicoId,
            String servicoNome,
            Long quantidade,
            BigDecimal totalPago
    ) {}
}
