package dev.vitor.barbearia.ProjetoBarber.cliente;

import org.springframework.stereotype.Component;

@Component
public class ClienteMapper {
    public ClienteResponseDTO toResponseDTO(ClienteModel model) {
        if (model == null) return null;
        return new ClienteResponseDTO(model.getId(), model.getNome(), model.getTelefone(), model.getEmail());
    }

    public ClienteModel toModel(ClienteDTO dto) {
        if (dto == null) return null;
        ClienteModel model = new ClienteModel();
        model.setNome(dto.nome());
        model.setTelefone(dto.telefone());
        model.setEmail(dto.email());
        return model;
    }
}
