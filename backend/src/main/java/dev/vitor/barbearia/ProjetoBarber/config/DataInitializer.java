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

    @Value("${spring.h2.console.enabled:false}")
    private boolean h2ConsoleEnabled;

    @Value("${spring.datasource.url:}")
    private String datasourceUrl;

    @Value("${spring.datasource.password:}")
    private String datasourcePassword;

    @Value("${api.security.token.secret:}")
    private String jwtSecret;

    @Override
    public void run(String... args) {
        emitirAlertasSegurancaProducao();

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

    private void emitirAlertasSegurancaProducao() {
        boolean alertaDetectado = false;

        if (h2ConsoleEnabled) {
            alertaDetectado = true;
            log.warn("================================================================================");
            log.warn("[SECURITY ALERT - PRODUÇÃO] O Console H2 (/h2-console) está HABILITADO!");
            log.warn("Nunca execute em produção com H2_CONSOLE_ENABLED=true.");
            log.warn("================================================================================");
        }

        if (datasourceUrl != null && datasourceUrl.contains("jdbc:h2:mem:")) {
            alertaDetectado = true;
            log.warn("[SECURITY ALERT - BANCO DE DADOS] Banco H2 em memória detectado ({}). "
                    + "Antes de subir em produção, configure DATABASE_URL para PostgreSQL/MySQL e defina DB_USERNAME/DB_PASSWORD.", datasourceUrl);
        }

        if (datasourcePassword == null || datasourcePassword.isBlank()) {
            alertaDetectado = true;
            log.warn("[SECURITY ALERT - BANCO DE DADOS] O banco de dados está configurado SEM SENHA (DB_PASSWORD vazio). "
                    + "Proibido para ambientes de produção!");
        }

        if ("fQUKXewf8e6NeK8Hx4+YQ4mdJ7OEu5cCSqtLbR6Xm0cIUs8VjvfmajbibVos8/xv".equals(jwtSecret)) {
            alertaDetectado = true;
            log.warn("[SECURITY ALERT - JWT] Chave JWT padrão de desenvolvimento em uso. "
                    + "Defina a variável de ambiente JWT_SECRET antes de ir para produção.");
        }

        if ("GestorBarber@2026!".equals(gestorSenha) || "gestor123456".equals(gestorSenha)) {
            alertaDetectado = true;
            log.warn("[SECURITY ALERT - CREDENCIAIS] Senha padrão do Gestor detectada. "
                    + "Defina GESTOR_SENHA no ambiente de produção.");
        }

        if (!alertaDetectado) {
            log.info("[SECURITY AUDIT] Verificação de configuração de produção concluída sem alertas.");
        }
    }
}
