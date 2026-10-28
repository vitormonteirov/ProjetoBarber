package dev.vitor.barbearia.ProjetoBarber.auth;

import dev.vitor.barbearia.ProjetoBarber.gestor.CriarBarbeiroUserDTO;
import dev.vitor.barbearia.ProjetoBarber.gestor.GestorBarbeiroService;
import dev.vitor.barbearia.ProjetoBarber.usuario.Role;
import dev.vitor.barbearia.ProjetoBarber.usuario.UsuarioModel;
import dev.vitor.barbearia.ProjetoBarber.usuario.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class SecurityRbacIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private GestorBarbeiroService gestorBarbeiroService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        usuarioRepository.deleteAll();
    }

    @Test
    @DisplayName("Todo usuário que criar conta no sistema deve ser CLIENTE por padrão")
    void deveRegistrarUsuarioComoClientePorPadrao() {
        RegistroDTO registro = new RegistroDTO(
                "Cliente Teste",
                "cliente.teste@barber.com",
                "senha123",
                "11987654321"
        );

        LoginResponseDTO responseRegistro = authService.registrar(registro);

        assertNotNull(responseRegistro.token());
        assertEquals(Role.ROLE_CLIENTE, responseRegistro.role());
        assertEquals("cliente.teste@barber.com", responseRegistro.email());

        LoginDTO login = new LoginDTO("cliente.teste@barber.com", "senha123");
        LoginResponseDTO responseLogin = authService.login(login);

        assertNotNull(responseLogin.token());
        assertEquals(Role.ROLE_CLIENTE, responseLogin.role());
    }

    @Test
    @DisplayName("Role BARBEIRO deve ser criada única e exclusivamente pelo Gestor")
    void gestorDeveCriarUsuarioComRoleBarbeiro() {
        CriarBarbeiroUserDTO dto = new CriarBarbeiroUserDTO(
                "Carlos Barbeiro",
                "carlos@barber.com",
                "barber123",
                "11977776666"
        );

        var response = gestorBarbeiroService.cadastrarBarbeiroComAcesso(dto);

        assertNotNull(response.id());
        assertEquals("Carlos Barbeiro", response.nome());

        UsuarioModel usuarioSalvo = usuarioRepository.findByEmail("carlos@barber.com").orElseThrow();
        assertEquals(Role.ROLE_BARBEIRO, usuarioSalvo.getRole());

        // Valida que o barbeiro consegue fazer login com as credenciais criadas pelo gestor
        LoginDTO login = new LoginDTO("carlos@barber.com", "barber123");
        LoginResponseDTO loginResponse = authService.login(login);
        assertEquals(Role.ROLE_BARBEIRO, loginResponse.role());
    }

    @Test
    @DisplayName("Deve validar hashing de senha BCrypt para usuários")
    void deveValidarHashingBCrypt() {
        RegistroDTO registro = new RegistroDTO(
                "Usuario Seguro",
                "seguro@barber.com",
                "senhaForte456",
                "11999998888"
        );

        authService.registrar(registro);

        UsuarioModel usuarioSalvo = usuarioRepository.findByEmail("seguro@barber.com").orElseThrow();
        assertNotEquals("senhaForte456", usuarioSalvo.getSenha());
        assertTrue(passwordEncoder.matches("senhaForte456", usuarioSalvo.getSenha()));
    }
}
