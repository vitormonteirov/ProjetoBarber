package dev.vitor.barbearia.ProjetoBarber.gestor;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AtualizarComissaoBarbeiroDTO(
        @NotNull(message = "O percentual de comissão é obrigatório.")
        @DecimalMin(value = "0.00", message = "O percentual de comissão não pode ser menor que 0%.")
        @DecimalMax(value = "100.00", message = "O percentual de comissão não pode ultrapassar 100%.")
        @Digits(integer = 3, fraction = 2, message = "O percentual deve ter no máximo 2 casas decimais.")
        BigDecimal percentualComissao
) {}
