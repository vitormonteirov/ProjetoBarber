package dev.vitor.barbearia.ProjetoBarber.gestor;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/gestor")
@RequiredArgsConstructor
@PreAuthorize("hasRole('GESTOR')")
public class GestorDashboardController {

    private final GestorDashboardService dashboardService;

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardResponseDTO> obterDashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fim) {
        DashboardResponseDTO response = dashboardService.obterMetricas(inicio, fim);
        return ResponseEntity.ok(response);
    }
}
