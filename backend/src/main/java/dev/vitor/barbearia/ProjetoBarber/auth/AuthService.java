package dev.vitor.barbearia.ProjetoBarber.auth;

import dev.vitor.barbearia.ProjetoBarber.cliente.ClienteDTO;
import dev.vitor.barbearia.ProjetoBarber.cliente.ClienteService;
import dev.vitor.barbearia.ProjetoBarber.exceptions.BusinessRuleException;
import dev.vitor.barbearia.ProjetoBarber.security.TokenService;
import dev.vitor.barbearia.ProjetoBarber.usuario.Role;
import dev.vitor.barbearia.ProjetoBarber.usuario.UsuarioModel;
import dev.vitor.barbearia.ProjetoBarber.usuario.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final ClienteService clienteService;

    @Transactional
    public LoginResponseDTO registrar(RegistroDTO dto) {
        if (usuarioRepository.existsByEmail(dto.email())) {
            throw new BusinessRuleException("Já existe um usuário cadastrado com este e-mail.");
        }

        // Regra de Segurança: Todo usuário que se cadastra publicamente é estritamente ROLE_CLIENTE por padrão
        UsuarioModel usuario = UsuarioModel.builder()
                .nome(dto.nome())
                .email(dto.email())
                .senha(passwordEncoder.encode(dto.senha()))
                .role(Role.ROLE_CLIENTE)
                .ativo(true)
                .build();

        UsuarioModel salvo = usuarioRepository.save(usuario);

        // Sincroniza entidade ClienteModel
        String telefone = dto.telefone() != null ? dto.telefone() : "11999999999";
        clienteService.criar(new ClienteDTO(dto.nome(), telefone, dto.email()));

        String token = tokenService.gerarToken(salvo);
        return new LoginResponseDTO(token, salvo.getId(), salvo.getNome(), salvo.getEmail(), salvo.getRole());
    }

    public LoginResponseDTO login(LoginDTO dto) {
        var authToken = new UsernamePasswordAuthenticationToken(dto.email(), dto.senha());
        var authentication = authenticationManager.authenticate(authToken);

        UsuarioModel usuario = (UsuarioModel) authentication.getPrincipal();
        String token = tokenService.gerarToken(usuario);

        return new LoginResponseDTO(token, usuario.getId(), usuario.getNome(), usuario.getEmail(), usuario.getRole());
    }

    @Transactional(readOnly = true)
    public LoginResponseDTO renovarSessao(String email) {
        UsuarioModel usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new dev.vitor.barbearia.ProjetoBarber.exceptions.ResourceNotFoundException("Usuário não encontrado."));
        if (Boolean.FALSE.equals(usuario.getAtivo())) {
            throw new org.springframework.security.access.AccessDeniedException("Sua conta está desativada.");
        }
        String novoToken = tokenService.gerarToken(usuario);
        return new LoginResponseDTO(novoToken, usuario.getId(), usuario.getNome(), usuario.getEmail(), usuario.getRole());
    }

    @Transactional
    public void alterarSenha(String email, AlterarSenhaDTO dto) {
        UsuarioModel usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new dev.vitor.barbearia.ProjetoBarber.exceptions.ResourceNotFoundException("Usuário não encontrado."));
        if (!passwordEncoder.matches(dto.senhaAtual(), usuario.getSenha())) {
            throw new BusinessRuleException("A senha atual informada está incorreta.");
        }
        usuario.setSenha(passwordEncoder.encode(dto.novaSenha()));
        usuarioRepository.save(usuario);
    }
}
