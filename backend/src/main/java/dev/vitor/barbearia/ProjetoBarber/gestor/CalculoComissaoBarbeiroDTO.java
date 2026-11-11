package dev.vitor.barbearia.ProjetoBarber.gestor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Resultado do cálculo/simulação de comissão de um barbeiro em um período.
 *
 * <p>Retorna tanto a comissão gravada historicamente em cada atendimento quanto a comissão
 * recalculada pelo percentual informado pelo Gestor (ou pelo percentual atual do barbeiro,
 * quando nenhum percentual de simulação é enviado).</p>
 */
public record CalculoComissaoBarbeiroDTO(
        Long barbeiroId,
        String barbeiroNome,
        Boolean barbeiroAtivo,
        LocalDate dataInicio,
        LocalDate dataFim,
        Long totalAtendimentos,
        BigDecimal totalFaturado,
        BigDecimal percentualCadastradoAtual,
        BigDecimal comissaoRegistradaNoHistorico,
        BigDecimal percentualAplicado,
        BigDecimal comissaoCalculada,
        BigDecimal liquidoBarbearia,
        List<ItemComissaoServicoDTO> porServico
) {
    public record ItemComissaoServicoDTO(
            Long servicoId,
            String servicoNome,
            Long quantidade,
            BigDecimal totalPago,
            BigDecimal comissaoRegistrada,
            BigDecimal comissaoCalculada
    ) {}
}
