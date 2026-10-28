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
public class BarbeiroAgendaController {

    private final AgendamentoService agendamentoService;
    private final AgendamentoMapper agendamentoMapper;

    @GetMapping("/minha-agenda")
    @PreAuthorize("hasRole('BARBEIRO')")
    public ResponseEntity<Page<AgendamentoResponseDTO>> minhaAgenda(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<AgendamentoModel> agendamentos = agendamentoService.listarPorBarbeiroEmail(userDetails.getUsername(), pageable);
        return ResponseEntity.ok(agendamentos.map(agendamentoMapper::toResponseDTO));
    }

    @PatchMapping("/agendamentos/{id}/alterar-horario")
    @PreAuthorize("hasAnyRole('BARBEIRO', 'GESTOR')")
    public ResponseEntity<AgendamentoResponseDTO> alterarHorario(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid AlterarHorarioDTO dto) {
        boolean isGestor = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_GESTOR"));

        AgendamentoModel atualizado = agendamentoService.alterarHorarioPeloBarbeiroOuGestor(
                id, userDetails.getUsername(), isGestor, dto);
        return ResponseEntity.ok(agendamentoMapper.toResponseDTO(atualizado));
    }

    @PatchMapping("/agendamentos/{id}/cancelar")
    @PreAuthorize("hasAnyRole('BARBEIRO', 'GESTOR')")
    public ResponseEntity<AgendamentoResponseDTO> cancelarAgendamento(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid CancelarAgendamentoBarbeiroDTO dto) {
        boolean isGestor = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_GESTOR"));

        AgendamentoModel cancelado = agendamentoService.cancelarPeloBarbeiroOuGestor(
                id, userDetails.getUsername(), isGestor, dto);
        return ResponseEntity.ok(agendamentoMapper.toResponseDTO(cancelado));
    }
}
