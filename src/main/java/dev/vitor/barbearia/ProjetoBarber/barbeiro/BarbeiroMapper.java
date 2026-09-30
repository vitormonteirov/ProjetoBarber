package dev.vitor.barbearia.ProjetoBarber.barbeiro;

import org.springframework.stereotype.Component;

@Component
public class BarbeiroMapper {
    public BarbeiroResponseDTO toResponseDTO(BarbeiroModel model) {
        if (model == null) return null;
        return new BarbeiroResponseDTO(model.getId(), model.getNome(), model.getEmail(), model.getTelefone());
    }

    public BarbeiroModel toModel(BarbeiroDTO dto) {
        if (dto == null) return null;
        BarbeiroModel model = new BarbeiroModel();
        model.setNome(dto.nome());
        model.setEmail(dto.email());
        model.setTelefone(dto.telefone());
        return model;
    }
}
