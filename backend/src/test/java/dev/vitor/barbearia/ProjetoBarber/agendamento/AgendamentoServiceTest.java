package dev.vitor.barbearia.ProjetoBarber.agendamento;

import dev.vitor.barbearia.ProjetoBarber.barbeiro.AlterarHorarioDTO;
import dev.vitor.barbearia.ProjetoBarber.barbeiro.BarbeiroModel;
import dev.vitor.barbearia.ProjetoBarber.barbeiro.BarbeiroService;
import dev.vitor.barbearia.ProjetoBarber.cliente.ClienteModel;
import dev.vitor.barbearia.ProjetoBarber.cliente.ClienteService;
import dev.vitor.barbearia.ProjetoBarber.exceptions.BusinessRuleException;
import dev.vitor.barbearia.ProjetoBarber.servico.ServicoModel;
import dev.vitor.barbearia.ProjetoBarber.servico.ServicoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgendamentoServiceTest {

    @Mock
    private AgendamentoRepository agendamentoRepository;

    @Mock
    private ClienteService clienteService;

    @Mock
    private BarbeiroService barbeiroService;

    @Mock
    private ServicoService servicoService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AgendamentoService agendamentoService;

    private ClienteModel cliente;
    private BarbeiroModel barbeiro;
    private ServicoModel servico;
    private LocalDateTime horarioFuturo;

    @BeforeEach
    void setUp() {
        cliente = ClienteModel.builder().id(1L).nome("João").ativo(true).build();
        barbeiro = BarbeiroModel.builder().id(1L).nome("Carlos").email("carlos@barber.com").ativo(true).build();
        servico = ServicoModel.builder().id(1L).nome("Corte Degrade").duracaoMinutos(40).preco(new BigDecimal("45.00")).ativo(true).build();
        horarioFuturo = LocalDateTime.now().plusDays(1).withHour(14).withMinute(0);
    }

    @Test
    @DisplayName("Deve agendar com sucesso para cliente cadastrado e emitir evento assíncrono")
    void deveAgendarComSucesso() {
        AgendamentoDTO dto = new AgendamentoDTO(1L, 1L, 1L, horarioFuturo);

        when(clienteService.buscarPorId(1L)).thenReturn(Optional.of(cliente));
        when(barbeiroService.buscarPorIdComLock(1L)).thenReturn(Optional.of(barbeiro));
        when(servicoService.buscarPorId(1L)).thenReturn(Optional.of(servico));
        when(agendamentoRepository.existsByClienteIdAndBarbeiroIdAndDiaLimit(eq(1L), eq(1L), any(), any(), eq(StatusAgendamento.AGENDADO))).thenReturn(false);
        when(agendamentoRepository.existsConflitoHorarioBarbeiro(eq(1L), eq(horarioFuturo), any(), eq(StatusAgendamento.AGENDADO))).thenReturn(false);
        when(agendamentoRepository.save(any(AgendamentoModel.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AgendamentoModel resultado = agendamentoService.agendar(dto);

        assertNotNull(resultado);
        assertEquals(StatusAgendamento.AGENDADO, resultado.getStatus());
        assertEquals(new BigDecimal("45.00"), resultado.getPreco());
        assertEquals(horarioFuturo.plusMinutes(40), resultado.getDataHoraFim());
        verify(agendamentoRepository, times(1)).save(any(AgendamentoModel.class));
        verify(eventPublisher, times(1)).publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("Deve agendar para cliente não cadastrado (não usuário) informando nome e telefone")
    void deveAgendarParaNaoUsuario() {
        AgendamentoDTO dto = new AgendamentoDTO(null, 1L, 1L, horarioFuturo, "Marcos Avulso", "11988887777");

        when(barbeiroService.buscarPorIdComLock(1L)).thenReturn(Optional.of(barbeiro));
        when(servicoService.buscarPorId(1L)).thenReturn(Optional.of(servico));
        when(agendamentoRepository.existsConflitoHorarioBarbeiro(eq(1L), eq(horarioFuturo), any(), eq(StatusAgendamento.AGENDADO))).thenReturn(false);
        when(agendamentoRepository.save(any(AgendamentoModel.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AgendamentoModel resultado = agendamentoService.agendar(dto);

        assertNotNull(resultado);
        assertNull(resultado.getCliente());
        assertEquals("Marcos Avulso", resultado.getNomeNaoUsuario());
        assertEquals("11988887777", resultado.getTelefoneNaoUsuario());
        verify(clienteService, never()).buscarPorId(any());
        verify(eventPublisher, times(1)).publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("Gestor pode alterar horário de qualquer barbeiro")
    void gestorPodeAlterarHorarioDeQualquerBarbeiro() {
        AgendamentoModel agendamento = AgendamentoModel.builder()
                .id(5L)
                .barbeiro(barbeiro)
                .dataHora(horarioFuturo)
                .dataHoraFim(horarioFuturo.plusMinutes(30))
                .status(StatusAgendamento.AGENDADO)
                .build();

        LocalDateTime novoHorario = horarioFuturo.plusHours(2);
        AlterarHorarioDTO dto = new AlterarHorarioDTO(novoHorario, "Cliente solicitou via WhatsApp");

        when(agendamentoRepository.findById(5L)).thenReturn(Optional.of(agendamento));
        when(barbeiroService.buscarPorIdComLock(1L)).thenReturn(Optional.of(barbeiro));
        when(agendamentoRepository.existsConflitoHorarioBarbeiroIgnorandoId(eq(1L), eq(novoHorario), any(), eq(StatusAgendamento.AGENDADO), eq(5L))).thenReturn(false);
        when(agendamentoRepository.save(any(AgendamentoModel.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AgendamentoModel resultado = agendamentoService.alterarHorarioPeloBarbeiroOuGestor(
                5L, "gestor@tenoriosbarber.com", true, dto);

        assertEquals(novoHorario, resultado.getDataHora());
        assertTrue(resultado.getNotaAlteracao().contains("Alterado pelo Gestor"));
    }

    @Test
    @DisplayName("Deve lançar BusinessRuleException quando houver conflito de horário para o barbeiro")
    void deveLancarExcecaoQuandoConflitoDeHorario() {
        AgendamentoDTO dto = new AgendamentoDTO(1L, 1L, 1L, horarioFuturo);

        when(clienteService.buscarPorId(1L)).thenReturn(Optional.of(cliente));
        when(barbeiroService.buscarPorIdComLock(1L)).thenReturn(Optional.of(barbeiro));
        when(servicoService.buscarPorId(1L)).thenReturn(Optional.of(servico));
        when(agendamentoRepository.existsByClienteIdAndBarbeiroIdAndDiaLimit(eq(1L), eq(1L), any(), any(), eq(StatusAgendamento.AGENDADO))).thenReturn(false);
        when(agendamentoRepository.existsConflitoHorarioBarbeiro(eq(1L), eq(horarioFuturo), any(), eq(StatusAgendamento.AGENDADO))).thenReturn(true);

        BusinessRuleException exception = assertThrows(BusinessRuleException.class, () -> agendamentoService.agendar(dto));
        assertTrue(exception.getMessage().contains("conflito de agenda"));
        verify(agendamentoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve impedir cancelamento com menos de 1 hora de antecedência")
    void deveImpedirCancelamentoComPoucaAntecedencia() {
        LocalDateTime emCinquentaMinutos = LocalDateTime.now().plusMinutes(50);
        AgendamentoModel agendamento = AgendamentoModel.builder()
                .id(10L)
                .dataHora(emCinquentaMinutos)
                .status(StatusAgendamento.AGENDADO)
                .build();

        when(agendamentoRepository.findById(10L)).thenReturn(Optional.of(agendamento));

        assertThrows(BusinessRuleException.class, () -> agendamentoService.cancelar(10L));
    }
}
