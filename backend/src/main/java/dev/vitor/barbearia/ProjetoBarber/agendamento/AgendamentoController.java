package dev.vitor.barbearia.ProjetoBarber.agendamento;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/agendamentos")
@RequiredArgsConstructor
public class AgendamentoController {

    private final AgendamentoService agendamentoService;
    private final AgendamentoMapper agendamentoMapper;

    @GetMapping
    @PreAuthorize("hasRole('GESTOR')")
    public ResponseEntity<Page<AgendamentoResponseDTO>> listar(@PageableDefault(size = 10) Pageable pageable) {
        Page<AgendamentoModel> agendamentos = agendamentoService.listarTodos(pageable);
        return ResponseEntity.ok(agendamentos.map(agendamentoMapper::toResponseDTO));
    }

    @GetMapping("/meus-agendamentos")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<Page<AgendamentoResponseDTO>> meusAgendamentos(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 10) Pageable pageable) {
        Page<AgendamentoModel> agendamentos = agendamentoService.listarPorClienteEmail(userDetails.getUsername(), pageable);
        return ResponseEntity.ok(agendamentos.map(agendamentoMapper::toResponseDTO));
    }

    @GetMapping("/disponibilidade")
    public ResponseEntity<List<LocalTime>> consultarDisponibilidade(
            @RequestParam Long barbeiroId,
            @RequestParam(required = false) Long servicoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        List<LocalTime> horarios = agendamentoService.calcularHorariosDisponiveis(barbeiroId, servicoId, data);
        return ResponseEntity.ok(horarios);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('CLIENTE', 'GESTOR')")
    public ResponseEntity<AgendamentoResponseDTO> agendar(@RequestBody @Valid AgendamentoDTO dto) {
        AgendamentoModel model = agendamentoService.agendar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(agendamentoMapper.toResponseDTO(model));
    }

    @PatchMapping("/{id}/cancelar")
    @PreAuthorize("hasAnyRole('CLIENTE', 'GESTOR')")
    public ResponseEntity<AgendamentoResponseDTO> cancelar(@PathVariable Long id) {
        AgendamentoModel model = agendamentoService.cancelar(id);
        return ResponseEntity.ok(agendamentoMapper.toResponseDTO(model));
    }
}
