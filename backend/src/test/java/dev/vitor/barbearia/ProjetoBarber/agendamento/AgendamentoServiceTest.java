package dev.vitor.barbearia.ProjetoBarber.agendamento;

import dev.vitor.barbearia.ProjetoBarber.barbeiro.AlterarHorarioDTO;
import dev.vitor.barbearia.ProjetoBarber.barbeiro.BarbeiroModel;
import dev.vitor.barbearia.ProjetoBarber.barbeiro.BarbeiroService;
import dev.vitor.barbearia.ProjetoBarber.cliente.ClienteModel;
import dev.vitor.barbearia.ProjetoBarber.cliente.ClienteService;
import dev.vitor.barbearia.ProjetoBarber.exceptions.BusinessRuleException;
import dev.vitor.barbearia.ProjetoBarber.historico.AtendimentoHistoricoModel;
import dev.vitor.barbearia.ProjetoBarber.historico.AtendimentoHistoricoRepository;
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

    @Mock
    private AtendimentoHistoricoRepository historicoRepository;

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

    @Test
    @DisplayName("Deve concluir atendimento e gravar snapshot imutável no histórico com comissão calculada")
    void deveConcluirAtendimentoEGravarHistorico() {
        barbeiro.setPercentualComissao(new BigDecimal("40.00"));
        AgendamentoModel agendamento = AgendamentoModel.builder()
                .id(20L)
                .cliente(cliente)
                .barbeiro(barbeiro)
                .servico(servico)
                .dataHora(LocalDateTime.now().minusMinutes(30))
                .preco(new BigDecimal("50.00"))
                .status(StatusAgendamento.AGENDADO)
                .build();

        when(agendamentoRepository.findById(20L)).thenReturn(Optional.of(agendamento));
        when(historicoRepository.existsByAgendamentoId(20L)).thenReturn(false);
        when(historicoRepository.save(any(AtendimentoHistoricoModel.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ConcluirAtendimentoDTO dto = new ConcluirAtendimentoDTO(new BigDecimal("60.00"), FormaPagamento.PIX);
        AtendimentoHistoricoModel historico = agendamentoService.concluirAtendimento(
                20L, "carlos@barber.com", false, dto);

        assertEquals(StatusAgendamento.CONCLUIDO, agendamento.getStatus());
        assertEquals("Carlos", historico.getBarbeiroNome());
        assertEquals("Corte Degrade", historico.getServicoNome());
        assertEquals(new BigDecimal("60.00"), historico.getValorPago());
        assertEquals(FormaPagamento.PIX, historico.getFormaPagamento());
        assertEquals(new BigDecimal("40.00"), historico.getPercentualComissao());
        assertEquals(new BigDecimal("24.00"), historico.getValorComissao());
    }

    @Test
    @DisplayName("Deve aplicar pausa padrão de 12:00 às 13:00 e impedir agendamento fora do expediente ou na pausa")
    void deveImpedirAgendamentoForaDoExpedienteOuNaPausa() {
        // Verifica que o barbeiro recém-instanciado já possui pausa padrão de 12:00 às 13:00
        assertEquals(java.time.LocalTime.of(12, 0), barbeiro.getHorarioInicioPausa());
        assertEquals(java.time.LocalTime.of(13, 0), barbeiro.getHorarioFimPausa());

        barbeiro.setHorarioInicioExpediente(java.time.LocalTime.of(9, 0));
        barbeiro.setHorarioFimExpediente(java.time.LocalTime.of(18, 0));

        when(clienteService.buscarPorId(1L)).thenReturn(Optional.of(cliente));
        when(barbeiroService.buscarPorIdComLock(1L)).thenReturn(Optional.of(barbeiro));
        when(servicoService.buscarPorId(1L)).thenReturn(Optional.of(servico));

        // 1. Tentativa antes do início do expediente (08:00 quando abre às 09:00)
        LocalDateTime antesExpediente = LocalDateTime.now().plusDays(1).withHour(8).withMinute(0);
        AgendamentoDTO dtoFora = new AgendamentoDTO(1L, 1L, 1L, antesExpediente);
        BusinessRuleException exFora = assertThrows(BusinessRuleException.class, () -> agendamentoService.agendar(dtoFora));
        assertTrue(exFora.getMessage().contains("fora do expediente"));

        // 2. Tentativa colidindo com a pausa padrão de almoço (12:00 às 12:40 quando pausa padrão é 12:00–13:00)
        LocalDateTime durantePausaPadrao = LocalDateTime.now().plusDays(1).withHour(12).withMinute(0);
        AgendamentoDTO dtoPausa = new AgendamentoDTO(1L, 1L, 1L, durantePausaPadrao);
        BusinessRuleException exPausa = assertThrows(BusinessRuleException.class, () -> agendamentoService.agendar(dtoPausa));
        assertTrue(exPausa.getMessage().contains("pausa/almoço"));

        // 3. Gestor altera a pausa do barbeiro para 13:00 às 14:00 -> o horário de 12:00 passa a ser permitido e 13:00 bloqueado
        barbeiro.setHorarioInicioPausa(java.time.LocalTime.of(13, 0));
        barbeiro.setHorarioFimPausa(java.time.LocalTime.of(14, 0));

        LocalDateTime duranteNovaPausa = LocalDateTime.now().plusDays(1).withHour(13).withMinute(0);
        AgendamentoDTO dtoNovaPausa = new AgendamentoDTO(1L, 1L, 1L, duranteNovaPausa);
        BusinessRuleException exNovaPausa = assertThrows(BusinessRuleException.class, () -> agendamentoService.agendar(dtoNovaPausa));
        assertTrue(exNovaPausa.getMessage().contains("13:00 às 14:00"));

        verify(agendamentoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve excluir slots do intervalo de pausa ao calcular horários disponíveis do barbeiro")
    void deveExcluirIntervaloDePausaNosHorariosDisponiveis() {
        barbeiro.setHorarioInicioExpediente(java.time.LocalTime.of(9, 0));
        barbeiro.setHorarioFimExpediente(java.time.LocalTime.of(14, 0));
        barbeiro.setHorarioInicioPausa(java.time.LocalTime.of(12, 0));
        barbeiro.setHorarioFimPausa(java.time.LocalTime.of(13, 0));

        ServicoModel servico30Min = ServicoModel.builder()
                .id(2L).nome("Corte Rápido").duracaoMinutos(30).preco(new BigDecimal("35.00")).ativo(true).build();

        when(servicoService.buscarPorId(2L)).thenReturn(Optional.of(servico30Min));
        when(barbeiroService.buscarPorId(1L)).thenReturn(Optional.of(barbeiro));
        when(agendamentoRepository.findByBarbeiroIdAndDataHoraBetweenAndStatus(eq(1L), any(), any(), eq(StatusAgendamento.AGENDADO)))
                .thenReturn(java.util.List.of());

        java.util.List<java.time.LocalTime> slots = agendamentoService.calcularHorariosDisponiveis(
                1L, 2L, java.time.LocalDate.now().plusDays(2));

        assertTrue(slots.contains(java.time.LocalTime.of(9, 0)));
        assertTrue(slots.contains(java.time.LocalTime.of(11, 30)));
        assertFalse(slots.contains(java.time.LocalTime.of(12, 0)), "Slot 12:00 deve estar bloqueado pela pausa");
        assertFalse(slots.contains(java.time.LocalTime.of(12, 30)), "Slot 12:30 deve estar bloqueado pela pausa");
        assertTrue(slots.contains(java.time.LocalTime.of(13, 0)));
        assertTrue(slots.contains(java.time.LocalTime.of(13, 30)));
    }

    @Test
    @DisplayName("Deve registrar ausência (NAO_COMPARECEU / No-Show) sem gerar histórico financeiro")
    void deveRegistrarNaoCompareceuSemGerarHistoricoFinanceiro() {
        AgendamentoModel agendamento = AgendamentoModel.builder()
                .id(30L)
                .cliente(cliente)
                .barbeiro(barbeiro)
                .servico(servico)
                .dataHora(LocalDateTime.now().minusMinutes(15))
                .status(StatusAgendamento.AGENDADO)
                .build();

        when(agendamentoRepository.findById(30L)).thenReturn(Optional.of(agendamento));
        when(agendamentoRepository.save(any(AgendamentoModel.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AgendamentoModel resultado = agendamentoService.marcarNaoCompareceu(
                30L, "carlos@barber.com", false, "Cliente não apareceu após 20min de tolerância");

        assertEquals(StatusAgendamento.NAO_COMPARECEU, resultado.getStatus());
        assertTrue(resultado.getNotaAlteracao().contains("No-Show registrado pelo Barbeiro"));
        verify(historicoRepository, never()).save(any());
    }
}
