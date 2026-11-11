package dev.vitor.barbearia.ProjetoBarber.historico;

import dev.vitor.barbearia.ProjetoBarber.agendamento.FormaPagamento;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Registro histórico (snapshot imutável) de um atendimento concluído.
 *
 * <p>Os dados de barbeiro, serviço e cliente são <b>copiados</b> no momento da conclusão e
 * os IDs são guardados sem chave estrangeira. Assim, mesmo que o barbeiro ou o serviço sejam
 * renomeados, desativados ou excluídos depois, o histórico financeiro permanece intacto.</p>
 */
@Entity
@Immutable
@Table(
        name = "atendimento_historico",
        uniqueConstraints = @UniqueConstraint(name = "uk_historico_agendamento", columnNames = "agendamento_id"),
        indexes = {
                @Index(name = "idx_historico_data", columnList = "data_hora_atendimento"),
                @Index(name = "idx_historico_barbeiro", columnList = "barbeiro_id, data_hora_atendimento")
        }
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AtendimentoHistoricoModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "agendamento_id", nullable = false, updatable = false)
    private Long agendamentoId;

    @Column(name = "barbeiro_id", nullable = false, updatable = false)
    private Long barbeiroId;

    @Column(name = "barbeiro_nome", nullable = false, updatable = false)
    private String barbeiroNome;

    @Column(name = "servico_id", updatable = false)
    private Long servicoId;

    @Column(name = "servico_nome", nullable = false, updatable = false)
    private String servicoNome;

    @Column(name = "cliente_id", updatable = false)
    private Long clienteId;

    @Column(name = "cliente_nome", updatable = false)
    private String clienteNome;

    @Column(name = "cliente_nao_usuario", nullable = false, updatable = false)
    private Boolean clienteNaoUsuario;

    @Column(name = "data_hora_atendimento", nullable = false, updatable = false)
    private LocalDateTime dataHoraAtendimento;

    @Column(name = "data_hora_conclusao", nullable = false, updatable = false)
    private LocalDateTime dataHoraConclusao;

    /** Preço de tabela do serviço no momento do atendimento. */
    @Column(name = "valor_tabela", precision = 10, scale = 2, updatable = false)
    private BigDecimal valorTabela;

    /** Valor efetivamente pago (entra no caixa). */
    @Column(name = "valor_pago", nullable = false, precision = 10, scale = 2, updatable = false)
    private BigDecimal valorPago;

    @Enumerated(EnumType.STRING)
    @Column(name = "forma_pagamento", nullable = false, length = 30, updatable = false)
    private FormaPagamento formaPagamento;

    /** Percentual de comissão vigente para o barbeiro no momento da conclusão. */
    @Column(name = "percentual_comissao", nullable = false, precision = 5, scale = 2, updatable = false)
    private BigDecimal percentualComissao;

    @Column(name = "valor_comissao", nullable = false, precision = 10, scale = 2, updatable = false)
    private BigDecimal valorComissao;

    /** E-mail de quem registrou a conclusão (auditoria). */
    @Column(name = "concluido_por", nullable = false, updatable = false)
    private String concluidoPor;
}
