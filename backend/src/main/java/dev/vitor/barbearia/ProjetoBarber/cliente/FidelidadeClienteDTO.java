package dev.vitor.barbearia.ProjetoBarber.cliente;

public record FidelidadeClienteDTO(
        Long clienteId,
        String clienteNome,
        Long totalAtendimentosConcluidos,
        Integer metaSelos,
        Integer selosNoCicloAtual,
        Integer faltamParaRecompensa,
        Long recompensasAcumuladas,
        Boolean proximaVisitaComRecompensa,
        Long totalFaltasNoShow
) {}
