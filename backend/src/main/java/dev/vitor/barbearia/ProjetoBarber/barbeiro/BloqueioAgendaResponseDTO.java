package dev.vitor.barbearia.ProjetoBarber.barbeiro;

import java.time.LocalDateTime;

public record BloqueioAgendaResponseDTO(
        Long id,
        Long barbeiroId,
        String barbeiroNome,
        LocalDateTime dataHoraInicio,
        LocalDateTime dataHoraFim,
        String motivo,
        String criadoPor
) {
    public static BloqueioAgendaResponseDTO from(BloqueioAgendaModel model) {
        if (model == null) return null;
        return new BloqueioAgendaResponseDTO(
                model.getId(),
                model.getBarbeiro() != null ? model.getBarbeiro().getId() : null,
                model.getBarbeiro() != null ? model.getBarbeiro().getNome() : null,
                model.getDataHoraInicio(),
                model.getDataHoraFim(),
                model.getMotivo(),
                model.getCriadoPor()
        );
    }
}
