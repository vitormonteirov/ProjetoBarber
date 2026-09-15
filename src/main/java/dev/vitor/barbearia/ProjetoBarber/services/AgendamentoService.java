package dev.vitor.barbearia.ProjetoBarber.services;

import dev.vitor.barbearia.ProjetoBarber.dtos.AgendamentoDTO;
import dev.vitor.barbearia.ProjetoBarber.models.Agendamento;
import dev.vitor.barbearia.ProjetoBarber.models.Barbeiro;
import dev.vitor.barbearia.ProjetoBarber.models.Cliente;
import dev.vitor.barbearia.ProjetoBarber.models.StatusAgendamento;
import dev.vitor.barbearia.ProjetoBarber.repositories.AgendamentoRepository;
import dev.vitor.barbearia.ProjetoBarber.repositories.BarbeiroRepository;
import dev.vitor.barbearia.ProjetoBarber.repositories.ClienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AgendamentoService {

    @Autowired
    private AgendamentoRepository agendamentoRepository;
    
    @Autowired
    private ClienteRepository clienteRepository;
    
    @Autowired
    private BarbeiroRepository barbeiroRepository;

    public Page<Agendamento> listarTodos(Pageable pageable) {
        return agendamentoRepository.findAll(pageable);
    }

    public Agendamento agendar(AgendamentoDTO dto) {
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

        Cliente cliente = clienteRepository.findById(dto.clienteId())
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado."));
                
        Barbeiro barbeiro = barbeiroRepository.findById(dto.barbeiroId())
                .orElseThrow(() -> new RuntimeException("Barbeiro não encontrado."));

        Agendamento agendamento = new Agendamento(null, cliente, barbeiro, dto.dataHora(), StatusAgendamento.AGENDADO);
        return agendamentoRepository.save(agendamento);
    }

    public Agendamento cancelar(Long id) {
        Agendamento agendamento = agendamentoRepository.findById(id)
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
