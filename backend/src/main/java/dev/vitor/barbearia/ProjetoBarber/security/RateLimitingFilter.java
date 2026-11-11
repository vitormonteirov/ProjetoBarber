package dev.vitor.barbearia.ProjetoBarber.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Filtro de proteção contra sobrecarga (DoS / Brute-Force / BCrypt CPU Exhaustion).
 * Aplica janelas deslizantes de 1 minuto por IP, diferenciando rotas sensíveis de
 * autenticação (/api/auth/**) das demais rotas da API (/api/**).
 */
@Component
@Slf4j
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final long WINDOW_MS = 60_000L;
    private static final int MAX_TRACKED_IPS = 10_000;

    @Value("${app.security.ratelimit.enabled:true}")
    private boolean enabled;

    @Value("${app.security.ratelimit.auth-per-minute:15}")
    private int authLimitPerMinute;

    @Value("${app.security.ratelimit.api-per-minute:120}")
    private int apiLimitPerMinute;

    private final Map<String, WindowCounter> counters = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        if (!enabled) {
            filterChain.doFilter(request, response);
            return;
        }

        String uri = request.getRequestURI();
        if (uri == null || !uri.startsWith("/api/")) {
            filterChain.doFilter(request, response);
            return;
        }

        boolean isAuthRoute = uri.startsWith("/api/auth/");
        int limit = isAuthRoute ? authLimitPerMinute : apiLimitPerMinute;
        String bucketType = isAuthRoute ? "AUTH" : "API";
        String clientIp = resolveClientIp(request);
        String key = bucketType + ":" + clientIp;

        long now = System.currentTimeMillis();
        if (counters.size() > MAX_TRACKED_IPS) {
            evictExpiredEntries(now);
        }

        WindowCounter counter = counters.compute(key, (k, existing) -> {
            if (existing == null || (now - existing.windowStartMs) >= WINDOW_MS) {
                return new WindowCounter(now, new AtomicInteger(1));
            }
            existing.count.incrementAndGet();
            return existing;
        });

        int currentCount = counter.count.get();
        long retryAfterSeconds = Math.max(1L, (WINDOW_MS - (now - counter.windowStartMs)) / 1000L);

        response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, limit - currentCount)));

        if (currentCount > limit) {
            log.warn("[RATE LIMIT] Bloqueio 429 para IP {} na rota {} ({}/{} req/min)",
                    clientIp, uri, currentCount, limit);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);

            String detail = isAuthRoute
                    ? "Muitas tentativas de autenticação em curto período. Aguarde " + retryAfterSeconds + " segundos antes de tentar novamente."
                    : "Limite de requisições excedido para proteção contra sobrecarga. Tente novamente em " + retryAfterSeconds + " segundos.";

            String body = String.format(
                    "{\"type\":\"https://barber.dev/errors/too-many-requests\","
                            + "\"title\":\"Muitas Requisições (Rate Limit)\","
                            + "\"status\":429,"
                            + "\"detail\":\"%s\","
                            + "\"message\":\"%s\","
                            + "\"retryAfterSeconds\":%d,"
                            + "\"timestamp\":\"%s\"}",
                    detail, detail, retryAfterSeconds, Instant.now().toString());
            response.getWriter().write(body);
            return;
        }

        filterChain.doFilter(request, response);
    }

    public void resetBuckets() {
        counters.clear();
    }

    private void evictExpiredEntries(long now) {
        counters.entrySet().removeIf(e -> (now - e.getValue().windowStartMs) >= WINDOW_MS);
    }

    private String resolveClientIp(HttpServletRequest request) {
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
    }

    private static final class WindowCounter {
        final long windowStartMs;
        final AtomicInteger count;

        WindowCounter(long windowStartMs, AtomicInteger count) {
            this.windowStartMs = windowStartMs;
            this.count = count;
        }
    }
}
