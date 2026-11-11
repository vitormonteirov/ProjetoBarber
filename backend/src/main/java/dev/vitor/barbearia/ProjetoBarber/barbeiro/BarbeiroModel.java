package dev.vitor.barbearia.ProjetoBarber.barbeiro;

import dev.vitor.barbearia.ProjetoBarber.agendamento.AgendamentoModel;
import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.math.BigDecimal;
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

    @OneToMany(mappedBy = "barbeiro", cascade = CascadeType.ALL)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnore
    private List<AgendamentoModel> agendamentos;

    public static final BigDecimal PERCENTUAL_COMISSAO_PADRAO = new BigDecimal("50.00");

    @PrePersist
    void aplicarPadroes() {
        if (ativo == null) ativo = true;
        if (percentualComissao == null) percentualComissao = PERCENTUAL_COMISSAO_PADRAO;
    }
}
