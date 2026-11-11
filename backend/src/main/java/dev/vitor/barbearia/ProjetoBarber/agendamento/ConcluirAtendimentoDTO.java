package dev.vitor.barbearia.ProjetoBarber.agendamento;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Dados informados ao concluir um atendimento.
 *
 * @param valorPago      valor efetivamente pago pelo cliente. Se nulo, assume o preço registrado no agendamento.
 * @param formaPagamento forma de pagamento utilizada (obrigatória).
 */
public record ConcluirAtendimentoDTO(
        @DecimalMin(value = "0.00", message = "O valor pago não pode ser negativo.")
        @Digits(integer = 8, fraction = 2, message = "O valor pago deve ter no máximo 2 casas decimais.")
        BigDecimal valorPago,

        @NotNull(message = "A forma de pagamento é obrigatória.")
        FormaPagamento formaPagamento
) {}
