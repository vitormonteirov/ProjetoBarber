package dev.vitor.barbearia.ProjetoBarber.agendamento.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
public class AgendamentoEventListener {

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAgendamentoCriado(AgendamentoCriadoEvent event) {
        log.info("[ASYNC EVENT] Agendamento #{} criado com sucesso para {} com barbeiro {} em {}. Processando pós-venda/notificações.",
                event.getAgendamentoId(), event.getClienteNome(), event.getBarbeiroNome(), event.getDataHora());
        // Desacoplamento assíncrono pronto para gateways de email, SMS ou WhatsApp
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAgendamentoCancelado(AgendamentoCanceladoEvent event) {
        log.info("[ASYNC EVENT] Agendamento #{} cancelado para {}. Barbeiro: {}. Motivo/Nota: {}.",
                event.getAgendamentoId(), event.getClienteNome(), event.getBarbeiroNome(), event.getNotaAlteracao());
        // Desacoplamento assíncrono pronto para disparos de cancelamento
    }
}
