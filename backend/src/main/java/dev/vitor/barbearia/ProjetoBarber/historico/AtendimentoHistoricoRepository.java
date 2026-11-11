package dev.vitor.barbearia.ProjetoBarber.historico;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Consultas do histórico. Todos os períodos usam intervalo semiaberto:
 * {@code inicio} (inclusivo) até {@code fim} (exclusivo).
 * As agregações são feitas no banco (GROUP BY) para escalar com o volume de dados.
 */
@Repository
public interface AtendimentoHistoricoRepository extends JpaRepository<AtendimentoHistoricoModel, Long> {

    boolean existsByAgendamentoId(Long agendamentoId);

    long countByClienteId(Long clienteId);

    @Query("""
            SELECT h FROM AtendimentoHistoricoModel h
            WHERE h.dataHoraAtendimento >= :inicio AND h.dataHoraAtendimento < :fim
              AND (:barbeiroId IS NULL OR h.barbeiroId = :barbeiroId)
              AND (:servicoId IS NULL OR h.servicoId = :servicoId)
            ORDER BY h.dataHoraAtendimento DESC
            """)
    Page<AtendimentoHistoricoModel> buscarPorPeriodo(
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim,
            @Param("barbeiroId") Long barbeiroId,
            @Param("servicoId") Long servicoId,
            Pageable pageable);

    /** [formaPagamento, quantidade, totalPago, totalComissao] */
    @Query("""
            SELECT h.formaPagamento, COUNT(h), SUM(h.valorPago), SUM(h.valorComissao)
            FROM AtendimentoHistoricoModel h
            WHERE h.dataHoraAtendimento >= :inicio AND h.dataHoraAtendimento < :fim
            GROUP BY h.formaPagamento
            """)
    List<Object[]> totalizarPorFormaPagamento(@Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);

    /** [barbeiroId, barbeiroNome, quantidade, totalPago, totalComissao] */
    @Query("""
            SELECT h.barbeiroId, MAX(h.barbeiroNome), COUNT(h), SUM(h.valorPago), SUM(h.valorComissao)
            FROM AtendimentoHistoricoModel h
            WHERE h.dataHoraAtendimento >= :inicio AND h.dataHoraAtendimento < :fim
            GROUP BY h.barbeiroId
            ORDER BY SUM(h.valorPago) DESC
            """)
    List<Object[]> totalizarPorBarbeiro(@Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);

    /** [servicoId, servicoNome, quantidade, totalPago] */
    @Query("""
            SELECT h.servicoId, MAX(h.servicoNome), COUNT(h), SUM(h.valorPago)
            FROM AtendimentoHistoricoModel h
            WHERE h.dataHoraAtendimento >= :inicio AND h.dataHoraAtendimento < :fim
            GROUP BY h.servicoId
            ORDER BY COUNT(h) DESC
            """)
    List<Object[]> totalizarPorServico(@Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);

    /** [barbeiroId, servicoId, servicoNome, quantidade, totalPago, totalComissao] */
    @Query("""
            SELECT h.barbeiroId, h.servicoId, MAX(h.servicoNome), COUNT(h), SUM(h.valorPago), SUM(h.valorComissao)
            FROM AtendimentoHistoricoModel h
            WHERE h.dataHoraAtendimento >= :inicio AND h.dataHoraAtendimento < :fim
            GROUP BY h.barbeiroId, h.servicoId
            """)
    List<Object[]> totalizarPorBarbeiroEServico(@Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);

    /** [dataHoraAtendimento, valorPago] — projeção enxuta para o caixa diário. */
    @Query("""
            SELECT h.dataHoraAtendimento, h.valorPago
            FROM AtendimentoHistoricoModel h
            WHERE h.dataHoraAtendimento >= :inicio AND h.dataHoraAtendimento < :fim
            """)
    List<Object[]> listarValoresPorData(@Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);
}
