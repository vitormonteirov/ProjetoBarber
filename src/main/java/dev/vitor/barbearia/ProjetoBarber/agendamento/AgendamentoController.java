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

    @GetMapping
    public ResponseEntity<Page<Agendamento>> listar(@PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(agendamentoService.listarTodos(pageable));
    }

    @PostMapping
    public ResponseEntity<Agendamento> agendar(@RequestBody @Valid AgendamentoDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(agendamentoService.agendar(dto));
    }

    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<Agendamento> cancelar(@PathVariable Long id) {
        return ResponseEntity.ok(agendamentoService.cancelar(id));
    }
}
