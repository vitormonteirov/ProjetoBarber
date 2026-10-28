package dev.vitor.barbearia.ProjetoBarber.agendamento;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AgendamentoResponseDTO(
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
    String telefoneNaoUsuario
) {
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
