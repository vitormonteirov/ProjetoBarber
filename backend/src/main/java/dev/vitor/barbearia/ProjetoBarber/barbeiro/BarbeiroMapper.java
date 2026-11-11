package dev.vitor.barbearia.ProjetoBarber.barbeiro;

import org.springframework.stereotype.Component;

@Component
public class BarbeiroMapper {
    public BarbeiroResponseDTO toResponseDTO(BarbeiroModel model) {
        if (model == null) return null;
        return new BarbeiroResponseDTO(
            model.getId(),
            model.getNome(),
            model.getEmail(),
            model.getTelefone(),
            model.getAtivo(),
            model.getPercentualComissao(),
            model.getHorarioInicioExpediente() != null ? model.getHorarioInicioExpediente() : BarbeiroModel.EXPEDIENTE_INICIO_PADRAO,
            model.getHorarioFimExpediente() != null ? model.getHorarioFimExpediente() : BarbeiroModel.EXPEDIENTE_FIM_PADRAO,
            model.getHorarioInicioPausa(),
            model.getHorarioFimPausa()
        );
    }

    public BarbeiroModel toModel(BarbeiroDTO dto) {
        if (dto == null) return null;
        BarbeiroModel model = new BarbeiroModel();
        model.setNome(dto.nome());
        model.setEmail(dto.email());
        model.setTelefone(dto.telefone());
        model.setAtivo(true);
        model.setHorarioInicioExpediente(dto.horarioInicioExpediente() != null ? dto.horarioInicioExpediente() : BarbeiroModel.EXPEDIENTE_INICIO_PADRAO);
        model.setHorarioFimExpediente(dto.horarioFimExpediente() != null ? dto.horarioFimExpediente() : BarbeiroModel.EXPEDIENTE_FIM_PADRAO);
        model.setHorarioInicioPausa(dto.horarioInicioPausa());
        model.setHorarioFimPausa(dto.horarioFimPausa());
        return model;
    }
}
