package dev.vitor.barbearia.ProjetoBarber.servico;

import org.springframework.stereotype.Component;

@Component
public class ServicoMapper {

    public ServicoResponseDTO toResponseDTO(ServicoModel model) {
        if (model == null) return null;
        return new ServicoResponseDTO(
            model.getId(),
            model.getNome(),
            model.getDescricao(),
            model.getPreco(),
            model.getDuracaoMinutos(),
            model.getAtivo()
        );
    }
}
