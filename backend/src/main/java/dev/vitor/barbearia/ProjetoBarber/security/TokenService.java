package dev.vitor.barbearia.ProjetoBarber.security;

import dev.vitor.barbearia.ProjetoBarber.usuario.UsuarioModel;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
public class TokenService {

    private final SecretKey signingKey;
    private final String issuer;
    private final long expirationHours;

    public TokenService(
            @Value("${api.security.token.secret:projeto-barber-secret-key-super-segura-e-forte-para-jwt-token-hmac-sha-256}") String secret,
            @Value("${api.security.token.issuer:ProjetoBarber-API}") String issuer,
            @Value("${api.security.token.expiration-hours:8}") long expirationHours) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.issuer = issuer;
        this.expirationHours = expirationHours;
    }

    public String gerarToken(UsuarioModel usuario) {
        Instant agora = Instant.now();
        Instant expiracao = agora.plus(expirationHours, ChronoUnit.HOURS);

        return Jwts.builder()
                .issuer(issuer)
                .subject(usuario.getEmail())
                .claim("id", usuario.getId())
                .claim("nome", usuario.getNome())
                .claim("role", usuario.getRole().name())
                .issuedAt(Date.from(agora))
                .expiration(Date.from(expiracao))
                .signWith(signingKey)
                .compact();
    }

    public String validarToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.getSubject();
        } catch (Exception e) {
            return null;
        }
    }
}
