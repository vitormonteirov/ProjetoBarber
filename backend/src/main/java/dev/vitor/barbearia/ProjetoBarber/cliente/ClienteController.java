package dev.vitor.barbearia.ProjetoBarber.cliente;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
public class ClienteController {

    private final ClienteService clienteService;
    private final ClienteMapper clienteMapper;

    @GetMapping
    @PreAuthorize("hasRole('GESTOR')")
    public ResponseEntity<Page<ClienteResponseDTO>> listar(
            @RequestParam(defaultValue = "true") boolean apenasAtivos,
            @PageableDefault(size = 10) Pageable pageable) {
        Page<ClienteModel> clientes = apenasAtivos
                ? clienteService.listarAtivos(pageable)
                : clienteService.listarTodos(pageable);
        return ResponseEntity.ok(clientes.map(clienteMapper::toResponseDTO));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('GESTOR', 'CLIENTE')")
    public ResponseEntity<ClienteResponseDTO> buscarPorId(@PathVariable Long id) {
        return clienteService.buscarPorId(id)
                .map(clienteMapper::toResponseDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('GESTOR')")
    public ResponseEntity<ClienteResponseDTO> criar(@RequestBody @Valid ClienteDTO dto) {
        ClienteModel model = clienteService.criar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(clienteMapper.toResponseDTO(model));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('GESTOR', 'CLIENTE')")
    public ResponseEntity<ClienteResponseDTO> atualizar(@PathVariable Long id, @RequestBody @Valid ClienteDTO dto) {
        ClienteModel model = clienteService.atualizar(id, dto);
        return ResponseEntity.ok(clienteMapper.toResponseDTO(model));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('GESTOR')")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        clienteService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
