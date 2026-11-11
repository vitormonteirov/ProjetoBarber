package dev.vitor.barbearia.ProjetoBarber.agendamento;

import dev.vitor.barbearia.ProjetoBarber.agendamento.event.AgendamentoCanceladoEvent;
import dev.vitor.barbearia.ProjetoBarber.agendamento.event.AgendamentoCriadoEvent;
import dev.vitor.barbearia.ProjetoBarber.barbeiro.AlterarHorarioDTO;
import dev.vitor.barbearia.ProjetoBarber.barbeiro.BarbeiroModel;
import dev.vitor.barbearia.ProjetoBarber.barbeiro.BarbeiroService;
import dev.vitor.barbearia.ProjetoBarber.barbeiro.BloqueioAgendaModel;
import dev.vitor.barbearia.ProjetoBarber.barbeiro.BloqueioAgendaRepository;
import dev.vitor.barbearia.ProjetoBarber.barbeiro.CancelarAgendamentoBarbeiroDTO;
import dev.vitor.barbearia.ProjetoBarber.cliente.ClienteModel;
import dev.vitor.barbearia.ProjetoBarber.cliente.ClienteService;
import dev.vitor.barbearia.ProjetoBarber.exceptions.BusinessRuleException;
import dev.vitor.barbearia.ProjetoBarber.exceptions.ResourceNotFoundException;
import dev.vitor.barbearia.ProjetoBarber.historico.AtendimentoHistoricoModel;
import dev.vitor.barbearia.ProjetoBarber.historico.AtendimentoHistoricoRepository;
import dev.vitor.barbearia.ProjetoBarber.servico.ServicoModel;
import dev.vitor.barbearia.ProjetoBarber.servico.ServicoService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AgendamentoService {

    private static final BigDecimal CEM = new BigDecimal("100");

    private final AgendamentoRepository agendamentoRepository;
    private final ClienteService clienteService;
    private final BarbeiroService barbeiroService;
    private final ServicoService servicoService;
    private final ApplicationEventPublisher eventPublisher;
    private final AtendimentoHistoricoRepository historicoRepository;
    private final BloqueioAgendaRepository bloqueioAgendaRepository;

    @Transactional(readOnly = true)
    public Page<AgendamentoModel> listarTodos(Pageable pageable) {
        return agendamentoRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<AgendamentoModel> listarPorStatus(StatusAgendamento status, Pageable pageable) {
        return agendamentoRepository.findByStatusOrderByDataHoraAsc(status, pageable);
    }

    @Transactional(readOnly = true)
    public Page<AgendamentoModel> listarPorBarbeiroEmail(String email, Pageable pageable) {
        return agendamentoRepository.findByBarbeiroEmailOrderByDataHoraAsc(email, pageable);
    }

    @Transactional(readOnly = true)
    public Page<AgendamentoModel> listarPorClienteEmail(String email, Pageable pageable) {
        return agendamentoRepository.findByClienteEmailOrderByDataHoraDesc(email, pageable);
    }

    @Transactional
    public AgendamentoModel agendar(AgendamentoDTO dto) {
        ClienteModel cliente = null;

        // Se informou clienteId, carrega e valida o cliente cadastrado
        if (dto.clienteId() != null) {
            cliente = clienteService.buscarPorId(dto.clienteId())
                    .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado com ID: " + dto.clienteId()));
            if (Boolean.FALSE.equals(cliente.getAtivo())) {
                throw new BusinessRuleException("Não é possível agendar para um cliente inativo.");
            }
        } else {
            // Se for um não usuário (reserva avulsa feita pelo Gestor), valida dados mínimos
            if (dto.nomeNaoUsuario() == null || dto.nomeNaoUsuario().isBlank()) {
                throw new BusinessRuleException("Para reservas de clientes não cadastrados, o nome do cliente deve ser informado.");
            }
        }

        // Validação e bloqueio pessimista do Barbeiro para evitar concorrência e double booking
        BarbeiroModel barbeiro = barbeiroService.buscarPorIdComLock(dto.barbeiroId())
                .orElseThrow(() -> new ResourceNotFoundException("Barbeiro não encontrado com ID: " + dto.barbeiroId()));
        if (Boolean.FALSE.equals(barbeiro.getAtivo())) {
            throw new BusinessRuleException("O barbeiro selecionado está inativo.");
        }

        // Resolução de Serviço(s) — suporta 1 ou múltiplos serviços combinados no mesmo agendamento
        Set<Long> idsServicos = new LinkedHashSet<>();
        if (dto.servicoIds() != null) {
            dto.servicoIds().stream().filter(id -> id != null).forEach(idsServicos::add);
        }
        if (dto.servicoId() != null) {
            idsServicos.add(dto.servicoId());
        }

        ServicoModel servicoPrincipal = null;
        int duracaoMinutos = 30;
        BigDecimal preco = null;
        String descricaoServicos = null;

        if (!idsServicos.isEmpty()) {
            List<ServicoModel> servicosSelecionados = new ArrayList<>();
            for (Long sId : idsServicos) {
                ServicoModel s = servicoService.buscarPorId(sId)
                        .orElseThrow(() -> new ResourceNotFoundException("Serviço não encontrado com ID: " + sId));
                if (Boolean.FALSE.equals(s.getAtivo())) {
                    throw new BusinessRuleException("O serviço selecionado está inativo: " + s.getNome());
                }
                servicosSelecionados.add(s);
            }
            servicoPrincipal = servicosSelecionados.get(0);
            duracaoMinutos = servicosSelecionados.stream()
                    .mapToInt(s -> s.getDuracaoMinutos() != null ? s.getDuracaoMinutos() : 30)
                    .sum();
            preco = servicosSelecionados.stream()
                    .map(s -> s.getPreco() != null ? s.getPreco() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            descricaoServicos = servicosSelecionados.stream()
                    .map(ServicoModel::getNome)
                    .collect(Collectors.joining(" + "));
        }

        LocalDateTime inicio = dto.dataHora();
        LocalDateTime fim = inicio.plusMinutes(duracaoMinutos);

        // Regra 0: Valida se o horário solicitado respeita o expediente e o intervalo de pausa do barbeiro
        validarDentroDoExpedienteEPausa(barbeiro, inicio.toLocalTime(), fim.toLocalTime());

        // Regra 0.1: Verifica se há bloqueio pontual de agenda / folga para o barbeiro no intervalo
        if (bloqueioAgendaRepository != null
                && bloqueioAgendaRepository.existsConflitoBloqueio(dto.barbeiroId(), inicio, fim)) {
            throw new BusinessRuleException("O barbeiro possui um bloqueio de agenda/folga registrado neste horário.");
        }

        // Regra 1: Limite diário de agendamento por dia do cliente com o mesmo barbeiro (apenas para cliente cadastrado)
        if (cliente != null) {
            LocalDateTime inicioDia = dto.dataHora().toLocalDate().atStartOfDay();
            LocalDateTime fimDia = dto.dataHora().toLocalDate().atTime(23, 59, 59);

            boolean clienteJaAgendadoHoje = agendamentoRepository.existsByClienteIdAndBarbeiroIdAndDiaLimit(
                    cliente.getId(), dto.barbeiroId(), inicioDia, fimDia, StatusAgendamento.AGENDADO);

            if (clienteJaAgendadoHoje) {
                throw new BusinessRuleException("Cliente já possui um agendamento marcado para este dia com este barbeiro.");
            }
        }

        // Regra 2: Barbeiro não pode ter agendamentos conflitantes no intervalo (Double Booking prevention)
        boolean conflitoHorario = agendamentoRepository.existsConflitoHorarioBarbeiro(
                dto.barbeiroId(), inicio, fim, StatusAgendamento.AGENDADO);

        if (conflitoHorario) {
            throw new BusinessRuleException("O barbeiro selecionado não está disponível no horário solicitado (conflito de agenda).");
        }

        AgendamentoModel agendamento = AgendamentoModel.builder()
                .cliente(cliente)
                .barbeiro(barbeiro)
                .servico(servicoPrincipal)
                .descricaoServicos(descricaoServicos)
                .duracaoTotalMinutos(duracaoMinutos)
                .dataHora(inicio)
                .dataHoraFim(fim)
                .preco(preco)
                .status(StatusAgendamento.AGENDADO)
                .nomeNaoUsuario(dto.nomeNaoUsuario())
                .telefoneNaoUsuario(dto.telefoneNaoUsuario())
                .build();

        AgendamentoModel salvo = agendamentoRepository.save(agendamento);
        eventPublisher.publishEvent(new AgendamentoCriadoEvent(salvo));
        return salvo;
    }

    @Transactional
    public AgendamentoModel cancelar(Long id) {
        return cancelar(id, null, true);
    }

    @Transactional
    public AgendamentoModel cancelar(Long id, String emailSolicitante, boolean isGestor) {
        AgendamentoModel agendamento = agendamentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado com ID: " + id));

        if (!isGestor) {
            ClienteModel cliente = agendamento.getCliente();
            if (cliente == null || cliente.getEmail() == null || !cliente.getEmail().equalsIgnoreCase(emailSolicitante)) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "Acesso negado: você só pode cancelar os seus próprios agendamentos.");
            }
        }

        if (agendamento.getStatus() == StatusAgendamento.CANCELADO) {
            throw new BusinessRuleException("Este agendamento já se encontra cancelado.");
        }
        if (agendamento.getStatus() == StatusAgendamento.CONCLUIDO) {
            throw new BusinessRuleException("Atendimentos já concluídos não podem ser cancelados.");
        }
        if (agendamento.getStatus() == StatusAgendamento.NAO_COMPARECEU) {
            throw new BusinessRuleException("Agendamentos já registrados como não comparecimento (No-Show) não podem ser cancelados.");
        }

        LocalDateTime limiteCancelamento = agendamento.getDataHora().minusHours(1);
        if (LocalDateTime.now().isAfter(limiteCancelamento)) {
            throw new BusinessRuleException("Agendamentos só podem ser cancelados com pelo menos 1 hora de antecedência.");
        }

        agendamento.setStatus(StatusAgendamento.CANCELADO);
        AgendamentoModel salvo = agendamentoRepository.save(agendamento);
        eventPublisher.publishEvent(new AgendamentoCanceladoEvent(salvo));
        return salvo;
    }

    @Transactional
    public AgendamentoModel alterarHorarioPeloBarbeiroOuGestor(Long agendamentoId, String emailSolicitante, boolean isGestor, AlterarHorarioDTO dto) {
        AgendamentoModel agendamento = agendamentoRepository.findById(agendamentoId)
                .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado com ID: " + agendamentoId));

        if (!isGestor && !agendamento.getBarbeiro().getEmail().equalsIgnoreCase(emailSolicitante)) {
            throw new BusinessRuleException("Você só pode alterar agendamentos da sua própria agenda.");
        }

        if (agendamento.getStatus() != StatusAgendamento.AGENDADO) {
            throw new BusinessRuleException("Apenas agendamentos com status AGENDADO podem ter o horário alterado.");
        }

        // Lock pessimista na agenda do barbeiro para evitar sobreposições concorrentes
        BarbeiroModel barbeiro = barbeiroService.buscarPorIdComLock(agendamento.getBarbeiro().getId())
                .orElse(agendamento.getBarbeiro());

        int duracao = agendamento.getDuracaoTotalMinutos() != null
                ? agendamento.getDuracaoTotalMinutos()
                : (agendamento.getServico() != null ? agendamento.getServico().getDuracaoMinutos() : 30);
        LocalDateTime novoInicio = dto.novaDataHora();
        LocalDateTime novoFim = novoInicio.plusMinutes(duracao);

        validarDentroDoExpedienteEPausa(barbeiro, novoInicio.toLocalTime(), novoFim.toLocalTime());

        if (bloqueioAgendaRepository != null
                && bloqueioAgendaRepository.existsConflitoBloqueio(barbeiro.getId(), novoInicio, novoFim)) {
            throw new BusinessRuleException("O barbeiro possui um bloqueio de agenda/folga registrado no novo horário selecionado.");
        }

        boolean conflito = agendamentoRepository.existsConflitoHorarioBarbeiroIgnorandoId(
                agendamento.getBarbeiro().getId(), novoInicio, novoFim, StatusAgendamento.AGENDADO, agendamento.getId());

        if (conflito) {
            throw new BusinessRuleException("Conflito de horário na nova data selecionada para o barbeiro.");
        }

        String papel = isGestor ? "Gestor" : "Barbeiro";
        agendamento.setDataHora(novoInicio);
        agendamento.setDataHoraFim(novoFim);
        agendamento.setNotaAlteracao("Alterado pelo " + papel + " (" + emailSolicitante + "): " + dto.notaAlteracao());

        return agendamentoRepository.save(agendamento);
    }

    @Transactional
    public AgendamentoModel cancelarPeloBarbeiroOuGestor(Long agendamentoId, String emailSolicitante, boolean isGestor, CancelarAgendamentoBarbeiroDTO dto) {
        AgendamentoModel agendamento = agendamentoRepository.findById(agendamentoId)
                .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado com ID: " + agendamentoId));

        if (!isGestor && !agendamento.getBarbeiro().getEmail().equalsIgnoreCase(emailSolicitante)) {
            throw new BusinessRuleException("Você só pode cancelar agendamentos da sua própria agenda.");
        }

        if (agendamento.getStatus() == StatusAgendamento.CANCELADO) {
            throw new BusinessRuleException("Este agendamento já se encontra cancelado.");
        }
        if (agendamento.getStatus() == StatusAgendamento.CONCLUIDO) {
            throw new BusinessRuleException("Atendimentos já concluídos não podem ser cancelados.");
        }
        if (agendamento.getStatus() == StatusAgendamento.NAO_COMPARECEU) {
            throw new BusinessRuleException("Agendamentos já registrados como não comparecimento (No-Show) não podem ser cancelados.");
        }

        String papel = isGestor ? "Gestor" : "Barbeiro";
        agendamento.setStatus(StatusAgendamento.CANCELADO);
        agendamento.setNotaAlteracao("Cancelado pelo " + papel + ". Motivo: " + dto.motivo());

        AgendamentoModel salvo = agendamentoRepository.save(agendamento);
        eventPublisher.publishEvent(new AgendamentoCanceladoEvent(salvo));
        return salvo;
    }

    /**
     * Registra ausência do cliente (No-Show): altera o status do agendamento para {@code NAO_COMPARECEU}
     * sem gerar faturamento nem comissão no histórico financeiro.
     */
    @Transactional
    public AgendamentoModel marcarNaoCompareceu(Long agendamentoId, String emailSolicitante, boolean isGestor, String observacao) {
        AgendamentoModel agendamento = agendamentoRepository.findById(agendamentoId)
                .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado com ID: " + agendamentoId));

        if (!isGestor && !agendamento.getBarbeiro().getEmail().equalsIgnoreCase(emailSolicitante)) {
            throw new BusinessRuleException("Você só pode registrar ausência em agendamentos da sua própria agenda.");
        }

        if (agendamento.getStatus() != StatusAgendamento.AGENDADO) {
            throw new BusinessRuleException("Apenas agendamentos com status AGENDADO podem ser marcados como Não Compareceu (No-Show).");
        }

        String papel = isGestor ? "Gestor" : "Barbeiro";
        String detalhe = (observacao != null && !observacao.isBlank()) ? observacao.trim() : "Cliente não compareceu no horário agendado";
        agendamento.setStatus(StatusAgendamento.NAO_COMPARECEU);
        agendamento.setNotaAlteracao("No-Show registrado pelo " + papel + " (" + emailSolicitante + "): " + detalhe);

        return agendamentoRepository.save(agendamento);
    }

    /**
     * Conclui um atendimento: marca o agendamento como CONCLUIDO e grava um snapshot imutável
     * em {@code atendimento_historico} com valor pago, forma de pagamento e comissão vigente.
     */
    @Transactional
    public AtendimentoHistoricoModel concluirAtendimento(Long agendamentoId, String emailSolicitante, boolean isGestor, ConcluirAtendimentoDTO dto) {
        AgendamentoModel agendamento = agendamentoRepository.findById(agendamentoId)
                .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado com ID: " + agendamentoId));

        BarbeiroModel barbeiro = agendamento.getBarbeiro();
        if (!isGestor && !barbeiro.getEmail().equalsIgnoreCase(emailSolicitante)) {
            throw new BusinessRuleException("Você só pode concluir atendimentos da sua própria agenda.");
        }
        if (agendamento.getStatus() != StatusAgendamento.AGENDADO) {
            throw new BusinessRuleException("Apenas agendamentos com status AGENDADO podem ser concluídos.");
        }
        if (agendamento.getDataHora().isAfter(LocalDateTime.now())) {
            throw new BusinessRuleException("Não é possível concluir um atendimento que ainda não começou.");
        }
        if (historicoRepository.existsByAgendamentoId(agendamentoId)) {
            throw new BusinessRuleException("Este atendimento já foi registrado no histórico.");
        }

        ServicoModel servico = agendamento.getServico();
        BigDecimal valorTabela = agendamento.getPreco() != null
                ? agendamento.getPreco()
                : (servico != null ? servico.getPreco() : null);

        BigDecimal valorPago = dto.valorPago() != null ? dto.valorPago() : valorTabela;
        if (valorPago == null) {
            throw new BusinessRuleException("Informe o valor pago: este atendimento não possui preço de serviço associado.");
        }
        valorPago = valorPago.setScale(2, RoundingMode.HALF_UP);

        BigDecimal percentual = barbeiro.getPercentualComissao() != null
                ? barbeiro.getPercentualComissao()
                : BarbeiroModel.PERCENTUAL_COMISSAO_PADRAO;
        BigDecimal valorComissao = valorPago.multiply(percentual).divide(CEM, 2, RoundingMode.HALF_UP);

        ClienteModel cliente = agendamento.getCliente();
        String nomeServicoSnapshot = (agendamento.getDescricaoServicos() != null && !agendamento.getDescricaoServicos().isBlank())
                ? agendamento.getDescricaoServicos()
                : (servico != null ? servico.getNome() : "Atendimento avulso");

        AtendimentoHistoricoModel historico = AtendimentoHistoricoModel.builder()
                .agendamentoId(agendamento.getId())
                .barbeiroId(barbeiro.getId())
                .barbeiroNome(barbeiro.getNome())
                .servicoId(servico != null ? servico.getId() : null)
                .servicoNome(nomeServicoSnapshot)
                .clienteId(cliente != null ? cliente.getId() : null)
                .clienteNome(cliente != null ? cliente.getNome() : agendamento.getNomeNaoUsuario())
                .clienteNaoUsuario(cliente == null)
                .dataHoraAtendimento(agendamento.getDataHora())
                .dataHoraConclusao(LocalDateTime.now())
                .valorTabela(valorTabela)
                .valorPago(valorPago)
                .formaPagamento(dto.formaPagamento())
                .percentualComissao(percentual)
                .valorComissao(valorComissao)
                .concluidoPor(emailSolicitante)
                .build();

        agendamento.setStatus(StatusAgendamento.CONCLUIDO);
        agendamentoRepository.save(agendamento);

        return historicoRepository.save(historico);
    }

    @Transactional(readOnly = true)
    public List<LocalTime> calcularHorariosDisponiveis(Long barbeiroId, Long servicoId, LocalDate data) {
        List<Long> servicoIds = servicoId != null ? List.of(servicoId) : List.of();
        return calcularHorariosDisponiveis(barbeiroId, servicoIds, data);
    }

    @Transactional(readOnly = true)
    public List<LocalTime> calcularHorariosDisponiveis(Long barbeiroId, List<Long> servicoIds, LocalDate data) {
        int duracaoMinutos = 30;
        if (servicoIds != null && !servicoIds.isEmpty()) {
            int soma = 0;
            for (Long sId : servicoIds) {
                if (sId == null) continue;
                ServicoModel servico = servicoService.buscarPorId(sId)
                        .orElseThrow(() -> new ResourceNotFoundException("Serviço não encontrado: " + sId));
                soma += servico.getDuracaoMinutos() != null ? servico.getDuracaoMinutos() : 30;
            }
            if (soma > 0) {
                duracaoMinutos = soma;
            }
        }

        BarbeiroModel barbeiro = barbeiroService.buscarPorId(barbeiroId).orElse(null);

        LocalTime inicioExpediente = (barbeiro != null && barbeiro.getHorarioInicioExpediente() != null)
                ? barbeiro.getHorarioInicioExpediente()
                : BarbeiroModel.EXPEDIENTE_INICIO_PADRAO;
        LocalTime fimExpediente = (barbeiro != null && barbeiro.getHorarioFimExpediente() != null)
                ? barbeiro.getHorarioFimExpediente()
                : BarbeiroModel.EXPEDIENTE_FIM_PADRAO;

        LocalTime inicioPausa = barbeiro != null ? barbeiro.getHorarioInicioPausa() : null;
        LocalTime fimPausa = barbeiro != null ? barbeiro.getHorarioFimPausa() : null;

        LocalDateTime inicioDia = data.atStartOfDay();
        LocalDateTime fimDia = data.atTime(23, 59, 59);

        List<AgendamentoModel> agendamentos = agendamentoRepository.findByBarbeiroIdAndDataHoraBetweenAndStatus(
                barbeiroId, inicioDia, fimDia, StatusAgendamento.AGENDADO);

        List<BloqueioAgendaModel> bloqueios = bloqueioAgendaRepository != null
                ? bloqueioAgendaRepository.findByBarbeiroIdNoPeriodo(barbeiroId, inicioDia, fimDia)
                : List.of();

        List<LocalTime> horariosLivres = new ArrayList<>();
        LocalTime slot = inicioExpediente;

        while (!slot.plusMinutes(duracaoMinutos).isAfter(fimExpediente)) {
            LocalTime slotFimTime = slot.plusMinutes(duracaoMinutos);
            LocalDateTime slotInicio = data.atTime(slot);
            LocalDateTime slotFim = slotInicio.plusMinutes(duracaoMinutos);

            // Verifica se o slot é no passado caso a data seja hoje
            boolean noPassado = slotInicio.isBefore(LocalDateTime.now());

            // Verifica se o slot conflita com o intervalo de pausa/almoço do barbeiro
            boolean emHorarioDePausa = (inicioPausa != null && fimPausa != null)
                    && slot.isBefore(fimPausa)
                    && slotFimTime.isAfter(inicioPausa);

            // Verifica se o slot conflita com algum bloqueio pontual de agenda/folga do barbeiro
            boolean emBloqueio = bloqueios.stream().anyMatch(b ->
                    slotInicio.isBefore(b.getDataHoraFim()) && slotFim.isAfter(b.getDataHoraInicio()));

            boolean ocupado = agendamentos.stream().anyMatch(a -> {
                LocalDateTime aInicio = a.getDataHora();
                LocalDateTime aFim = a.getDataHoraFim() != null ? a.getDataHoraFim() : a.getDataHora().plusMinutes(30);
                return slotInicio.isBefore(aFim) && slotFim.isAfter(aInicio);
            });

            if (!noPassado && !emHorarioDePausa && !emBloqueio && !ocupado) {
                horariosLivres.add(slot);
            }

            slot = slot.plusMinutes(30);
        }

        return horariosLivres;
    }

    private void validarDentroDoExpedienteEPausa(BarbeiroModel barbeiro, LocalTime inicioSlot, LocalTime fimSlot) {
        LocalTime inicioExpediente = (barbeiro != null && barbeiro.getHorarioInicioExpediente() != null)
                ? barbeiro.getHorarioInicioExpediente()
                : BarbeiroModel.EXPEDIENTE_INICIO_PADRAO;
        LocalTime fimExpediente = (barbeiro != null && barbeiro.getHorarioFimExpediente() != null)
                ? barbeiro.getHorarioFimExpediente()
                : BarbeiroModel.EXPEDIENTE_FIM_PADRAO;

        // Verifica também se o serviço cruzou a meia-noite (fimSlot < inicioSlot)
        if (fimSlot.isBefore(inicioSlot) || inicioSlot.isBefore(inicioExpediente) || fimSlot.isAfter(fimExpediente)) {
            throw new BusinessRuleException(String.format(
                    "O horário solicitado (%s às %s) está fora do expediente do barbeiro (%s às %s).",
                    inicioSlot, fimSlot, inicioExpediente, fimExpediente));
        }

        if (barbeiro != null && barbeiro.getHorarioInicioPausa() != null && barbeiro.getHorarioFimPausa() != null) {
            LocalTime inicioPausa = barbeiro.getHorarioInicioPausa();
            LocalTime fimPausa = barbeiro.getHorarioFimPausa();
            if (inicioSlot.isBefore(fimPausa) && fimSlot.isAfter(inicioPausa)) {
                throw new BusinessRuleException(String.format(
                        "O horário solicitado (%s às %s) conflita com o intervalo de pausa/almoço do barbeiro (%s às %s).",
                        inicioSlot, fimSlot, inicioPausa, fimPausa));
            }
        }
    }
}
