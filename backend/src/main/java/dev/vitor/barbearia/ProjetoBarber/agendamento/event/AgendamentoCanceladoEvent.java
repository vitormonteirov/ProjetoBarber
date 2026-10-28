package dev.vitor.barbearia.ProjetoBarber.agendamento.event;

import dev.vitor.barbearia.ProjetoBarber.agendamento.AgendamentoModel;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class AgendamentoCanceladoEvent {
    private final Long agendamentoId;
    private final String clienteNome;
    private final String clienteEmail;
    private final String barbeiroNome;
    private final LocalDateTime dataHora;
    private final String notaAlteracao;

    public AgendamentoCanceladoEvent(AgendamentoModel model) {
        this.agendamentoId = model.getId();
        this.clienteNome = model.getCliente() != null ? model.getCliente().getNome() : model.getNomeNaoUsuario();
        this.clienteEmail = model.getCliente() != null ? model.getCliente().getEmail() : null;
        this.barbeiroNome = model.getBarbeiro().getNome();
        this.dataHora = model.getDataHora();
        this.notaAlteracao = model.getNotaAlteracao();
    }
}
