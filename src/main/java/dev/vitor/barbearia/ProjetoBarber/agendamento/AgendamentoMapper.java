package dev.vitor.barbearia.ProjetoBarber.agendamento;

import org.springframework.stereotype.Component;

@Component
public class AgendamentoMapper {
    public AgendamentoResponseDTO toResponseDTO(AgendamentoModel model) {
        if (model == null) return null;
        return new AgendamentoResponseDTO(
            model.getId(),
            model.getCliente() != null ? model.getCliente().getId() : null,
            model.getCliente() != null ? model.getCliente().getNome() : null,
            model.getBarbeiro() != null ? model.getBarbeiro().getId() : null,
            model.getBarbeiro() != null ? model.getBarbeiro().getNome() : null,
            model.getDataHora(),
            model.getStatus() != null ? model.getStatus().name() : null
        );
    }
}
