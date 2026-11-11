package dev.vitor.barbearia.ProjetoBarber.gestor;

import dev.vitor.barbearia.ProjetoBarber.agendamento.*;
import dev.vitor.barbearia.ProjetoBarber.barbeiro.BarbeiroDTO;
import dev.vitor.barbearia.ProjetoBarber.barbeiro.BarbeiroModel;
import dev.vitor.barbearia.ProjetoBarber.barbeiro.BarbeiroRepository;
import dev.vitor.barbearia.ProjetoBarber.barbeiro.BarbeiroService;
import dev.vitor.barbearia.ProjetoBarber.historico.AtendimentoHistoricoRepository;
import dev.vitor.barbearia.ProjetoBarber.historico.AtendimentoHistoricoResponseDTO;
import dev.vitor.barbearia.ProjetoBarber.servico.ServicoDTO;
import dev.vitor.barbearia.ProjetoBarber.servico.ServicoModel;
import dev.vitor.barbearia.ProjetoBarber.servico.ServicoRepository;
import dev.vitor.barbearia.ProjetoBarber.servico.ServicoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class GestorRelatorioHistoricoIntegrationTest {

    @Autowired
    private AgendamentoService agendamentoService;

    @Autowired
    private AgendamentoRepository agendamentoRepository;

    @Autowired
    private BarbeiroService barbeiroService;

    @Autowired
    private BarbeiroRepository barbeiroRepository;

    @Autowired
    private ServicoService servicoService;

    @Autowired
    private ServicoRepository servicoRepository;

    @Autowired
    private AtendimentoHistoricoRepository historicoRepository;

    @Autowired
    private GestorDashboardService dashboardService;

    @BeforeEach
    void limparBanco() {
        historicoRepository.deleteAll();
        agendamentoRepository.deleteAll();
        barbeiroRepository.deleteAll();
        servicoRepository.deleteAll();
    }

    @Test
    @DisplayName("Deve preservar histórico, caixa e cálculo de comissão mesmo após barbeiro ou serviço deixarem de existir")
    void devePreservarHistoricoECalcularCaixaEComissaoNoPeriodo() {
        // 1. Cria barbeiro com comissão de 40% e dois serviços
        BarbeiroModel barbeiro = barbeiroService.criar(new BarbeiroDTO("Rafael Navalha", "rafael@barber.com", "11999990000"));
        barbeiroService.atualizarPercentualComissao(barbeiro.getId(), new BigDecimal("40.00"));

        ServicoModel corte = servicoService.criar(new ServicoDTO("Corte Clássico", "Tesoura e máquina", new BigDecimal("50.00"), 30));
        ServicoModel barba = servicoService.criar(new ServicoDTO("Barba Terapia", "Toalha quente", new BigDecimal("40.00"), 30));

        // 2. Registra dois agendamentos no passado recente e conclui ambos
        LocalDateTime ontem10h = LocalDate.now().minusDays(1).atTime(10, 0);
        LocalDateTime ontem14h = LocalDate.now().minusDays(1).atTime(14, 0);

        AgendamentoModel ag1 = agendamentoRepository.save(AgendamentoModel.builder()
                .barbeiro(barbeiro)
                .servico(corte)
                .dataHora(ontem10h)
                .dataHoraFim(ontem10h.plusMinutes(30))
                .preco(corte.getPreco())
                .status(StatusAgendamento.AGENDADO)
                .nomeNaoUsuario("Cliente Avulso 1")
                .build());

        AgendamentoModel ag2 = agendamentoRepository.save(AgendamentoModel.builder()
                .barbeiro(barbeiro)
                .servico(barba)
                .dataHora(ontem14h)
                .dataHoraFim(ontem14h.plusMinutes(30))
                .preco(barba.getPreco())
                .status(StatusAgendamento.AGENDADO)
                .nomeNaoUsuario("Cliente Avulso 2")
                .build());

        // Conclui: 1º pelo preço de tabela (50.00 no PIX), 2º com valor pago customizado (45.00 em DINHEIRO)
        agendamentoService.concluirAtendimento(ag1.getId(), "gestor@tenoriosbarber.com", true,
                new ConcluirAtendimentoDTO(null, FormaPagamento.PIX));
        agendamentoService.concluirAtendimento(ag2.getId(), "rafael@barber.com", false,
                new ConcluirAtendimentoDTO(new BigDecimal("45.00"), FormaPagamento.DINHEIRO));

        Long barbeiroIdOriginal = barbeiro.getId();
        Long servicoCorteIdOriginal = corte.getId();

        // 3. Desativa/exclui logicamente E remove fisicamente agendamento/serviço/barbeiro para provar independência do histórico
        barbeiroService.excluir(barbeiroIdOriginal);
        servicoService.excluir(servicoCorteIdOriginal);
        agendamentoRepository.deleteAll();
        servicoRepository.deleteById(servicoCorteIdOriginal);
        barbeiroRepository.deleteById(barbeiroIdOriginal);

        LocalDate inicio = LocalDate.now().minusDays(5);
        LocalDate fim = LocalDate.now();

        // 4. Verifica relatório dinâmico do período
        RelatorioPeriodoDTO relatorio = dashboardService.obterRelatorioPeriodo(inicio, fim);
        assertEquals(2L, relatorio.totalAtendimentosRealizados());
        assertEquals(new BigDecimal("95.00"), relatorio.caixaTotal());
        assertEquals(new BigDecimal("38.00"), relatorio.comissaoTotal()); // 40% de 95.00
        assertEquals(new BigDecimal("57.00"), relatorio.lucroLiquido());
        assertEquals(1, relatorio.porBarbeiro().size());
        assertEquals("Rafael Navalha", relatorio.porBarbeiro().get(0).barbeiroNome());
        assertEquals(2, relatorio.porBarbeiro().get(0).servicos().size());

        // 5. Verifica histórico imutável preservado
        Page<AtendimentoHistoricoResponseDTO> historico = dashboardService.listarHistorico(
                inicio, fim, barbeiroIdOriginal, null, PageRequest.of(0, 10));
        assertEquals(2, historico.getTotalElements());

        // 6. Simula nova porcentagem de comissão (ex.: Gestor insere 55%) sobre os atendimentos realizados pelo barbeiro
        CalculoComissaoBarbeiroDTO simulacao = dashboardService.calcularComissaoBarbeiro(
                barbeiroIdOriginal, inicio, fim, new BigDecimal("55.00"));
        assertEquals("Rafael Navalha", simulacao.barbeiroNome());
        assertEquals(2L, simulacao.totalAtendimentos());
        assertEquals(new BigDecimal("95.00"), simulacao.totalFaturado());
        assertEquals(new BigDecimal("38.00"), simulacao.comissaoRegistradaNoHistorico());
        assertEquals(new BigDecimal("55.00"), simulacao.percentualAplicado());
        assertEquals(new BigDecimal("52.25"), simulacao.comissaoCalculada()); // 55% de 95.00
        assertEquals(new BigDecimal("42.75"), simulacao.liquidoBarbearia());
    }
}
