package dev.vitor.barbearia.ProjetoBarber.security;

import dev.vitor.barbearia.ProjetoBarber.barbeiro.BarbeiroModel;
import dev.vitor.barbearia.ProjetoBarber.barbeiro.BarbeiroRepository;
import dev.vitor.barbearia.ProjetoBarber.cliente.ClienteModel;
import dev.vitor.barbearia.ProjetoBarber.cliente.ClienteRepository;
import dev.vitor.barbearia.ProjetoBarber.usuario.Role;
import dev.vitor.barbearia.ProjetoBarber.usuario.UsuarioModel;
import dev.vitor.barbearia.ProjetoBarber.usuario.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class SecurityFilter extends OncePerRequestFilter {

    private final TokenService tokenService;
    private final UsuarioRepository usuarioRepository;
    private final BarbeiroRepository barbeiroRepository;
    private final ClienteRepository clienteRepository;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String token = recuperarToken(request);

        if (token != null) {
            String email = tokenService.validarToken(token);
            if (email != null) {
                UsuarioModel usuario = usuarioRepository.findByEmail(email).orElse(null);

                if (usuario == null || !usuario.isEnabled() || !isPerfilVinculadoAtivo(usuario)) {
                    log.warn("[SECURITY FILTER] Bloqueio imediato de token pertencente a usuário inativo/revogado: {}", email);
                    SecurityContextHolder.clearContext();
                    escreverErroContaInativa(response);
                    return;
                }

                var authentication = new UsernamePasswordAuthenticationToken(
                        usuario, null, usuario.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isPerfilVinculadoAtivo(UsuarioModel usuario) {
        if (usuario.getRole() == Role.ROLE_BARBEIRO) {
            return barbeiroRepository.findByEmail(usuario.getEmail())
                    .map(BarbeiroModel::getAtivo)
                    .orElse(true);
        }
        if (usuario.getRole() == Role.ROLE_CLIENTE) {
            return clienteRepository.findByEmail(usuario.getEmail())
                    .map(ClienteModel::getAtivo)
                    .orElse(true);
        }
        return true;
    }

    private void escreverErroContaInativa(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        String detail = "Sua conta foi desativada ou revogada pelo administrador. Faça login novamente ou contate a gerência.";
        String body = String.format(
                "{\"type\":\"https://barber.dev/errors/account-disabled\","
                        + "\"title\":\"Conta Desativada ou Sessão Revogada\","
                        + "\"status\":401,"
                        + "\"detail\":\"%s\","
                        + "\"message\":\"%s\","
                        + "\"timestamp\":\"%s\"}",
                detail, detail, Instant.now().toString());
        response.getWriter().write(body);
    }

    private String recuperarToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        return authHeader.replace("Bearer ", "").trim();
    }
}
