package dev.vitor.barbearia.ProjetoBarber.agendamento;

import dev.vitor.barbearia.ProjetoBarber.barbeiro.BarbeiroModel;
import dev.vitor.barbearia.ProjetoBarber.barbeiro.BarbeiroService;
import dev.vitor.barbearia.ProjetoBarber.cliente.ClienteModel;
import dev.vitor.barbearia.ProjetoBarber.cliente.ClienteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
class AgendamentoService {

    @Autowired
    private AgendamentoRepository agendamentoRepository;
    
    @Autowired
    private ClienteService clienteService;
    
    @Autowired
    private BarbeiroService barbeiroService;

    public Page<AgendamentoModel> listarTodos(Pageable pageable) {
        return agendamentoRepository.findAll(pageable);
    }

    public AgendamentoModel agendar(AgendamentoDTO dto) {
        // Regra 1: Verificar limite de agendamento por dia do cliente
        LocalDateTime inicioDia = dto.dataHora().toLocalDate().atStartOfDay();
        LocalDateTime fimDia = dto.dataHora().toLocalDate().atTime(23, 59, 59);
        
        boolean clienteJaAgendadoHoje = agendamentoRepository.existsByClienteIdAndDiaLimit(
                dto.clienteId(), inicioDia, fimDia, StatusAgendamento.AGENDADO);
                
        if (clienteJaAgendadoHoje) {
            throw new IllegalArgumentException("Cliente já possui um agendamento marcado para este dia.");
        }

        // Regra 2: Barbeiro não pode ter 2 clientes no mesmo horário
        boolean barbeiroOcupado = agendamentoRepository.existsByBarbeiroIdAndDataHoraAndStatus(
                dto.barbeiroId(), dto.dataHora(), StatusAgendamento.AGENDADO);
                
        if (barbeiroOcupado) {
            throw new IllegalArgumentException("O barbeiro selecionado não está disponível neste horário.");
        }

        // Usando os Services das outras features para garantir o encapsulamento
        ClienteModel cliente = clienteService.buscarPorId(dto.clienteId())
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado."));
                
        BarbeiroModel barbeiro = barbeiroService.buscarPorId(dto.barbeiroId())
                .orElseThrow(() -> new RuntimeException("Barbeiro não encontrado."));

        AgendamentoModel agendamento = new AgendamentoModel(null, cliente, barbeiro, dto.dataHora(), StatusAgendamento.AGENDADO);
        return agendamentoRepository.save(agendamento);
    }

    public AgendamentoModel cancelar(Long id) {
        AgendamentoModel agendamento = agendamentoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Agendamento não encontrado com ID: " + id));

        // Regra 3: Cliente deve cancelar com pelo menos 1h de antecedência
        LocalDateTime limiteCancelamento = agendamento.getDataHora().minusHours(1);
        if (LocalDateTime.now().isAfter(limiteCancelamento)) {
            throw new IllegalArgumentException("Agendamentos só podem ser cancelados com pelo menos 1 hora de antecedência.");
        }

        agendamento.setStatus(StatusAgendamento.CANCELADO);
        return agendamentoRepository.save(agendamento);
    }
}
