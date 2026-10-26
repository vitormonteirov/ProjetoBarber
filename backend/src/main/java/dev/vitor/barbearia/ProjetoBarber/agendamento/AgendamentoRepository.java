package dev.vitor.barbearia.ProjetoBarber.agendamento;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AgendamentoRepository extends JpaRepository<AgendamentoModel, Long> {

    boolean existsByBarbeiroIdAndDataHoraAndStatus(Long barbeiroId, LocalDateTime dataHora, StatusAgendamento status);

    @Query("SELECT COUNT(a) > 0 FROM AgendamentoModel a WHERE a.barbeiro.id = :barbeiroId " +
           "AND a.status = :status " +
           "AND (:ignorarId IS NULL OR a.id != :ignorarId) " +
           "AND ((a.dataHora < :dataHoraFim AND COALESCE(a.dataHoraFim, a.dataHora) > :dataHoraInicio))")
    boolean existsConflitoHorarioBarbeiroIgnorandoId(
            @Param("barbeiroId") Long barbeiroId,
            @Param("dataHoraInicio") LocalDateTime dataHoraInicio,
            @Param("dataHoraFim") LocalDateTime dataHoraFim,
            @Param("status") StatusAgendamento status,
            @Param("ignorarId") Long ignorarId);

    default boolean existsConflitoHorarioBarbeiro(Long barbeiroId, LocalDateTime dataHoraInicio, LocalDateTime dataHoraFim, StatusAgendamento status) {
        return existsConflitoHorarioBarbeiroIgnorandoId(barbeiroId, dataHoraInicio, dataHoraFim, status, null);
    }

    @Query("SELECT COUNT(a) > 0 FROM AgendamentoModel a WHERE a.cliente.id = :clienteId " +
           "AND a.barbeiro.id = :barbeiroId " +
           "AND a.dataHora >= :inicioDia AND a.dataHora <= :fimDia " +
           "AND a.status = :status")
    boolean existsByClienteIdAndBarbeiroIdAndDiaLimit(
            @Param("clienteId") Long clienteId, 
            @Param("barbeiroId") Long barbeiroId,
            @Param("inicioDia") LocalDateTime inicioDia, 
            @Param("fimDia") LocalDateTime fimDia, 
            @Param("status") StatusAgendamento status);

    Page<AgendamentoModel> findByBarbeiroEmailOrderByDataHoraAsc(String email, Pageable pageable);

    Page<AgendamentoModel> findByClienteEmailOrderByDataHoraDesc(String email, Pageable pageable);

    List<AgendamentoModel> findByBarbeiroIdAndDataHoraBetweenAndStatus(Long barbeiroId, LocalDateTime inicio, LocalDateTime fim, StatusAgendamento status);

    List<AgendamentoModel> findByDataHoraBetween(LocalDateTime inicio, LocalDateTime fim);

    List<AgendamentoModel> findByStatus(StatusAgendamento status);
}
