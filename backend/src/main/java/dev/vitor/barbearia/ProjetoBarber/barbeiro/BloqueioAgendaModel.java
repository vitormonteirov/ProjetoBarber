package dev.vitor.barbearia.ProjetoBarber.barbeiro;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "bloqueio_agenda")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BloqueioAgendaModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "barbeiro_id", nullable = false)
    private BarbeiroModel barbeiro;

    @Column(name = "data_hora_inicio", nullable = false)
    private LocalDateTime dataHoraInicio;

    @Column(name = "data_hora_fim", nullable = false)
    private LocalDateTime dataHoraFim;

    @Column(nullable = false, length = 255)
    private String motivo;

    @Column(name = "criado_por", length = 150)
    private String criadoPor;
}
