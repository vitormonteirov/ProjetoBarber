package dev.vitor.barbearia.ProjetoBarber.security;

import dev.vitor.barbearia.ProjetoBarber.usuario.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfigurations {

    private final SecurityFilter securityFilter;
    private final UsuarioRepository usuarioRepository;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(req -> {
                    // Console H2 e Swagger/Docs caso aplicável
                    req.requestMatchers("/h2-console/**").permitAll();

                    // Autenticação e Registro
                    req.requestMatchers(HttpMethod.POST, "/api/auth/**").permitAll();

                    // Rotas Públicas para clientes consultarem catálogo antes de agendar
                    req.requestMatchers(HttpMethod.GET, "/api/servicos/**").permitAll();
                    req.requestMatchers(HttpMethod.GET, "/api/barbeiros/**").permitAll();
                    req.requestMatchers(HttpMethod.GET, "/api/agendamentos/disponibilidade").permitAll();

                    // Rotas restritas para Gestor
                    req.requestMatchers("/api/gestor/**").hasRole("GESTOR");

                    // Rotas restritas para Barbeiro
                    req.requestMatchers("/api/barbeiro/**").hasRole("BARBEIRO");

                    // Rotas restritas para Cliente
                    req.requestMatchers("/api/cliente/**").hasRole("CLIENTE");

                    // Demais requisições autenticadas
                    req.anyRequest().authenticated();
                })
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable)) // Permite H2 Console
                .addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> usuarioRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + username));
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService());
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
