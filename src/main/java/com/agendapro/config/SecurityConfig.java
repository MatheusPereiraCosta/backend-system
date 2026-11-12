package com.agendapro.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Regras de acesso da API:
 * - Público (sem login): cadastro/login, busca e página do estabelecimento, serviços, profissionais,
 *   horários livres e criação de agendamento pelo cliente final.
 * - Dono logado: tudo em /establishments/{id}/** somente quando {id} é o estabelecimento dele (claim do JWT).
 * - Qualquer outra rota é negada.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Em produção o nginx serve front e API no mesmo domínio (sem CORS). A lista só vale para o
     * `ng serve` (localhost:4200 chamando a API em outra porta). Configurável em app.cors.allowed-origins.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins:http://localhost:4200}") List<String> allowedOrigins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable) // API stateless com Bearer token, sem cookies de sessão
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/register", "/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/auth/me").authenticated()
                        .requestMatchers(HttpMethod.GET, "/establishments/search", "/establishments/slug/*").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/establishments/*/services",
                                "/establishments/*/professionals",
                                "/establishments/*/professionals/by-service/*",
                                "/establishments/*/availability").permitAll()
                        .requestMatchers(HttpMethod.POST, "/establishments/*/appointments").permitAll()
                        .requestMatchers(HttpMethod.POST, "/establishments").authenticated()
                        .requestMatchers("/establishments/{establishmentId}/**").access(ownerOfEstablishment())
                        .anyRequest().denyAll());
        return http.build();
    }

    private AuthorizationManager<RequestAuthorizationContext> ownerOfEstablishment() {
        return (authentication, context) -> {
            var auth = authentication.get();
            if (!(auth instanceof JwtAuthenticationToken jwtAuth) || !auth.isAuthenticated()) {
                return new AuthorizationDecision(false);
            }
            String owned = jwtAuth.getToken().getClaimAsString("establishmentId");
            String requested = context.getVariables().get("establishmentId");
            return new AuthorizationDecision(owned != null && owned.equalsIgnoreCase(requested));
        };
    }
}
