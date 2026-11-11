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

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@SpringBootTest
class SecurityHardeningIntegrationTest {

    @Autowired
    private RateLimitingFilter rateLimitingFilter;

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
}
