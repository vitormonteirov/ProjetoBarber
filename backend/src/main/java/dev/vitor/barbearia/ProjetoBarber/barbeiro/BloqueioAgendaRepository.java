package dev.vitor.barbearia.ProjetoBarber.barbeiro;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BloqueioAgendaRepository extends JpaRepository<BloqueioAgendaModel, Long> {

    @Query("""
            SELECT COUNT(b) > 0 FROM BloqueioAgendaModel b
            WHERE b.barbeiro.id = :barbeiroId
              AND b.dataHoraInicio < :fim
              AND b.dataHoraFim > :inicio
            """)
    boolean existsConflitoBloqueio(
            @Param("barbeiroId") Long barbeiroId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim);

    @Query("""
            SELECT b FROM BloqueioAgendaModel b
            WHERE b.barbeiro.id = :barbeiroId
              AND b.dataHoraInicio < :fimDia
              AND b.dataHoraFim > :inicioDia
            ORDER BY b.dataHoraInicio ASC
            """)
    List<BloqueioAgendaModel> findByBarbeiroIdNoPeriodo(
            @Param("barbeiroId") Long barbeiroId,
            @Param("inicioDia") LocalDateTime inicioDia,
            @Param("fimDia") LocalDateTime fimDia);

    List<BloqueioAgendaModel> findByBarbeiroEmailAndDataHoraFimGreaterThanEqualOrderByDataHoraInicioAsc(
            String email, LocalDateTime aPartirDe);

    List<BloqueioAgendaModel> findByDataHoraFimGreaterThanEqualOrderByDataHoraInicioAsc(LocalDateTime aPartirDe);
}
