package dev.vitor.barbearia.ProjetoBarber.gestor;

import dev.vitor.barbearia.ProjetoBarber.agendamento.AgendamentoModel;
import dev.vitor.barbearia.ProjetoBarber.agendamento.AgendamentoRepository;
import dev.vitor.barbearia.ProjetoBarber.agendamento.StatusAgendamento;
import dev.vitor.barbearia.ProjetoBarber.barbeiro.BarbeiroModel;
import dev.vitor.barbearia.ProjetoBarber.barbeiro.BarbeiroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GestorDashboardService {

    private final AgendamentoRepository agendamentoRepository;
    private final BarbeiroRepository barbeiroRepository;

    private static final BigDecimal PERCENTUAL_COMISSAO_PADRAO = new BigDecimal("0.50"); // 50% de comissão

    @Transactional(readOnly = true)
    public DashboardResponseDTO obterMetricas(LocalDateTime inicio, LocalDateTime fim) {
        List<AgendamentoModel> agendamentos = (inicio != null && fim != null)
                ? agendamentoRepository.findByDataHoraBetween(inicio, fim)
                : agendamentoRepository.findAll();

        long totalAgendamentos = agendamentos.size();
        long concluidos = agendamentos.stream().filter(a -> a.getStatus() == StatusAgendamento.CONCLUIDO).count();
        long cancelados = agendamentos.stream().filter(a -> a.getStatus() == StatusAgendamento.CANCELADO).count();
        long pendentes = agendamentos.stream().filter(a -> a.getStatus() == StatusAgendamento.AGENDADO).count();

        // O faturamento contabiliza serviços concluídos (ou agendados caso ainda não marcados como concluídos)
        List<AgendamentoModel> faturaveis = agendamentos.stream()
                .filter(a -> a.getStatus() == StatusAgendamento.CONCLUIDO || a.getStatus() == StatusAgendamento.AGENDADO)
                .toList();

        BigDecimal faturamentoTotal = faturaveis.stream()
                .map(a -> a.getPreco() != null ? a.getPreco() : (a.getServico() != null ? a.getServico().getPreco() : BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Agrupamento por Barbeiro para cálculo das comissões
        Map<BarbeiroModel, List<AgendamentoModel>> porBarbeiro = faturaveis.stream()
                .collect(Collectors.groupingBy(AgendamentoModel::getBarbeiro));

        List<ComissaoBarbeiroDTO> comissoes = new ArrayList<>();
        BigDecimal comissaoTotalBarbeiros = BigDecimal.ZERO;

        for (Map.Entry<BarbeiroModel, List<AgendamentoModel>> entry : porBarbeiro.entrySet()) {
            BarbeiroModel barbeiro = entry.getKey();
            List<AgendamentoModel> atendimentos = entry.getValue();

            BigDecimal faturamentoBarbeiro = atendimentos.stream()
                    .map(a -> a.getPreco() != null ? a.getPreco() : (a.getServico() != null ? a.getServico().getPreco() : BigDecimal.ZERO))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal valorComissao = faturamentoBarbeiro.multiply(PERCENTUAL_COMISSAO_PADRAO).setScale(2, RoundingMode.HALF_UP);
            comissaoTotalBarbeiros = comissaoTotalBarbeiros.add(valorComissao);

            comissoes.add(new ComissaoBarbeiroDTO(
                    barbeiro.getId(),
                    barbeiro.getNome(),
                    (long) atendimentos.size(),
                    faturamentoBarbeiro,
                    valorComissao,
                    PERCENTUAL_COMISSAO_PADRAO.multiply(new BigDecimal("100"))
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
}
