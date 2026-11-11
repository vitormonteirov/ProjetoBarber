package dev.vitor.barbearia.ProjetoBarber.security;

import dev.vitor.barbearia.ProjetoBarber.agendamento.AgendamentoModel;
import dev.vitor.barbearia.ProjetoBarber.agendamento.AgendamentoRepository;
import dev.vitor.barbearia.ProjetoBarber.agendamento.AgendamentoService;
import dev.vitor.barbearia.ProjetoBarber.agendamento.StatusAgendamento;
import dev.vitor.barbearia.ProjetoBarber.barbeiro.BarbeiroDTO;
import dev.vitor.barbearia.ProjetoBarber.barbeiro.BarbeiroModel;
import dev.vitor.barbearia.ProjetoBarber.barbeiro.BarbeiroService;
import dev.vitor.barbearia.ProjetoBarber.cliente.ClienteModel;
import dev.vitor.barbearia.ProjetoBarber.cliente.ClienteRepository;
import dev.vitor.barbearia.ProjetoBarber.usuario.Role;
import dev.vitor.barbearia.ProjetoBarber.usuario.UsuarioModel;
import dev.vitor.barbearia.ProjetoBarber.usuario.UsuarioRepository;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@SpringBootTest
class SecurityHardeningIntegrationTest {

    @Autowired
    private RateLimitingFilter rateLimitingFilter;

    @Autowired
    private SecurityFilter securityFilter;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AgendamentoService agendamentoService;

    @Autowired
    private AgendamentoRepository agendamentoRepository;

    @Autowired
    private BarbeiroService barbeiroService;

    @Autowired
    private ClienteRepository clienteRepository;

    @Value("${spring.h2.console.enabled:false}")
    private boolean h2ConsoleEnabled;

    @BeforeEach
    void setUp() {
        rateLimitingFilter.resetBuckets();
    }

    @Test
    @DisplayName("Console H2 do banco de dados deve estar desabilitado por padrão por segurança")
    void h2ConsoleDeveEstarDesabilitadoPorPadrao() throws Exception {
        assertFalse(h2ConsoleEnabled, "O Console H2 não deve estar aberto por padrão!");

        var props = new java.util.Properties();
        try (var in = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            assertNotNull(in);
            props.load(in);
        }
        assertEquals("${H2_CONSOLE_ENABLED:false}", props.getProperty("spring.h2.console.enabled"));
    }

    @Test
    @DisplayName("RateLimitingFilter deve bloquear com HTTP 429 após exceder o limite de tentativas em /api/auth/login")
    void deveBloquearCom429QuandoExcederLimiteAuth() throws Exception {
        FilterChain chain = mock(FilterChain.class);
        String attackerIp = "203.0.113.77";

        for (int i = 1; i <= 15; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/auth/login");
            req.setRemoteAddr(attackerIp);
            MockHttpServletResponse res = new MockHttpServletResponse();
            rateLimitingFilter.doFilter(req, res, chain);
            assertEquals(200, res.getStatus(), "Requisição #" + i + " deveria ser permitida");
        }

        // 16ª tentativa dentro do mesmo minuto deve retornar 429 Too Many Requests
        MockHttpServletRequest blockedReq = new MockHttpServletRequest("POST", "/api/auth/login");
        blockedReq.setRemoteAddr(attackerIp);
        MockHttpServletResponse blockedRes = new MockHttpServletResponse();
        rateLimitingFilter.doFilter(blockedReq, blockedRes, chain);

        assertEquals(429, blockedRes.getStatus());
        assertNotNull(blockedRes.getHeader("Retry-After"));
        assertTrue(blockedRes.getContentAsString().contains("Muitas tentativas de autenticação"));
    }

    @Test
    @DisplayName("Proteção IDOR: um cliente não pode cancelar o agendamento pertencente a outro cliente")
    void clienteNaoPodeCancelarAgendamentoDeOutroCliente() {
        ClienteModel dono = clienteRepository.save(ClienteModel.builder()
                .nome("Dono Real")
                .email("dono.idor@barber.com")
                .telefone("11977776666")
                .ativo(true)
                .build());

        BarbeiroModel barbeiro = barbeiroService.criar(
                new BarbeiroDTO("Barbeiro Security", "barbeiro.sec@barber.com", "11966665555"));

        AgendamentoModel agendamento = agendamentoRepository.save(AgendamentoModel.builder()
                .cliente(dono)
                .barbeiro(barbeiro)
                .dataHora(LocalDateTime.now().plusDays(2))
                .dataHoraFim(LocalDateTime.now().plusDays(2).plusMinutes(30))
                .status(StatusAgendamento.AGENDADO)
                .build());

        // Outro cliente tenta cancelar passando seu próprio e-mail na sessão
        assertThrows(AccessDeniedException.class, () ->
                agendamentoService.cancelar(agendamento.getId(), "invasor@barber.com", false));

        // O dono real consegue cancelar normalmente
        AgendamentoModel cancelado = agendamentoService.cancelar(
                agendamento.getId(), "dono.idor@barber.com", false);
        assertEquals(StatusAgendamento.CANCELADO, cancelado.getStatus());
    }

