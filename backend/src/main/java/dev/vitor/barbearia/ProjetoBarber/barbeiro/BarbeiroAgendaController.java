package dev.vitor.barbearia.ProjetoBarber.barbeiro;

import dev.vitor.barbearia.ProjetoBarber.agendamento.AgendamentoMapper;
import dev.vitor.barbearia.ProjetoBarber.agendamento.AgendamentoModel;
import dev.vitor.barbearia.ProjetoBarber.agendamento.AgendamentoResponseDTO;
import dev.vitor.barbearia.ProjetoBarber.agendamento.AgendamentoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/barbeiro")
@RequiredArgsConstructor
@PreAuthorize("hasRole('BARBEIRO')")
public class BarbeiroAgendaController {

    private final AgendamentoService agendamentoService;
    private final AgendamentoMapper agendamentoMapper;

    @GetMapping("/minha-agenda")
    public ResponseEntity<Page<AgendamentoResponseDTO>> minhaAgenda(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<AgendamentoModel> agendamentos = agendamentoService.listarPorBarbeiroEmail(userDetails.getUsername(), pageable);
        return ResponseEntity.ok(agendamentos.map(agendamentoMapper::toResponseDTO));
    }

    @PatchMapping("/agendamentos/{id}/alterar-horario")
    public ResponseEntity<AgendamentoResponseDTO> alterarHorario(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid AlterarHorarioDTO dto) {
        AgendamentoModel atualizado = agendamentoService.alterarHorarioPeloBarbeiro(id, userDetails.getUsername(), dto);
        return ResponseEntity.ok(agendamentoMapper.toResponseDTO(atualizado));
    }

    @PatchMapping("/agendamentos/{id}/cancelar")
    public ResponseEntity<AgendamentoResponseDTO> cancelarAgendamento(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid CancelarAgendamentoBarbeiroDTO dto) {
        AgendamentoModel cancelado = agendamentoService.cancelarPeloBarbeiro(id, userDetails.getUsername(), dto);
        return ResponseEntity.ok(agendamentoMapper.toResponseDTO(cancelado));
    }
}
