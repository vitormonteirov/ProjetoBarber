package dev.vitor.barbearia.ProjetoBarber.auth;

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
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        usuarioRepository.deleteAll();
    }

    @Test
    @DisplayName("Deve registrar cliente e permitir login retornando Bearer JWT com role correta")
    void deveRegistrarELogarComSucesso() {
        RegistroDTO registro = new RegistroDTO(
                "Cliente Teste",
                "cliente.teste@barber.com",
                "senha123",
                "11987654321",
                Role.ROLE_CLIENTE
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
    @DisplayName("Deve registrar gestor com sucesso e validar hashing de senha BCrypt")
    void deveRegistrarGestorEValidarBCrypt() {
        RegistroDTO registro = new RegistroDTO(
                "Gestor Master",
                "gestor@barber.com",
                "senhaForte456",
                "11999998888",
                Role.ROLE_GESTOR
        );

        LoginResponseDTO response = authService.registrar(registro);

        assertNotNull(response.token());
        assertEquals(Role.ROLE_GESTOR, response.role());

        UsuarioModel usuarioSalvo = usuarioRepository.findByEmail("gestor@barber.com").orElseThrow();
        assertNotEquals("senhaForte456", usuarioSalvo.getSenha());
        assertTrue(passwordEncoder.matches("senhaForte456", usuarioSalvo.getSenha()));
    }

    @Test
    @DisplayName("Deve registrar barbeiro com sucesso e associar perfil ROLE_BARBEIRO")
    void deveRegistrarBarbeiro() {
        RegistroDTO registro = new RegistroDTO(
                "Carlos Barbeiro",
                "carlos@barber.com",
                "barber123",
                "11977776666",
                Role.ROLE_BARBEIRO
        );

        LoginResponseDTO response = authService.registrar(registro);

        assertNotNull(response.token());
        assertEquals(Role.ROLE_BARBEIRO, response.role());
    }
}