    @Test
    @DisplayName("Proteção BOLA no Agendamento: um cliente não pode criar agendamento passando clienteId de outro cliente")
    void clienteNaoPodeAgendarEmNomeDeOutroCliente() {
        ClienteModel vitima = clienteRepository.save(ClienteModel.builder()
                .nome("Vitima BOLA")
                .email("vitima.bola@barber.com")
                .telefone("11911112222")
                .ativo(true)
                .build());

        clienteRepository.save(ClienteModel.builder()
                .nome("Atacante BOLA")
                .email("atacante.bola@barber.com")
                .telefone("11933334444")
                .ativo(true)
                .build());

        BarbeiroModel barbeiro = barbeiroService.criar(
                new BarbeiroDTO("Barbeiro BOLA", "barbeiro.bola@barber.com", "11966667777"));

        var dtoMalicioso = new dev.vitor.barbearia.ProjetoBarber.agendamento.AgendamentoDTO(
                vitima.getId(),
                barbeiro.getId(),
                1L,
                LocalDateTime.now().plusDays(3).withHour(10).withMinute(0));

        assertThrows(AccessDeniedException.class, () ->
                agendamentoService.agendar(dtoMalicioso, "atacante.bola@barber.com", false));
    }

    @Test
    @DisplayName("SecurityFilter deve bloquear imediatamente com HTTP 401 um token JWT válido cujo barbeiro/usuário foi desativado")
    void securityFilterDeveBloquearImediatamenteUsuarioOuBarbeiroDesativado() throws Exception {
        String emailBarbeiro = "barbeiro.revogado@barber.com";

        BarbeiroModel barbeiro = barbeiroService.criar(
                new BarbeiroDTO("Barbeiro Revogado", emailBarbeiro, "11955554444"));

        UsuarioModel usuario = usuarioRepository.save(UsuarioModel.builder()
                .nome("Barbeiro Revogado")
                .email(emailBarbeiro)
                .senha(passwordEncoder.encode("SenhaForte@123"))
                .role(Role.ROLE_BARBEIRO)
                .ativo(true)
                .build());

        // Emite um JWT válido enquanto o barbeiro está ativo
        String jwtValido = tokenService.gerarToken(usuario);

        // Desativa o barbeiro via BarbeiroService (que também desativa o UsuarioModel vinculado)
        barbeiroService.excluir(barbeiro.getId());

        UsuarioModel usuarioPosExclusao = usuarioRepository.findByEmail(emailBarbeiro).orElseThrow();
        assertFalse(usuarioPosExclusao.getAtivo(), "O UsuarioModel vinculado ao barbeiro deveria ter sido desativado");

        // Tenta usar o JWT emitido anteriormente em uma requisição autenticada
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/agendamentos/barbeiro/" + barbeiro.getId());
        req.addHeader("Authorization", "Bearer " + jwtValido);
        MockHttpServletResponse res = new MockHttpServletResponse();

        securityFilter.doFilter(req, res, chain);

        assertEquals(401, res.getStatus(), "Token de usuário desativado deve ser rejeitado imediatamente com HTTP 401");
        assertTrue(res.getContentAsString().contains("Sua conta foi desativada ou revogada"));
        verify(chain, never()).doFilter(req, res);
    }

