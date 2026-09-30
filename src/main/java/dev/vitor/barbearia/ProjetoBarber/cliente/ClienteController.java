package dev.vitor.barbearia.ProjetoBarber.cliente;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/clientes")
class ClienteController {

    @Autowired
    private ClienteService clienteService;

    @Autowired
    private ClienteMapper clienteMapper;

    @GetMapping
    public ResponseEntity<Page<ClienteResponseDTO>> listar(@PageableDefault(size = 10) Pageable pageable) {
        Page<ClienteModel> clientes = clienteService.listarTodos(pageable);
        return ResponseEntity.ok(clientes.map(clienteMapper::toResponseDTO));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClienteResponseDTO> buscarPorId(@PathVariable Long id) {
        return clienteService.buscarPorId(id)
                .map(clienteMapper::toResponseDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ClienteResponseDTO> criar(@RequestBody @Valid ClienteDTO dto) {
        ClienteModel model = clienteService.criar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(clienteMapper.toResponseDTO(model));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClienteResponseDTO> atualizar(@PathVariable Long id, @RequestBody @Valid ClienteDTO dto) {
        ClienteModel model = clienteService.atualizar(id, dto);
        return ResponseEntity.ok(clienteMapper.toResponseDTO(model));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        clienteService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
