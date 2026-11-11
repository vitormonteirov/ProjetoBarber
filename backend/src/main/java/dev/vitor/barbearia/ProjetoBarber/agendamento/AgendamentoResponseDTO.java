package dev.vitor.barbearia.ProjetoBarber.agendamento;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AgendamentoResponseDTO(
    Long id,
    Long clienteId,
    String clienteNome,
    String clienteTelefone,
    Long barbeiroId,
    String barbeiroNome,
    Long servicoId,
    String servicoNome,
    Integer duracaoTotalMinutos,
    LocalDateTime dataHora,
    LocalDateTime dataHoraFim,
    BigDecimal preco,
    String status,
    String notaAlteracao,
    Boolean isNaoUsuario,
    String nomeNaoUsuario,
    String telefoneNaoUsuario,
    Long clienteTotalConcluidos,
    Long clienteFaltasCount,
    Boolean clienteElegivelDescontoFidelidade
) {
    public AgendamentoResponseDTO(
            Long id,
            Long clienteId,
            String clienteNome,
            Long barbeiroId,
            String barbeiroNome,
            Long servicoId,
            String servicoNome,
            LocalDateTime dataHora,
            LocalDateTime dataHoraFim,
            BigDecimal preco,
            String status,
            String notaAlteracao,
            Boolean isNaoUsuario,
            String nomeNaoUsuario,
            String telefoneNaoUsuario) {
        this(
                id,
                clienteId,
                clienteNome,
                telefoneNaoUsuario,
                barbeiroId,
                barbeiroNome,
                servicoId,
                servicoNome,
                null,
                dataHora,
                dataHoraFim,
                preco,
                status,
                notaAlteracao,
                isNaoUsuario,
                nomeNaoUsuario,
                telefoneNaoUsuario,
                0L,
                0L,
                false
        );
    }

    public AgendamentoResponseDTO(Long id, Long clienteId, String clienteNome, Long barbeiroId, String barbeiroNome, LocalDateTime dataHora, String status) {
        this(id, clienteId, clienteNome, barbeiroId, barbeiroNome, null, null, dataHora, null, null, status, null, false, null, null);
    }

    public AgendamentoResponseDTO(Long id, Long clienteId, String clienteNome, Long barbeiroId, String barbeiroNome, Long servicoId, String servicoNome, LocalDateTime dataHora, LocalDateTime dataHoraFim, BigDecimal preco, String status) {
        this(id, clienteId, clienteNome, barbeiroId, barbeiroNome, servicoId, servicoNome, dataHora, dataHoraFim, preco, status, null, false, null, null);
    }

    public AgendamentoResponseDTO(Long id, Long clienteId, String clienteNome, Long barbeiroId, String barbeiroNome, Long servicoId, String servicoNome, LocalDateTime dataHora, LocalDateTime dataHoraFim, BigDecimal preco, String status, String notaAlteracao) {
        this(id, clienteId, clienteNome, barbeiroId, barbeiroNome, servicoId, servicoNome, dataHora, dataHoraFim, preco, status, notaAlteracao, false, null, null);
    }
}