    @Test
    @DisplayName("SEC-02: RateLimitingFilter não deve permitir bypass via falsificação de header X-Forwarded-For")
    void rateLimitingFilterNaoDevePermitirBypassViaXForwardedForSpoofing() throws Exception {
        FilterChain chain = mock(FilterChain.class);
        String realAttackerIp = "198.51.100.42";

        for (int i = 1; i <= 15; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/auth/login");
            req.setRemoteAddr(realAttackerIp);
            req.addHeader("X-Forwarded-For", "10.0.0." + i);
            MockHttpServletResponse res = new MockHttpServletResponse();
            rateLimitingFilter.doFilter(req, res, chain);
            assertEquals(200, res.getStatus());
        }

        // 16ª tentativa com novo X-Forwarded-For forjado ainda deve ser bloqueada pelo IP real da conexão
        MockHttpServletRequest spoofedReq = new MockHttpServletRequest("POST", "/api/auth/login");
        spoofedReq.setRemoteAddr(realAttackerIp);
        spoofedReq.addHeader("X-Forwarded-For", "10.0.0.250");
        MockHttpServletResponse spoofedRes = new MockHttpServletResponse();
        rateLimitingFilter.doFilter(spoofedReq, spoofedRes, chain);

        assertEquals(429, spoofedRes.getStatus(), "Falsificação de X-Forwarded-For não deve burlar o Rate Limit!");
    }

    @Test
    @DisplayName("SEC-04: Deve impedir que um cliente atualize seu e-mail para o e-mail já pertencente a outro cliente")
    void deveImpedirColisaoDeEmailEntreClientes(@Autowired dev.vitor.barbearia.ProjetoBarber.cliente.ClienteService clienteService) {
        clienteRepository.save(ClienteModel.builder()
                .nome("Cliente Existente")
                .email("existente.sec04@barber.com")
                .telefone("11911110000")
                .ativo(true)
                .build());

        ClienteModel outroCliente = clienteRepository.save(ClienteModel.builder()
                .nome("Outro Cliente")
                .email("outro.sec04@barber.com")
                .telefone("11922220000")
                .ativo(true)
                .build());

        var dtoColisao = new dev.vitor.barbearia.ProjetoBarber.cliente.ClienteDTO(
                "Outro Cliente", "11922220000", "existente.sec04@barber.com");

        assertThrows(dev.vitor.barbearia.ProjetoBarber.exceptions.BusinessRuleException.class, () ->
                clienteService.atualizar(outroCliente.getId(), dtoColisao));
    }

    @Test
    @DisplayName("OBS-01: CorrelationIdFilter deve gerar e propagar cabeçalho X-Correlation-Id em cada requisição")
    void correlationIdFilterDeveGerarEPropagarHeader(@Autowired dev.vitor.barbearia.ProjetoBarber.config.CorrelationIdFilter correlationIdFilter) throws Exception {
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/servicos");
        MockHttpServletResponse res = new MockHttpServletResponse();

        correlationIdFilter.doFilter(req, res, chain);

        String generatedId = res.getHeader(dev.vitor.barbearia.ProjetoBarber.config.CorrelationIdFilter.CORRELATION_ID_HEADER);
        assertNotNull(generatedId, "X-Correlation-Id deve ser gerado automaticamente quando ausente");
        assertFalse(generatedId.isBlank());
    }

    @Test
    @DisplayName("Fase 3 Auth: Deve renovar token JWT de usuário ativo e permitir alteração segura de senha")
    void deveRenovarSessaoEAlterarSenhaComSucesso(@Autowired dev.vitor.barbearia.ProjetoBarber.auth.AuthService authService) {
        String email = "fase3.auth@barber.com";
        usuarioRepository.save(UsuarioModel.builder()
                .nome("Usuario Fase 3")
                .email(email)
                .senha(passwordEncoder.encode("SenhaAntiga123"))
                .role(Role.ROLE_CLIENTE)
                .ativo(true)
                .build());

        var renovado = authService.renovarSessao(email);
        assertNotNull(renovado.token());
        assertEquals(email, tokenService.validarToken(renovado.token()));

        // Tentativa com senha atual errada deve falhar
        assertThrows(dev.vitor.barbearia.ProjetoBarber.exceptions.BusinessRuleException.class, () ->
                authService.alterarSenha(email, new dev.vitor.barbearia.ProjetoBarber.auth.AlterarSenhaDTO("SenhaErrada", "NovaSenhaForte123")));

        // Alteração com senha atual correta deve atualizar o hash BCrypt
        authService.alterarSenha(email, new dev.vitor.barbearia.ProjetoBarber.auth.AlterarSenhaDTO("SenhaAntiga123", "NovaSenhaForte123"));
        UsuarioModel atualizado = usuarioRepository.findByEmail(email).orElseThrow();
        assertTrue(passwordEncoder.matches("NovaSenhaForte123", atualizado.getSenha()));
    }
}
