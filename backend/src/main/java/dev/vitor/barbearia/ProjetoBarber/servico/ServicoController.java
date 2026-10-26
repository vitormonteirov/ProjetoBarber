package dev.vitor.barbearia.ProjetoBarber.servico;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/servicos")
@RequiredArgsConstructor
public class ServicoController {

    private final ServicoService servicoService;
    private final ServicoMapper servicoMapper;

    @GetMapping
    public ResponseEntity<Page<ServicoResponseDTO>> listar(
            @RequestParam(defaultValue = "true") boolean apenasAtivos,
            @PageableDefault(size = 10) Pageable pageable) {
        Page<ServicoModel> servicos = apenasAtivos
                ? servicoService.listarAtivos(pageable)
                : servicoService.listarTodos(pageable);
        return ResponseEntity.ok(servicos.map(servicoMapper::toResponseDTO));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServicoResponseDTO> buscarPorId(@PathVariable Long id) {
        return servicoService.buscarPorId(id)
                .map(servicoMapper::toResponseDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ServicoResponseDTO> criar(@RequestBody @Valid ServicoDTO dto) {
        ServicoModel model = servicoService.criar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(servicoMapper.toResponseDTO(model));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ServicoResponseDTO> atualizar(@PathVariable Long id, @RequestBody @Valid ServicoDTO dto) {
        ServicoModel model = servicoService.atualizar(id, dto);
        return ResponseEntity.ok(servicoMapper.toResponseDTO(model));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        servicoService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
