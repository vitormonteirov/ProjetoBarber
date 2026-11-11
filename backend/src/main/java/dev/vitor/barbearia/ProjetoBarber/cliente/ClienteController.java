package dev.vitor.barbearia.ProjetoBarber.cliente;

import dev.vitor.barbearia.ProjetoBarber.agendamento.AgendamentoMapper;
import dev.vitor.barbearia.ProjetoBarber.agendamento.AgendamentoRepository;
import dev.vitor.barbearia.ProjetoBarber.agendamento.StatusAgendamento;
import dev.vitor.barbearia.ProjetoBarber.historico.AtendimentoHistoricoRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
public class ClienteController {

    private final ClienteService clienteService;
    private final ClienteMapper clienteMapper;
    private final ClienteRepository clienteRepository;
    private final AtendimentoHistoricoRepository historicoRepository;
    private final AgendamentoRepository agendamentoRepository;

    @GetMapping("/me/fidelidade")
    @PreAuthorize("hasAnyRole('CLIENTE', 'GESTOR')")
    public ResponseEntity<FidelidadeClienteDTO> minhaFidelidade(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            throw new AccessDeniedException("Usuário não autenticado.");
        }
        ClienteModel cliente = clienteRepository.findByEmail(userDetails.getUsername()).orElse(null);
        if (cliente == null) {
            return ResponseEntity.ok(new FidelidadeClienteDTO(
                    null, userDetails.getUsername(), 0L, AgendamentoMapper.META_SELOS_FIDELIDADE, 0, AgendamentoMapper.META_SELOS_FIDELIDADE, 0L, false, 0L));
        }

        int meta = AgendamentoMapper.META_SELOS_FIDELIDADE;
        long concluidos = historicoRepository.countByClienteId(cliente.getId());
        long faltas = agendamentoRepository.countByClienteIdAndStatus(cliente.getId(), StatusAgendamento.NAO_COMPARECEU);
        int selosNoCiclo = (int) (concluidos % meta);
        int faltam = selosNoCiclo == 0 && concluidos > 0 ? 0 : (meta - selosNoCiclo);
        long recompensas = concluidos / meta;
        boolean proximaComRecompensa = concluidos > 0 && (selosNoCiclo == (meta - 1) || selosNoCiclo == 0);

        return ResponseEntity.ok(new FidelidadeClienteDTO(
                cliente.getId(),
                cliente.getNome(),
                concluidos,
                meta,
                selosNoCiclo,
                faltam,
                recompensas,
                proximaComRecompensa,
                faltas
        ));
    }

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
    public ResponseEntity<ClienteResponseDTO> buscarPorId(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return clienteService.buscarPorId(id)
                .map(cliente -> {
                    validarTitularidadeOuGestor(cliente, userDetails);
                    return clienteMapper.toResponseDTO(cliente);
                })
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
    public ResponseEntity<ClienteResponseDTO> atualizar(
            @PathVariable Long id,
            @RequestBody @Valid ClienteDTO dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        clienteService.buscarPorId(id)
                .ifPresent(cliente -> validarTitularidadeOuGestor(cliente, userDetails));
        ClienteModel model = clienteService.atualizar(id, dto);
        return ResponseEntity.ok(clienteMapper.toResponseDTO(model));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('GESTOR')")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        clienteService.excluir(id);
        return ResponseEntity.noContent().build();
    }

    private void validarTitularidadeOuGestor(ClienteModel cliente, UserDetails userDetails) {
        if (userDetails == null) {
            throw new AccessDeniedException("Usuário não autenticado.");
        }
        boolean isGestor = userDetails.getAuthorities().stream()
                .anyMatch(a -> "ROLE_GESTOR".equals(a.getAuthority()));
        if (!isGestor && (cliente.getEmail() == null || !cliente.getEmail().equalsIgnoreCase(userDetails.getUsername()))) {
            throw new AccessDeniedException("Acesso negado: você só pode consultar ou alterar os seus próprios dados cadastrais.");
        }
    }
}
