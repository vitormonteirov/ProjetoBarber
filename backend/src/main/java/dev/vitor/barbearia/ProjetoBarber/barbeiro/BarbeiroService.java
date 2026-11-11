package dev.vitor.barbearia.ProjetoBarber.barbeiro;

import dev.vitor.barbearia.ProjetoBarber.exceptions.ResourceNotFoundException;
import dev.vitor.barbearia.ProjetoBarber.usuario.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BarbeiroService {

    private final BarbeiroRepository barbeiroRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional(readOnly = true)
    public Page<BarbeiroModel> listarAtivos(Pageable pageable) {
        return barbeiroRepository.findByAtivoTrue(pageable);
    }

    @Transactional(readOnly = true)
    public Page<BarbeiroModel> listarTodos(Pageable pageable) {
        return barbeiroRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Optional<BarbeiroModel> buscarPorId(Long id) {
        return barbeiroRepository.findById(id);
    }

    @Transactional
    public Optional<BarbeiroModel> buscarPorIdComLock(Long id) {
        return barbeiroRepository.findByIdWithLock(id);
    }

    @Transactional
    public BarbeiroModel criar(BarbeiroDTO dto) {
        BarbeiroModel barbeiro = BarbeiroModel.builder()
                .nome(dto.nome())
                .email(dto.email())
                .telefone(dto.telefone())
                .ativo(true)
                .build();
        return barbeiroRepository.save(barbeiro);
    }

    @Transactional
    public BarbeiroModel atualizar(Long id, BarbeiroDTO dto) {
        return barbeiroRepository.findById(id).map(barbeiro -> {
            barbeiro.setNome(dto.nome());
            barbeiro.setEmail(dto.email());
            barbeiro.setTelefone(dto.telefone());
            return barbeiroRepository.save(barbeiro);
        }).orElseThrow(() -> new ResourceNotFoundException("Barbeiro não encontrado com ID: " + id));
    }

    @Transactional
    public void excluir(Long id) {
        BarbeiroModel barbeiro = barbeiroRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Barbeiro não encontrado com ID: " + id));
        barbeiro.setAtivo(false);
        barbeiroRepository.save(barbeiro);

        if (barbeiro.getEmail() != null) {
            usuarioRepository.findByEmail(barbeiro.getEmail()).ifPresent(usuario -> {
                usuario.setAtivo(false);
                usuarioRepository.save(usuario);
            });
        }
    }

    @Transactional
    public BarbeiroModel atualizarPercentualComissao(Long id, java.math.BigDecimal percentualComissao) {
        BarbeiroModel barbeiro = barbeiroRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Barbeiro não encontrado com ID: " + id));
        barbeiro.setPercentualComissao(percentualComissao.setScale(2, java.math.RoundingMode.HALF_UP));
        return barbeiroRepository.save(barbeiro);
    }
}
