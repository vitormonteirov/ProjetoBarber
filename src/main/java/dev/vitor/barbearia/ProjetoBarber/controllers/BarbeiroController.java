package dev.vitor.barbearia.ProjetoBarber.controllers;

import dev.vitor.barbearia.ProjetoBarber.dtos.BarbeiroDTO;
import dev.vitor.barbearia.ProjetoBarber.models.Barbeiro;
import dev.vitor.barbearia.ProjetoBarber.services.BarbeiroService;
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
public class BarbeiroController {

    @Autowired
    private BarbeiroService barbeiroService;

    @GetMapping
    public ResponseEntity<Page<Barbeiro>> listar(@PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(barbeiroService.listarTodos(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Barbeiro> buscarPorId(@PathVariable Long id) {
        return barbeiroService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Barbeiro> criar(@RequestBody @Valid BarbeiroDTO dto) {
        Barbeiro barbeiro = barbeiroService.criar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(barbeiro);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Barbeiro> atualizar(@PathVariable Long id, @RequestBody @Valid BarbeiroDTO dto) {
        Barbeiro barbeiro = barbeiroService.atualizar(id, dto);
        return ResponseEntity.ok(barbeiro);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        barbeiroService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
