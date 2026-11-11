package dev.vitor.barbearia.ProjetoBarber.gestor;

import dev.vitor.barbearia.ProjetoBarber.agendamento.AgendamentoRepository;
import dev.vitor.barbearia.ProjetoBarber.agendamento.FormaPagamento;
import dev.vitor.barbearia.ProjetoBarber.agendamento.StatusAgendamento;
import dev.vitor.barbearia.ProjetoBarber.barbeiro.BarbeiroModel;
import dev.vitor.barbearia.ProjetoBarber.barbeiro.BarbeiroRepository;
import dev.vitor.barbearia.ProjetoBarber.exceptions.BusinessRuleException;
import dev.vitor.barbearia.ProjetoBarber.gestor.CalculoComissaoBarbeiroDTO.ItemComissaoServicoDTO;
import dev.vitor.barbearia.ProjetoBarber.gestor.RelatorioPeriodoDTO.*;
import dev.vitor.barbearia.ProjetoBarber.historico.AtendimentoHistoricoModel;
import dev.vitor.barbearia.ProjetoBarber.historico.AtendimentoHistoricoRepository;
import dev.vitor.barbearia.ProjetoBarber.historico.AtendimentoHistoricoResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GestorDashboardService {

    private static final BigDecimal CEM = new BigDecimal("100");
    private static final BigDecimal ZERO = new BigDecimal("0.00");

    private final AgendamentoRepository agendamentoRepository;
    private final BarbeiroRepository barbeiroRepository;
    private final AtendimentoHistoricoRepository historicoRepository;

    @Transactional(readOnly = true)
    public DashboardResponseDTO obterMetricas(LocalDateTime inicio, LocalDateTime fim) {
        LocalDateTime de = inicio != null ? inicio : LocalDate.of(2000, 1, 1).atStartOfDay();
        LocalDateTime ate = fim != null ? fim : LocalDate.now().plusYears(10).atStartOfDay();

        boolean temFiltroPeriodo = (inicio != null && fim != null);
        long totalAgendamentos = temFiltroPeriodo
                ? agendamentoRepository.countByDataHoraBetween(inicio, fim)
                : agendamentoRepository.count();
        long cancelados = temFiltroPeriodo
                ? agendamentoRepository.countByDataHoraBetweenAndStatus(inicio, fim, StatusAgendamento.CANCELADO)
                : agendamentoRepository.countByStatus(StatusAgendamento.CANCELADO);
        long pendentes = temFiltroPeriodo
                ? agendamentoRepository.countByDataHoraBetweenAndStatus(inicio, fim, StatusAgendamento.AGENDADO)
                : agendamentoRepository.countByStatus(StatusAgendamento.AGENDADO);

        Map<Long, BarbeiroModel> barbeirosMap = barbeiroRepository.findAll().stream()
                .collect(Collectors.toMap(BarbeiroModel::getId, Function.identity()));

        List<Object[]> rowsBarbeiro = historicoRepository.totalizarPorBarbeiro(de, ate);

        long concluidos = 0L;
        BigDecimal faturamentoTotal = ZERO;
        BigDecimal comissaoTotalBarbeiros = ZERO;
        List<ComissaoBarbeiroDTO> comissoes = new ArrayList<>();

        for (Object[] row : rowsBarbeiro) {
            Long barbeiroId = (Long) row[0];
            String barbeiroNomeHistorico = (String) row[1];
            Long qtd = (Long) row[2];
            BigDecimal totalPago = toMoney((BigDecimal) row[3]);
            BigDecimal totalComissao = toMoney((BigDecimal) row[4]);

            BarbeiroModel barbeiro = barbeirosMap.get(barbeiroId);
            String nomeExibicao = barbeiro != null ? barbeiro.getNome() : barbeiroNomeHistorico;
            BigDecimal percentualExibicao = barbeiro != null && barbeiro.getPercentualComissao() != null
                    ? barbeiro.getPercentualComissao()
                    : (totalPago.compareTo(BigDecimal.ZERO) > 0
                            ? totalComissao.multiply(CEM).divide(totalPago, 2, RoundingMode.HALF_UP)
                            : BarbeiroModel.PERCENTUAL_COMISSAO_PADRAO);

            concluidos += qtd;
            faturamentoTotal = faturamentoTotal.add(totalPago);
            comissaoTotalBarbeiros = comissaoTotalBarbeiros.add(totalComissao);

            comissoes.add(new ComissaoBarbeiroDTO(
                    barbeiroId,
                    nomeExibicao,
                    qtd,
                    totalPago,
                    totalComissao,
                    percentualExibicao
            ));
        }

        BigDecimal lucroLiquido = faturamentoTotal.subtract(comissaoTotalBarbeiros);

        return new DashboardResponseDTO(
                totalAgendamentos,
                concluidos,
                cancelados,
                pendentes,
                faturamentoTotal,
                comissaoTotalBarbeiros,
                lucroLiquido,
                comissoes
        );
    }

    @Transactional(readOnly = true)
    public RelatorioPeriodoDTO obterRelatorioPeriodo(LocalDate dataInicio, LocalDate dataFim) {
        IntervaloPeriodo intervalo = resolverIntervalo(dataInicio, dataFim);

        LocalDateTime fimInclusivo = intervalo.fimExclusivo().minusNanos(1);
        long pendentes = agendamentoRepository.countByDataHoraBetweenAndStatus(
                intervalo.inicio(), fimInclusivo, StatusAgendamento.AGENDADO);
        long cancelados = agendamentoRepository.countByDataHoraBetweenAndStatus(
                intervalo.inicio(), fimInclusivo, StatusAgendamento.CANCELADO);

        // 1. Por forma de pagamento
        List<CaixaPorFormaPagamentoDTO> porFormaPagamento = historicoRepository
                .totalizarPorFormaPagamento(intervalo.inicio(), intervalo.fimExclusivo())
                .stream()
                .map(r -> new CaixaPorFormaPagamentoDTO(
                        r[0] instanceof FormaPagamento fp ? fp.name() : String.valueOf(r[0]),
                        (Long) r[1],
                        toMoney((BigDecimal) r[2]),
                        toMoney((BigDecimal) r[3])
                ))
                .sorted(Comparator.comparing(CaixaPorFormaPagamentoDTO::totalPago).reversed())
                .toList();

        // 2. Por barbeiro + serviços feitos por cada barbeiro (mantém barbeiros e serviços mesmo se excluídos/inativos depois)
        Map<Long, BarbeiroModel> barbeirosAtuais = barbeiroRepository.findAll().stream()
                .collect(Collectors.toMap(BarbeiroModel::getId, Function.identity()));

        Map<Long, List<ResumoServicoPeriodoDTO>> servicosPorBarbeiro = new HashMap<>();
        for (Object[] r : historicoRepository.totalizarPorBarbeiroEServico(intervalo.inicio(), intervalo.fimExclusivo())) {
            Long bId = (Long) r[0];
            Long sId = (Long) r[1];
            String sNome = (String) r[2];
            Long qtd = (Long) r[3];
            BigDecimal pago = toMoney((BigDecimal) r[4]);
            servicosPorBarbeiro.computeIfAbsent(bId, k -> new ArrayList<>())
                    .add(new ResumoServicoPeriodoDTO(sId, sNome, qtd, pago));
        }
        servicosPorBarbeiro.values().forEach(l ->
                l.sort(Comparator.comparing(ResumoServicoPeriodoDTO::quantidade).reversed()));

        List<ResumoBarbeiroPeriodoDTO> porBarbeiro = new ArrayList<>();
        long totalRealizados = 0L;
        BigDecimal caixaTotal = ZERO;
        BigDecimal comissaoTotal = ZERO;

        for (Object[] r : historicoRepository.totalizarPorBarbeiro(intervalo.inicio(), intervalo.fimExclusivo())) {
            Long bId = (Long) r[0];
            String bNomeSnap = (String) r[1];
            Long qtd = (Long) r[2];
            BigDecimal pago = toMoney((BigDecimal) r[3]);
            BigDecimal comissao = toMoney((BigDecimal) r[4]);

            BarbeiroModel bModel = barbeirosAtuais.get(bId);
            Boolean ativo = bModel != null ? bModel.getAtivo() : Boolean.FALSE;
            BigDecimal pctAtual = bModel != null ? bModel.getPercentualComissao() : null;

            totalRealizados += qtd;
            caixaTotal = caixaTotal.add(pago);
            comissaoTotal = comissaoTotal.add(comissao);

            porBarbeiro.add(new ResumoBarbeiroPeriodoDTO(
                    bId,
                    bNomeSnap,
                    ativo,
                    pctAtual,
                    qtd,
                    pago,
                    comissao,
                    servicosPorBarbeiro.getOrDefault(bId, List.of())
            ));
        }

        // 3. Por serviço (ranking geral do período)
        List<ResumoServicoPeriodoDTO> porServico = historicoRepository
                .totalizarPorServico(intervalo.inicio(), intervalo.fimExclusivo())
                .stream()
                .map(r -> new ResumoServicoPeriodoDTO(
                        (Long) r[0],
                        (String) r[1],
                        (Long) r[2],
                        toMoney((BigDecimal) r[3])
                ))
                .toList();

        // 4. Evolução diária do caixa
        Map<LocalDate, BigDecimal> valorPorDia = new TreeMap<>();
        Map<LocalDate, Long> qtdPorDia = new TreeMap<>();
        for (Object[] r : historicoRepository.listarValoresPorData(intervalo.inicio(), intervalo.fimExclusivo())) {
            LocalDate dia = ((LocalDateTime) r[0]).toLocalDate();
            BigDecimal val = toMoney((BigDecimal) r[1]);
            valorPorDia.merge(dia, val, BigDecimal::add);
            qtdPorDia.merge(dia, 1L, Long::sum);
        }
        List<CaixaDiarioDTO> porDia = valorPorDia.entrySet().stream()
                .map(e -> new CaixaDiarioDTO(e.getKey(), qtdPorDia.getOrDefault(e.getKey(), 0L), e.getValue()))
                .toList();

        BigDecimal ticketMedio = totalRealizados > 0
                ? caixaTotal.divide(BigDecimal.valueOf(totalRealizados), 2, RoundingMode.HALF_UP)
                : ZERO;
        BigDecimal lucroLiquido = caixaTotal.subtract(comissaoTotal);

        return new RelatorioPeriodoDTO(
                intervalo.dataInicio(),
                intervalo.dataFim(),
                totalRealizados,
                pendentes,
                cancelados,
                caixaTotal,
                ticketMedio,
                comissaoTotal,
                lucroLiquido,
                porFormaPagamento,
                porDia,
                porBarbeiro,
                porServico
        );
    }

    @Transactional(readOnly = true)
    public Page<AtendimentoHistoricoResponseDTO> listarHistorico(
            LocalDate dataInicio,
            LocalDate dataFim,
            Long barbeiroId,
            Long servicoId,
            Pageable pageable) {
        IntervaloPeriodo intervalo = resolverIntervalo(dataInicio, dataFim);
        Page<AtendimentoHistoricoModel> page = historicoRepository.buscarPorPeriodo(
                intervalo.inicio(), intervalo.fimExclusivo(), barbeiroId, servicoId, pageable);
        return page.map(AtendimentoHistoricoResponseDTO::from);
    }

    /**
     * Calcula e/ou simula a comissão de um barbeiro específico no período,
     * permitindo que o Gestor informe qualquer percentual (0–100%) para análise.
     * Funciona mesmo que o barbeiro já tenha sido desativado ou removido do cadastro ativo.
     */
    @Transactional(readOnly = true)
    public CalculoComissaoBarbeiroDTO calcularComissaoBarbeiro(
            Long barbeiroId,
            LocalDate dataInicio,
            LocalDate dataFim,
            BigDecimal percentualPersonalizado) {
        if (percentualPersonalizado != null
                && (percentualPersonalizado.compareTo(BigDecimal.ZERO) < 0 || percentualPersonalizado.compareTo(CEM) > 0)) {
            throw new BusinessRuleException("O percentual de comissão deve estar entre 0% e 100%.");
        }

        IntervaloPeriodo intervalo = resolverIntervalo(dataInicio, dataFim);
        Optional<BarbeiroModel> barbeiroOpt = barbeiroRepository.findById(barbeiroId);

        BigDecimal percentualCadastrado = barbeiroOpt
                .map(BarbeiroModel::getPercentualComissao)
                .orElse(BarbeiroModel.PERCENTUAL_COMISSAO_PADRAO);
        BigDecimal percentualAplicado = (percentualPersonalizado != null ? percentualPersonalizado : percentualCadastrado)
                .setScale(2, RoundingMode.HALF_UP);

        // Detalhamento por serviço realizado pelo barbeiro no período
        List<Object[]> rowsServico = historicoRepository
                .totalizarPorBarbeiroEServico(intervalo.inicio(), intervalo.fimExclusivo())
                .stream()
                .filter(r -> Objects.equals(r[0], barbeiroId))
                .toList();

        List<ItemComissaoServicoDTO> itens = new ArrayList<>();
        long totalAtendimentos = 0L;
        BigDecimal totalFaturado = ZERO;
        BigDecimal comissaoRegistrada = ZERO;

        for (Object[] r : rowsServico) {
            Long sId = (Long) r[1];
            String sNome = (String) r[2];
            Long qtd = (Long) r[3];
            BigDecimal pago = toMoney((BigDecimal) r[4]);
            BigDecimal comReg = toMoney((BigDecimal) r[5]);
            BigDecimal comCalc = pago.multiply(percentualAplicado).divide(CEM, 2, RoundingMode.HALF_UP);

            totalAtendimentos += qtd;
            totalFaturado = totalFaturado.add(pago);
            comissaoRegistrada = comissaoRegistrada.add(comReg);

            itens.add(new ItemComissaoServicoDTO(sId, sNome, qtd, pago, comReg, comCalc));
        }
        itens.sort(Comparator.comparing(ItemComissaoServicoDTO::quantidade).reversed());

        // Nome do barbeiro: cadastro atual ou snapshot do histórico
        String nomeBarbeiro = barbeiroOpt.map(BarbeiroModel::getNome).orElseGet(() ->
                historicoRepository.totalizarPorBarbeiro(intervalo.inicio(), intervalo.fimExclusivo()).stream()
                        .filter(r -> Objects.equals(r[0], barbeiroId))
                        .map(r -> (String) r[1])
                        .findFirst()
                        .orElse("Barbeiro #" + barbeiroId));
        Boolean ativo = barbeiroOpt.map(BarbeiroModel::getAtivo).orElse(Boolean.FALSE);

        BigDecimal comissaoCalculada = totalFaturado.multiply(percentualAplicado).divide(CEM, 2, RoundingMode.HALF_UP);
        BigDecimal liquidoBarbearia = totalFaturado.subtract(comissaoCalculada);

        return new CalculoComissaoBarbeiroDTO(
                barbeiroId,
                nomeBarbeiro,
                ativo,
                intervalo.dataInicio(),
                intervalo.dataFim(),
                totalAtendimentos,
                totalFaturado,
                percentualCadastrado,
                comissaoRegistrada,
                percentualAplicado,
                comissaoCalculada,
                liquidoBarbearia,
                itens
        );
    }

    private IntervaloPeriodo resolverIntervalo(LocalDate dataInicio, LocalDate dataFim) {
        LocalDate fim = dataFim != null ? dataFim : LocalDate.now();
        LocalDate inicio = dataInicio != null ? dataInicio : fim.withDayOfMonth(1);
        if (inicio.isAfter(fim)) {
            throw new BusinessRuleException("A data inicial não pode ser posterior à data final.");
        }
        return new IntervaloPeriodo(inicio, fim, inicio.atStartOfDay(), fim.plusDays(1).atStartOfDay());
    }

    private BigDecimal toMoney(BigDecimal val) {
        return val != null ? val.setScale(2, RoundingMode.HALF_UP) : ZERO;
    }

    private record IntervaloPeriodo(
            LocalDate dataInicio,
            LocalDate dataFim,
            LocalDateTime inicio,
            LocalDateTime fimExclusivo
    ) {}
}
