package dev.vitor.barbearia.ProjetoBarber.barbeiro;

import dev.vitor.barbearia.ProjetoBarber.exceptions.BusinessRuleException;
import dev.vitor.barbearia.ProjetoBarber.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BloqueioAgendaService {

    private final BloqueioAgendaRepository bloqueioRepository;
    private final BarbeiroRepository barbeiroRepository;

    @Transactional
    public BloqueioAgendaModel criarBloqueio(BloqueioAgendaDTO dto, String emailSolicitante, boolean isGestor) {
        if (dto.dataHoraInicio() == null || dto.dataHoraFim() == null || !dto.dataHoraInicio().isBefore(dto.dataHoraFim())) {
            throw new BusinessRuleException("O início do bloqueio deve ser anterior ao horário de término.");
        }

        BarbeiroModel barbeiro;
        if (isGestor && dto.barbeiroId() != null) {
            barbeiro = barbeiroRepository.findById(dto.barbeiroId())
                    .orElseThrow(() -> new ResourceNotFoundException("Barbeiro não encontrado com ID: " + dto.barbeiroId()));
        } else {
            barbeiro = barbeiroRepository.findByEmail(emailSolicitante)
                    .orElseThrow(() -> new ResourceNotFoundException("Perfil de barbeiro não encontrado para: " + emailSolicitante));
        }

        if (!isGestor && !barbeiro.getEmail().equalsIgnoreCase(emailSolicitante)) {
            throw new BusinessRuleException("Você só pode registrar bloqueios na sua própria agenda.");
        }

        BloqueioAgendaModel bloqueio = BloqueioAgendaModel.builder()
                .barbeiro(barbeiro)
                .dataHoraInicio(dto.dataHoraInicio())
                .dataHoraFim(dto.dataHoraFim())
                .motivo(dto.motivo().trim())
                .criadoPor(emailSolicitante)
                .build();

        return bloqueioRepository.save(bloqueio);
    }

    @Transactional(readOnly = true)
    public List<BloqueioAgendaModel> listarBloqueiosAtivos(String emailSolicitante, boolean isGestor) {
        LocalDateTime inicioHoje = LocalDate.now().atStartOfDay();
        if (isGestor) {
            return bloqueioRepository.findByDataHoraFimGreaterThanEqualOrderByDataHoraInicioAsc(inicioHoje);
        }
        return bloqueioRepository.findByBarbeiroEmailAndDataHoraFimGreaterThanEqualOrderByDataHoraInicioAsc(
                emailSolicitante, inicioHoje);
    }

    @Transactional
    public void removerBloqueio(Long id, String emailSolicitante, boolean isGestor) {
        BloqueioAgendaModel bloqueio = bloqueioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bloqueio de agenda não encontrado com ID: " + id));

        if (!isGestor && !bloqueio.getBarbeiro().getEmail().equalsIgnoreCase(emailSolicitante)) {
            throw new BusinessRuleException("Você só pode remover bloqueios da sua própria agenda.");
        }

        bloqueioRepository.delete(bloqueio);
    }
}
