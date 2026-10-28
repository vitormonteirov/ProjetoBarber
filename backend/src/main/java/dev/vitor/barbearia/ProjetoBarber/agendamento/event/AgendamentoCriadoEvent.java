package dev.vitor.barbearia.ProjetoBarber.agendamento.event;

import dev.vitor.barbearia.ProjetoBarber.agendamento.AgendamentoModel;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
public class AgendamentoCriadoEvent {
    private final Long agendamentoId;
    private final Long clienteId;
    private final String clienteNome;
    private final String clienteEmail;
    private final Long barbeiroId;
    private final String barbeiroNome;
    private final String servicoNome;
    private final LocalDateTime dataHora;
    private final LocalDateTime dataHoraFim;
    private final BigDecimal preco;
    private final boolean naoUsuario;

    public AgendamentoCriadoEvent(AgendamentoModel model) {
        this.agendamentoId = model.getId();
        this.clienteId = model.getCliente() != null ? model.getCliente().getId() : null;
        this.clienteNome = model.getCliente() != null ? model.getCliente().getNome() : model.getNomeNaoUsuario();
        this.clienteEmail = model.getCliente() != null ? model.getCliente().getEmail() : null;
        this.barbeiroId = model.getBarbeiro().getId();
        this.barbeiroNome = model.getBarbeiro().getNome();
        this.servicoNome = model.getServico() != null ? model.getServico().getNome() : null;
        this.dataHora = model.getDataHora();
        this.dataHoraFim = model.getDataHoraFim();
        this.preco = model.getPreco();
        this.naoUsuario = model.getCliente() == null;
    }
}
