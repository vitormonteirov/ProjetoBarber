package dev.vitor.barbearia.ProjetoBarber.controllers;

import dev.vitor.barbearia.ProjetoBarber.dtos.AgendamentoDTO;
import dev.vitor.barbearia.ProjetoBarber.models.Agendamento;
import dev.vitor.barbearia.ProjetoBarber.services.AgendamentoService;
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
public class AgendamentoController {

    @Autowired
    private AgendamentoService agendamentoService;

    @GetMapping
    public ResponseEntity<Page<Agendamento>> listar(@PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(agendamentoService.listarTodos(pageable));
    }

    @PostMapping
    public ResponseEntity<Agendamento> agendar(@RequestBody @Valid AgendamentoDTO dto) {
        Agendamento agendamento = agendamentoService.agendar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(agendamento);
    }

    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<Agendamento> cancelar(@PathVariable Long id) {
        Agendamento cancelado = agendamentoService.cancelar(id);
        return ResponseEntity.ok(cancelado);
    }
}
