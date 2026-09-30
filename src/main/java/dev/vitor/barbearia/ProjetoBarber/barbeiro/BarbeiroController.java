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

    @Autowired
    private BarbeiroMapper barbeiroMapper;

    @GetMapping
    public ResponseEntity<Page<BarbeiroResponseDTO>> listar(@PageableDefault(size = 10) Pageable pageable) {
        Page<BarbeiroModel> barbeiros = barbeiroService.listarTodos(pageable);
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
    public ResponseEntity<BarbeiroResponseDTO> criar(@RequestBody @Valid BarbeiroDTO dto) {
        BarbeiroModel model = barbeiroService.criar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(barbeiroMapper.toResponseDTO(model));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BarbeiroResponseDTO> atualizar(@PathVariable Long id, @RequestBody @Valid BarbeiroDTO dto) {
        BarbeiroModel model = barbeiroService.atualizar(id, dto);
        return ResponseEntity.ok(barbeiroMapper.toResponseDTO(model));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        barbeiroService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
