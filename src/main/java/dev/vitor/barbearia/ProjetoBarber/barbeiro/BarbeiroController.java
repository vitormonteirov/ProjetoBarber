package dev.vitor.barbearia.ProjetoBarber.barbeiro;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/barbeiros")
class BarbeiroController {

    @Autowired
    private BarbeiroService barbeiroService;

    @GetMapping
    public ResponseEntity<Page<Barbeiro>> listar(@PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(barbeiroService.listarTodos(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Barbeiro> buscarPorId(@PathVariable Long id) {
        return barbeiroService.buscarPorId(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Barbeiro> criar(@RequestBody @Valid BarbeiroDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(barbeiroService.criar(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Barbeiro> atualizar(@PathVariable Long id, @RequestBody @Valid BarbeiroDTO dto) {
        return ResponseEntity.ok(barbeiroService.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        barbeiroService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
