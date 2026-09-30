package dev.vitor.barbearia.ProjetoBarber.agendamento;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
interface AgendamentoRepository extends JpaRepository<AgendamentoModel, Long> {

    boolean existsByBarbeiroIdAndDataHoraAndStatus(Long barbeiroId, LocalDateTime dataHora, StatusAgendamento status);

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
}
