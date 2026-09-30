package dev.vitor.barbearia.ProjetoBarber.agendamento;

import java.time.LocalDateTime;

public record AgendamentoResponseDTO(Long id, Long clienteId, String clienteNome, Long barbeiroId, String barbeiroNome, LocalDateTime dataHora, String status) {}
