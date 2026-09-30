package dev.vitor.barbearia.ProjetoBarber.agendamento;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/agendamentos")
class AgendamentoController {

    @Autowired
    private AgendamentoService agendamentoService;
    
    @Autowired
    private AgendamentoMapper agendamentoMapper;

    @GetMapping
    public ResponseEntity<Page<AgendamentoResponseDTO>> listar(@PageableDefault(size = 10) Pageable pageable) {
        Page<AgendamentoModel> agendamentos = agendamentoService.listarTodos(pageable);
        return ResponseEntity.ok(agendamentos.map(agendamentoMapper::toResponseDTO));
    }

    @PostMapping
    public ResponseEntity<AgendamentoResponseDTO> agendar(@RequestBody @Valid AgendamentoDTO dto) {
        AgendamentoModel model = agendamentoService.agendar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(agendamentoMapper.toResponseDTO(model));
    }

    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<AgendamentoResponseDTO> cancelar(@PathVariable Long id) {
        AgendamentoModel model = agendamentoService.cancelar(id);
        return ResponseEntity.ok(agendamentoMapper.toResponseDTO(model));
    }
}
