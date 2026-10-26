package dev.vitor.barbearia.ProjetoBarber.agendamento;

import dev.vitor.barbearia.ProjetoBarber.barbeiro.BarbeiroModel;
import dev.vitor.barbearia.ProjetoBarber.barbeiro.BarbeiroService;
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
import java.time.LocalDateTime;

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

    @Transactional
    public AgendamentoModel agendar(AgendamentoDTO dto) {
        // Validação e carregamento do Cliente
        ClienteModel cliente = clienteService.buscarPorId(dto.clienteId())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado com ID: " + dto.clienteId()));
        if (Boolean.FALSE.equals(cliente.getAtivo())) {
            throw new BusinessRuleException("Não é possível agendar para um cliente inativo.");
        }

        // Validação e carregamento do Barbeiro
        BarbeiroModel barbeiro = barbeiroService.buscarPorId(dto.barbeiroId())
                .orElseThrow(() -> new ResourceNotFoundException("Barbeiro não encontrado com ID: " + dto.barbeiroId()));
        if (Boolean.FALSE.equals(barbeiro.getAtivo())) {
            throw new BusinessRuleException("O barbeiro selecionado está inativo.");
        }

        // Resolução de Serviço, Preço e Duração
        ServicoModel servico = null;
        int duracaoMinutos = 30; // duração padrão quando não especificado
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

        // Regra 1: Limite diário de agendamento por dia do cliente com o mesmo barbeiro
        LocalDateTime inicioDia = dto.dataHora().toLocalDate().atStartOfDay();
        LocalDateTime fimDia = dto.dataHora().toLocalDate().atTime(23, 59, 59);

        boolean clienteJaAgendadoHoje = agendamentoRepository.existsByClienteIdAndBarbeiroIdAndDiaLimit(
                dto.clienteId(), dto.barbeiroId(), inicioDia, fimDia, StatusAgendamento.AGENDADO);

        if (clienteJaAgendadoHoje) {
            throw new BusinessRuleException("Cliente já possui um agendamento marcado para este dia com este barbeiro.");
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

        // Regra 3: Cliente deve cancelar com pelo menos 1h de antecedência
        LocalDateTime limiteCancelamento = agendamento.getDataHora().minusHours(1);
        if (LocalDateTime.now().isAfter(limiteCancelamento)) {
            throw new BusinessRuleException("Agendamentos só podem ser cancelados com pelo menos 1 hora de antecedência.");
        }

        agendamento.setStatus(StatusAgendamento.CANCELADO);
        return agendamentoRepository.save(agendamento);
    }
}
