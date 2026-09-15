package dev.vitor.barbearia.ProjetoBarber.repositories;

import dev.vitor.barbearia.ProjetoBarber.models.Agendamento;
import dev.vitor.barbearia.ProjetoBarber.models.StatusAgendamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {

    boolean existsByBarbeiroIdAndDataHoraAndStatus(Long barbeiroId, LocalDateTime dataHora, StatusAgendamento status);

    @Query("SELECT COUNT(a) > 0 FROM Agendamento a WHERE a.cliente.id = :clienteId " +
           "AND a.dataHora >= :inicioDia AND a.dataHora <= :fimDia " +
           "AND a.status = :status")
    boolean existsByClienteIdAndDiaLimit(
            @Param("clienteId") Long clienteId, 
            @Param("inicioDia") LocalDateTime inicioDia, 
            @Param("fimDia") LocalDateTime fimDia, 
            @Param("status") StatusAgendamento status);
}
