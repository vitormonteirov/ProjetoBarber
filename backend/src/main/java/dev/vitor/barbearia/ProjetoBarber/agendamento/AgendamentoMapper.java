package dev.vitor.barbearia.ProjetoBarber.agendamento;

import org.springframework.stereotype.Component;

@Component
public class AgendamentoMapper {
    public AgendamentoResponseDTO toResponseDTO(AgendamentoModel model) {
        if (model == null) return null;
        boolean isNaoUsuario = model.getCliente() == null;
        String clienteNomeExibicao = isNaoUsuario
                ? (model.getNomeNaoUsuario() != null ? model.getNomeNaoUsuario() + " (Não Cadastrado)" : "Cliente Avulso (Não Cadastrado)")
                : model.getCliente().getNome();

        return new AgendamentoResponseDTO(
            model.getId(),
            model.getCliente() != null ? model.getCliente().getId() : null,
            clienteNomeExibicao,
            model.getBarbeiro() != null ? model.getBarbeiro().getId() : null,
            model.getBarbeiro() != null ? model.getBarbeiro().getNome() : null,
            model.getServico() != null ? model.getServico().getId() : null,
            model.getServico() != null ? model.getServico().getNome() : null,
            model.getDataHora(),
            model.getDataHoraFim(),
            model.getPreco(),
            model.getStatus() != null ? model.getStatus().name() : null,
            model.getNotaAlteracao(),
            isNaoUsuario,
            model.getNomeNaoUsuario(),
            model.getTelefoneNaoUsuario()
        );
    }
}
