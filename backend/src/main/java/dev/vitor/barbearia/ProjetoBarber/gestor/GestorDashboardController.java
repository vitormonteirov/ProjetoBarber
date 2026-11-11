package dev.vitor.barbearia.ProjetoBarber.gestor;

import dev.vitor.barbearia.ProjetoBarber.barbeiro.BarbeiroResponseDTO;
import dev.vitor.barbearia.ProjetoBarber.historico.AtendimentoHistoricoResponseDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
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

    @GetMapping("/relatorio")
    public ResponseEntity<RelatorioPeriodoDTO> obterRelatorioPeriodo(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim) {
        return ResponseEntity.ok(dashboardService.obterRelatorioPeriodo(dataInicio, dataFim));
    }

    @GetMapping("/historico")
    public ResponseEntity<Page<AtendimentoHistoricoResponseDTO>> listarHistorico(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(required = false) Long barbeiroId,
            @RequestParam(required = false) Long servicoId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(dashboardService.listarHistorico(dataInicio, dataFim, barbeiroId, servicoId, pageable));
    }

    @GetMapping("/comissoes/{barbeiroId}")
    public ResponseEntity<CalculoComissaoBarbeiroDTO> calcularComissaoBarbeiro(
            @PathVariable Long barbeiroId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(required = false) BigDecimal percentual) {
        return ResponseEntity.ok(dashboardService.calcularComissaoBarbeiro(barbeiroId, dataInicio, dataFim, percentual));
    }

    @PostMapping("/barbeiros")
    public ResponseEntity<BarbeiroResponseDTO> cadastrarBarbeiroComAcesso(@RequestBody @Valid CriarBarbeiroUserDTO dto) {
        BarbeiroResponseDTO response = gestorBarbeiroService.cadastrarBarbeiroComAcesso(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/barbeiros/{id}/comissao")
    public ResponseEntity<BarbeiroResponseDTO> atualizarComissaoBarbeiro(
            @PathVariable Long id,
            @RequestBody @Valid AtualizarComissaoBarbeiroDTO dto) {
        return ResponseEntity.ok(gestorBarbeiroService.atualizarComissao(id, dto));
    }
}
