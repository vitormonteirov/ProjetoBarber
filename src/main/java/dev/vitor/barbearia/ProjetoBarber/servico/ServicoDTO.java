package dev.vitor.barbearia.ProjetoBarber.servico;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record ServicoDTO(
    @NotBlank(message = "O nome do serviço é obrigatório.")
    String nome,

    String descricao,

    @NotNull(message = "O preço do serviço é obrigatório.")
    @Positive(message = "O preço deve ser maior que zero.")
    BigDecimal preco,

    @NotNull(message = "A duração em minutos é obrigatória.")
    @Min(value = 5, message = "A duração mínima do serviço deve ser de 5 minutos.")
    Integer duracaoMinutos
) {}
