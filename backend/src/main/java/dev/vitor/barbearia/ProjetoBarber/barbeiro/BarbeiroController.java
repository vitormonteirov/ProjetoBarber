package dev.vitor.barbearia.ProjetoBarber.barbeiro;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/barbeiros")
@RequiredArgsConstructor
public class BarbeiroController {

    private final BarbeiroService barbeiroService;
    private final BarbeiroMapper barbeiroMapper;

    @GetMapping
    public ResponseEntity<Page<BarbeiroResponseDTO>> listar(
            @RequestParam(defaultValue = "true") boolean apenasAtivos,
            @PageableDefault(size = 10) Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails) {
        boolean isGestor = userDetails != null && userDetails.getAuthorities().stream()
                .anyMatch(a -> "ROLE_GESTOR".equals(a.getAuthority()));
        boolean filtrarAtivos = apenasAtivos || !isGestor;
        Page<BarbeiroModel> barbeiros = filtrarAtivos
                ? barbeiroService.listarAtivos(pageable)
                : barbeiroService.listarTodos(pageable);
        return ResponseEntity.ok(barbeiros.map(barbeiroMapper::toResponseDTO));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BarbeiroResponseDTO> buscarPorId(@PathVariable Long id) {
        return barbeiroService.buscarPorId(id)
                .map(barbeiroMapper::toResponseDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('GESTOR')")
    public ResponseEntity<BarbeiroResponseDTO> criar(@RequestBody @Valid BarbeiroDTO dto) {
        BarbeiroModel model = barbeiroService.criar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(barbeiroMapper.toResponseDTO(model));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('GESTOR')")
    public ResponseEntity<BarbeiroResponseDTO> atualizar(@PathVariable Long id, @RequestBody @Valid BarbeiroDTO dto) {
        BarbeiroModel model = barbeiroService.atualizar(id, dto);
        return ResponseEntity.ok(barbeiroMapper.toResponseDTO(model));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('GESTOR')")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        barbeiroService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
