package dev.vitor.barbearia.ProjetoBarber.gestor;

import dev.vitor.barbearia.ProjetoBarber.barbeiro.BarbeiroDTO;
import dev.vitor.barbearia.ProjetoBarber.barbeiro.BarbeiroModel;
import dev.vitor.barbearia.ProjetoBarber.barbeiro.BarbeiroResponseDTO;
import dev.vitor.barbearia.ProjetoBarber.barbeiro.BarbeiroService;
import dev.vitor.barbearia.ProjetoBarber.exceptions.BusinessRuleException;
import dev.vitor.barbearia.ProjetoBarber.usuario.Role;
import dev.vitor.barbearia.ProjetoBarber.usuario.UsuarioModel;
import dev.vitor.barbearia.ProjetoBarber.usuario.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GestorBarbeiroService {

    private final UsuarioRepository usuarioRepository;
    private final BarbeiroService barbeiroService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public BarbeiroResponseDTO cadastrarBarbeiroComAcesso(CriarBarbeiroUserDTO dto) {
        if (usuarioRepository.existsByEmail(dto.email())) {
            throw new BusinessRuleException("Já existe um usuário cadastrado com este e-mail.");
        }

        // 1. Cria a conta de acesso com ROLE_BARBEIRO (atribuída única e exclusivamente pelo Gestor)
        UsuarioModel usuario = UsuarioModel.builder()
                .nome(dto.nome())
                .email(dto.email())
                .senha(passwordEncoder.encode(dto.senha()))
                .role(Role.ROLE_BARBEIRO)
                .ativo(true)
                .build();
        usuarioRepository.save(usuario);

        // 2. Cria a entidade de domínio BarbeiroModel
        BarbeiroModel barbeiro = barbeiroService.criar(new BarbeiroDTO(dto.nome(), dto.email(), dto.telefone()));

        return new BarbeiroResponseDTO(barbeiro.getId(), barbeiro.getNome(), barbeiro.getEmail(), barbeiro.getTelefone(), barbeiro.getAtivo());
    }
}
