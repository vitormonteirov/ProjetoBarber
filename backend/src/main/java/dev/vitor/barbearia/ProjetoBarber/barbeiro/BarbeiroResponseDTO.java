package dev.vitor.barbearia.ProjetoBarber.barbeiro;

import java.math.BigDecimal;
import java.time.LocalTime;

public record BarbeiroResponseDTO(
        Long id,
        String nome,
        String email,
        String telefone,
        Boolean ativo,
        BigDecimal percentualComissao,
        LocalTime horarioInicioExpediente,
        LocalTime horarioFimExpediente,
        LocalTime horarioInicioPausa,
        LocalTime horarioFimPausa
) {
    public BarbeiroResponseDTO(Long id, String nome, String email, String telefone, Boolean ativo, BigDecimal percentualComissao) {
        this(
                id,
                nome,
                email,
                telefone,
                ativo,
                percentualComissao,
                BarbeiroModel.EXPEDIENTE_INICIO_PADRAO,
                BarbeiroModel.EXPEDIENTE_FIM_PADRAO,
                null,
                null
        );
    }

    public BarbeiroResponseDTO(Long id, String nome, String email, String telefone, Boolean ativo) {
        this(id, nome, email, telefone, ativo, BarbeiroModel.PERCENTUAL_COMISSAO_PADRAO);
    }

    public BarbeiroResponseDTO(Long id, String nome, String email, String telefone) {
        this(id, nome, email, telefone, true, BarbeiroModel.PERCENTUAL_COMISSAO_PADRAO);
    }
}
