package dev.vitor.barbearia.ProjetoBarber.config;

import dev.vitor.barbearia.ProjetoBarber.usuario.Role;
import dev.vitor.barbearia.ProjetoBarber.usuario.UsuarioModel;
import dev.vitor.barbearia.ProjetoBarber.usuario.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.gestor.email:gestor@tenoriosbarber.com}")
    private String gestorEmail;

    @Value("${app.gestor.senha:gestor123456}")
    private String gestorSenha;

    @Value("${app.gestor.nome:Gestor Principal}")
    private String gestorNome;

    @Override
    public void run(String... args) {
        if (!usuarioRepository.existsByEmail(gestorEmail)) {
            log.info("Inicializando conta prévia do Gestor: {}", gestorEmail);
            UsuarioModel gestor = UsuarioModel.builder()
                    .nome(gestorNome)
                    .email(gestorEmail)
                    .senha(passwordEncoder.encode(gestorSenha))
                    .role(Role.ROLE_GESTOR)
                    .ativo(true)
                    .build();
            usuarioRepository.save(gestor);
            log.info("Conta do Gestor criada com sucesso!");
        }
    }
}
