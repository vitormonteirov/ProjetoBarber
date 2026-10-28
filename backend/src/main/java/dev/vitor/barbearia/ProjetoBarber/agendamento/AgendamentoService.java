package dev.vitor.barbearia.ProjetoBarber.agendamento;

import dev.vitor.barbearia.ProjetoBarber.barbeiro.AlterarHorarioDTO;
import dev.vitor.barbearia.ProjetoBarber.barbeiro.BarbeiroModel;
import dev.vitor.barbearia.ProjetoBarber.barbeiro.BarbeiroService;
import dev.vitor.barbearia.ProjetoBarber.barbeiro.CancelarAgendamentoBarbeiroDTO;
import dev.vitor.barbearia.ProjetoBarber.cliente.ClienteModel;
import dev.vitor.barbearia.ProjetoBarber.cliente.ClienteService;
import dev.vitor.barbearia.ProjetoBarber.exceptions.BusinessRuleException;
import dev.vitor.barbearia.ProjetoBarber.exceptions.ResourceNotFoundException;
import dev.vitor.barbearia.ProjetoBarber.servico.ServicoModel;
import dev.vitor.barbearia.ProjetoBarber.servico.ServicoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AgendamentoService {

    private final AgendamentoRepository agendamentoRepository;
    private final ClienteService clienteService;
    private final BarbeiroService barbeiroService;
    private final ServicoService servicoService;

    @Transactional(readOnly = true)
    public Page<AgendamentoModel> listarTodos(Pageable pageable) {
        return agendamentoRepository.findAll(pageable);
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

        // Validação e carregamento do Barbeiro
        BarbeiroModel barbeiro = barbeiroService.buscarPorId(dto.barbeiroId())
                .orElseThrow(() -> new ResourceNotFoundException("Barbeiro não encontrado com ID: " + dto.barbeiroId()));
        if (Boolean.FALSE.equals(barbeiro.getAtivo())) {
            throw new BusinessRuleException("O barbeiro selecionado está inativo.");
        }

        // Resolução de Serviço, Preço e Duração
        ServicoModel servico = null;
        int duracaoMinutos = 30;
        BigDecimal preco = null;

        if (dto.servicoId() != null) {
            servico = servicoService.buscarPorId(dto.servicoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Serviço não encontrado com ID: " + dto.servicoId()));
            if (Boolean.FALSE.equals(servico.getAtivo())) {
                throw new BusinessRuleException("O serviço selecionado está inativo.");
            }
            duracaoMinutos = servico.getDuracaoMinutos();
            preco = servico.getPreco();
        }

        LocalDateTime inicio = dto.dataHora();
        LocalDateTime fim = inicio.plusMinutes(duracaoMinutos);

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
                .servico(servico)
                .dataHora(inicio)
                .dataHoraFim(fim)
                .preco(preco)
                .status(StatusAgendamento.AGENDADO)
                .nomeNaoUsuario(dto.nomeNaoUsuario())
                .telefoneNaoUsuario(dto.telefoneNaoUsuario())
                .build();

        return agendamentoRepository.save(agendamento);
    }

    @Transactional
    public AgendamentoModel cancelar(Long id) {
        AgendamentoModel agendamento = agendamentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado com ID: " + id));

        if (agendamento.getStatus() == StatusAgendamento.CANCELADO) {
            throw new BusinessRuleException("Este agendamento já se encontra cancelado.");
        }

        LocalDateTime limiteCancelamento = agendamento.getDataHora().minusHours(1);
        if (LocalDateTime.now().isAfter(limiteCancelamento)) {
            throw new BusinessRuleException("Agendamentos só podem ser cancelados com pelo menos 1 hora de antecedência.");
        }

        agendamento.setStatus(StatusAgendamento.CANCELADO);
        return agendamentoRepository.save(agendamento);
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

        int duracao = agendamento.getServico() != null ? agendamento.getServico().getDuracaoMinutos() : 30;
        LocalDateTime novoInicio = dto.novaDataHora();
        LocalDateTime novoFim = novoInicio.plusMinutes(duracao);

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

        String papel = isGestor ? "Gestor" : "Barbeiro";
        agendamento.setStatus(StatusAgendamento.CANCELADO);
        agendamento.setNotaAlteracao("Cancelado pelo " + papel + ". Motivo: " + dto.motivo());

        return agendamentoRepository.save(agendamento);
    }

    @Transactional(readOnly = true)
    public List<LocalTime> calcularHorariosDisponiveis(Long barbeiroId, Long servicoId, LocalDate data) {
        int duracaoMinutos = 30;
        if (servicoId != null) {
            ServicoModel servico = servicoService.buscarPorId(servicoId)
                    .orElseThrow(() -> new ResourceNotFoundException("Serviço não encontrado: " + servicoId));
            duracaoMinutos = servico.getDuracaoMinutos();
        }

        // Horário de funcionamento padrão: 08:00 às 19:00
        LocalTime inicioExpediente = LocalTime.of(8, 0);
        LocalTime fimExpediente = LocalTime.of(19, 0);

        LocalDateTime inicioDia = data.atStartOfDay();
        LocalDateTime fimDia = data.atTime(23, 59, 59);

        List<AgendamentoModel> agendamentos = agendamentoRepository.findByBarbeiroIdAndDataHoraBetweenAndStatus(
                barbeiroId, inicioDia, fimDia, StatusAgendamento.AGENDADO);

        List<LocalTime> horariosLivres = new ArrayList<>();
        LocalTime slot = inicioExpediente;

        while (!slot.plusMinutes(duracaoMinutos).isAfter(fimExpediente)) {
            LocalDateTime slotInicio = data.atTime(slot);
            LocalDateTime slotFim = slotInicio.plusMinutes(duracaoMinutos);

            // Verifica se o slot é no passado caso a data seja hoje
            boolean noPassado = slotInicio.isBefore(LocalDateTime.now());

            boolean ocupado = agendamentos.stream().anyMatch(a -> {
                LocalDateTime aInicio = a.getDataHora();
                LocalDateTime aFim = a.getDataHoraFim() != null ? a.getDataHoraFim() : a.getDataHora().plusMinutes(30);
                return slotInicio.isBefore(aFim) && slotFim.isAfter(aInicio);
            });

            if (!noPassado && !ocupado) {
                horariosLivres.add(slot);
            }

            slot = slot.plusMinutes(30);
        }

        return horariosLivres;
    }
}
