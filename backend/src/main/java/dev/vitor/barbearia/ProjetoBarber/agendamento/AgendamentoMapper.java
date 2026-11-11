package dev.vitor.barbearia.ProjetoBarber.agendamento;

import org.springframework.stereotype.Component;

@Component
public class AgendamentoMapper {

    public static final int META_SELOS_FIDELIDADE = 10;

    public AgendamentoResponseDTO toResponseDTO(AgendamentoModel model) {
        if (model == null) return null;
        boolean isNaoUsuario = model.getCliente() == null;
        String clienteNomeExibicao = isNaoUsuario
                ? (model.getNomeNaoUsuario() != null ? model.getNomeNaoUsuario() + " (Não Cadastrado)" : "Cliente Avulso (Não Cadastrado)")
                : model.getCliente().getNome();

        String clienteTelefone = isNaoUsuario
                ? model.getTelefoneNaoUsuario()
                : (model.getCliente() != null ? model.getCliente().getTelefone() : null);

        String servicoNomeExibicao = (model.getDescricaoServicos() != null && !model.getDescricaoServicos().isBlank())
                ? model.getDescricaoServicos()
                : (model.getServico() != null ? model.getServico().getNome() : null);

        Integer duracaoTotal = model.getDuracaoTotalMinutos() != null
                ? model.getDuracaoTotalMinutos()
                : (model.getServico() != null ? model.getServico().getDuracaoMinutos() : 30);

        return new AgendamentoResponseDTO(
            model.getId(),
            model.getCliente() != null ? model.getCliente().getId() : null,
            clienteNomeExibicao,
            clienteTelefone,
            model.getBarbeiro() != null ? model.getBarbeiro().getId() : null,
            model.getBarbeiro() != null ? model.getBarbeiro().getNome() : null,
            model.getServico() != null ? model.getServico().getId() : null,
            servicoNomeExibicao,
            duracaoTotal,
            model.getDataHora(),
            model.getDataHoraFim(),
            model.getPreco(),
            model.getStatus() != null ? model.getStatus().name() : null,
            model.getNotaAlteracao(),
            isNaoUsuario,
            model.getNomeNaoUsuario(),
            model.getTelefoneNaoUsuario(),
            0L,
            0L,
            false
        );
    }
}

