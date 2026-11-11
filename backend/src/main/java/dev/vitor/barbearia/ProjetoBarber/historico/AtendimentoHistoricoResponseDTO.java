package dev.vitor.barbearia.ProjetoBarber.historico;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AtendimentoHistoricoResponseDTO(
        Long id,
        Long agendamentoId,
        Long barbeiroId,
        String barbeiroNome,
        Long servicoId,
        String servicoNome,
        Long clienteId,
        String clienteNome,
        Boolean clienteNaoUsuario,
        LocalDateTime dataHoraAtendimento,
        LocalDateTime dataHoraConclusao,
        BigDecimal valorTabela,
        BigDecimal valorPago,
        String formaPagamento,
        BigDecimal percentualComissao,
        BigDecimal valorComissao,
        String concluidoPor
) {
    public static AtendimentoHistoricoResponseDTO from(AtendimentoHistoricoModel h) {
        return new AtendimentoHistoricoResponseDTO(
                h.getId(), h.getAgendamentoId(),
                h.getBarbeiroId(), h.getBarbeiroNome(),
                h.getServicoId(), h.getServicoNome(),
                h.getClienteId(), h.getClienteNome(), h.getClienteNaoUsuario(),
                h.getDataHoraAtendimento(), h.getDataHoraConclusao(),
                h.getValorTabela(), h.getValorPago(),
                h.getFormaPagamento() != null ? h.getFormaPagamento().name() : null,
                h.getPercentualComissao(), h.getValorComissao(),
                h.getConcluidoPor()
        );
    }
}
