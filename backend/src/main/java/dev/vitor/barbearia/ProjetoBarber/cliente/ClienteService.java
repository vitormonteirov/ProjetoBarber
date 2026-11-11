package dev.vitor.barbearia.ProjetoBarber.cliente;

import dev.vitor.barbearia.ProjetoBarber.agendamento.AgendamentoMapper;
import dev.vitor.barbearia.ProjetoBarber.agendamento.AgendamentoRepository;
import dev.vitor.barbearia.ProjetoBarber.agendamento.StatusAgendamento;
import dev.vitor.barbearia.ProjetoBarber.exceptions.ResourceNotFoundException;
import dev.vitor.barbearia.ProjetoBarber.historico.AtendimentoHistoricoRepository;
import dev.vitor.barbearia.ProjetoBarber.usuario.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final AtendimentoHistoricoRepository historicoRepository;
    private final AgendamentoRepository agendamentoRepository;

    @Transactional(readOnly = true)
    public Page<ClienteModel> listarAtivos(Pageable pageable) {
        return clienteRepository.findByAtivoTrue(pageable);
    }

    @Transactional(readOnly = true)
    public Page<ClienteModel> listarTodos(Pageable pageable) {
        return clienteRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Optional<ClienteModel> buscarPorId(Long id) {
        return clienteRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<ClienteModel> buscarPorEmail(String email) {
        return clienteRepository.findByEmail(email);
    }

    @Transactional(readOnly = true)
    public FidelidadeClienteDTO obterFidelidadePorEmail(String email) {
        ClienteModel cliente = clienteRepository.findByEmail(email).orElse(null);
        int meta = AgendamentoMapper.META_SELOS_FIDELIDADE;
        if (cliente == null) {
            return new FidelidadeClienteDTO(null, email, 0L, meta, 0, meta, 0L, false, 0L);
        }

        long concluidos = historicoRepository.countByClienteId(cliente.getId());
        long faltas = agendamentoRepository.countByClienteIdAndStatus(cliente.getId(), StatusAgendamento.NAO_COMPARECEU);
        int selosNoCiclo = (int) (concluidos % meta);
        int faltam = selosNoCiclo == 0 && concluidos > 0 ? 0 : (meta - selosNoCiclo);
        long recompensas = concluidos / meta;
        boolean proximaComRecompensa = concluidos > 0 && (selosNoCiclo == (meta - 1) || selosNoCiclo == 0);

        return new FidelidadeClienteDTO(
                cliente.getId(),
                cliente.getNome(),
                concluidos,
                meta,
                selosNoCiclo,
                faltam,
                recompensas,
                proximaComRecompensa,
                faltas
        );
    }

    @Transactional
    public ClienteModel criar(ClienteDTO dto) {
        if (dto.email() != null && clienteRepository.existsByEmail(dto.email())) {
            throw new dev.vitor.barbearia.ProjetoBarber.exceptions.BusinessRuleException("Já existe um cliente cadastrado com este e-mail.");
        }
        ClienteModel cliente = ClienteModel.builder()
                .nome(dto.nome())
                .telefone(dto.telefone())
                .email(dto.email())
                .ativo(true)
                .build();
        return clienteRepository.save(cliente);
    }

    @Transactional
    public ClienteModel atualizar(Long id, ClienteDTO dto) {
        return clienteRepository.findById(id).map(cliente -> {
            String emailAnterior = cliente.getEmail();
            boolean trocouEmail = emailAnterior == null || !emailAnterior.equalsIgnoreCase(dto.email());
            if (trocouEmail && dto.email() != null) {
                if (clienteRepository.existsByEmail(dto.email()) || usuarioRepository.existsByEmail(dto.email())) {
                    throw new dev.vitor.barbearia.ProjetoBarber.exceptions.BusinessRuleException("Este e-mail já está em uso por outra conta.");
                }
            }

            cliente.setNome(dto.nome());
            cliente.setTelefone(dto.telefone());
            cliente.setEmail(dto.email());
            ClienteModel salvo = clienteRepository.save(cliente);

            if (emailAnterior != null && !emailAnterior.equalsIgnoreCase(dto.email())) {
                usuarioRepository.findByEmail(emailAnterior).ifPresent(usuario -> {
                    usuario.setEmail(dto.email());
                    usuario.setNome(dto.nome());
                    usuarioRepository.save(usuario);
                });
            }
            return salvo;
        }).orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado com ID: " + id));
    }

    @Transactional
    public void excluir(Long id) {
        ClienteModel cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado com ID: " + id));
        cliente.setAtivo(false);
        clienteRepository.save(cliente);

        if (cliente.getEmail() != null) {
            usuarioRepository.findByEmail(cliente.getEmail()).ifPresent(usuario -> {
                usuario.setAtivo(false);
                usuarioRepository.save(usuario);
            });
        }
    }
}

