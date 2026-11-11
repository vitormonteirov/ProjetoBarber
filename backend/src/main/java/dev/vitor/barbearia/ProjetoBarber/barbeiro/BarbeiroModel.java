package dev.vitor.barbearia.ProjetoBarber.barbeiro;

import dev.vitor.barbearia.ProjetoBarber.agendamento.AgendamentoModel;
import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

@Entity
@Table(name = "barbeiro")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BarbeiroModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nome;
    private String email;
    private String telefone;

    @Builder.Default
    @Column(nullable = false)
    private Boolean ativo = true;

    /** Percentual de comissão do barbeiro (0 a 100) definido pelo Gestor. */
    @Builder.Default
    @Column(name = "percentual_comissao", nullable = false, precision = 5, scale = 2)
    private BigDecimal percentualComissao = PERCENTUAL_COMISSAO_PADRAO;

    /** Horário inicial de expediente do barbeiro (padrão 08:00). */
    @Builder.Default
    @Column(name = "horario_inicio_expediente", nullable = false)
    private LocalTime horarioInicioExpediente = EXPEDIENTE_INICIO_PADRAO;

    /** Horário final de expediente do barbeiro (padrão 19:00). */
    @Builder.Default
    @Column(name = "horario_fim_expediente", nullable = false)
    private LocalTime horarioFimExpediente = EXPEDIENTE_FIM_PADRAO;

    /** Início do intervalo de pausa/almoço (padrão 12:00, configurável pelo Gestor). */
    @Builder.Default
    @Column(name = "horario_inicio_pausa")
    private LocalTime horarioInicioPausa = PAUSA_INICIO_PADRAO;

    /** Fim do intervalo de pausa/almoço (padrão 13:00, configurável pelo Gestor). */
    @Builder.Default
    @Column(name = "horario_fim_pausa")
    private LocalTime horarioFimPausa = PAUSA_FIM_PADRAO;

    @OneToMany(mappedBy = "barbeiro", cascade = CascadeType.ALL)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnore
    private List<AgendamentoModel> agendamentos;

    public static final BigDecimal PERCENTUAL_COMISSAO_PADRAO = new BigDecimal("50.00");
    public static final LocalTime EXPEDIENTE_INICIO_PADRAO = LocalTime.of(8, 0);
    public static final LocalTime EXPEDIENTE_FIM_PADRAO = LocalTime.of(19, 0);
    public static final LocalTime PAUSA_INICIO_PADRAO = LocalTime.of(12, 0);
    public static final LocalTime PAUSA_FIM_PADRAO = LocalTime.of(13, 0);

    @PrePersist
    void aplicarPadroes() {
        if (ativo == null) ativo = true;
        if (percentualComissao == null) percentualComissao = PERCENTUAL_COMISSAO_PADRAO;
        if (horarioInicioExpediente == null) horarioInicioExpediente = EXPEDIENTE_INICIO_PADRAO;
        if (horarioFimExpediente == null) horarioFimExpediente = EXPEDIENTE_FIM_PADRAO;
        if (horarioInicioPausa == null && horarioFimPausa == null) {
            horarioInicioPausa = PAUSA_INICIO_PADRAO;
            horarioFimPausa = PAUSA_FIM_PADRAO;
        }
    }
}
