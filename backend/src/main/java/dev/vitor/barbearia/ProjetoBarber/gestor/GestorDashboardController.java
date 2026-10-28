package dev.vitor.barbearia.ProjetoBarber.gestor;

import dev.vitor.barbearia.ProjetoBarber.barbeiro.BarbeiroResponseDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/gestor")
@RequiredArgsConstructor
@PreAuthorize("hasRole('GESTOR')")
public class GestorDashboardController {

    private final GestorDashboardService dashboardService;
    private final GestorBarbeiroService gestorBarbeiroService;

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardResponseDTO> obterDashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fim) {
        DashboardResponseDTO response = dashboardService.obterMetricas(inicio, fim);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/barbeiros")
    public ResponseEntity<BarbeiroResponseDTO> cadastrarBarbeiroComAcesso(@RequestBody @Valid CriarBarbeiroUserDTO dto) {
        BarbeiroResponseDTO response = gestorBarbeiroService.cadastrarBarbeiroComAcesso(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
