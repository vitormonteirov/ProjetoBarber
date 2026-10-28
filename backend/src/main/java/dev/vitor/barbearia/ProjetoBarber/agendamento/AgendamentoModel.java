package dev.vitor.barbearia.ProjetoBarber.agendamento;

import dev.vitor.barbearia.ProjetoBarber.cliente.ClienteModel;
import dev.vitor.barbearia.ProjetoBarber.barbeiro.BarbeiroModel;
import dev.vitor.barbearia.ProjetoBarber.servico.ServicoModel;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "agendamento")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgendamentoModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = true)
    private ClienteModel cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "barbeiro_id", nullable = false)
    private BarbeiroModel barbeiro;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "servico_id")
    private ServicoModel servico;

    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora;

    @Column(name = "data_hora_fim")
    private LocalDateTime dataHoraFim;

    @Column(precision = 10, scale = 2)
    private BigDecimal preco;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusAgendamento status;

    @Column(name = "nota_alteracao", length = 500)
    private String notaAlteracao;

    @Column(name = "nome_nao_usuario", length = 255)
    private String nomeNaoUsuario;

    @Column(name = "telefone_nao_usuario", length = 50)
    private String telefoneNaoUsuario;
}
