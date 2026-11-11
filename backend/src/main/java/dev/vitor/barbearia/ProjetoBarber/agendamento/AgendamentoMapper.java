package dev.vitor.barbearia.ProjetoBarber.agendamento;

import dev.vitor.barbearia.ProjetoBarber.historico.AtendimentoHistoricoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AgendamentoMapper {

    public static final int META_SELOS_FIDELIDADE = 10;

    private final AgendamentoRepository agendamentoRepository;
    private final AtendimentoHistoricoRepository historicoRepository;

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

        long totalConcluidos = 0L;
        long faltasCount = 0L;
        boolean elegivelFidelidade = false;

        if (!isNaoUsuario && model.getCliente() != null && model.getCliente().getId() != null) {
            Long clienteId = model.getCliente().getId();
            totalConcluidos = historicoRepository.countByClienteId(clienteId);
            faltasCount = agendamentoRepository.countByClienteIdAndStatus(clienteId, StatusAgendamento.NAO_COMPARECEU);
            elegivelFidelidade = totalConcluidos > 0 && (totalConcluidos % META_SELOS_FIDELIDADE == (META_SELOS_FIDELIDADE - 1) || totalConcluidos % META_SELOS_FIDELIDADE == 0);
        }

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
            totalConcluidos,
            faltasCount,
            elegivelFidelidade
        );
    }
}
