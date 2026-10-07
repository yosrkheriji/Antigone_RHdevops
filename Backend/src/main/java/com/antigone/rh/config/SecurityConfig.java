package com.antigone.rh.config;

import com.antigone.rh.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import jakarta.servlet.DispatcherType;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
// Sans cette annotation, Spring n'active pas la securite au niveau methode : tous les
// @PreAuthorize du projet sont alors evalues... jamais. Elle est indispensable au
// cloisonnement par permission exige par l'assistant IA.
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // Origines autorisées : celles déclarées dans app.frontend-url (séparées par
        // des virgules), plus les déploiements Render. Ajouter un frontend se fait
        // donc en configuration, sans toucher à cette classe.
        List<String> origines = new ArrayList<>(Arrays.stream(frontendUrl.split(","))
                .map(String::trim)
                .filter(origine -> !origine.isEmpty())
                .toList());
        origines.add("https://*.onrender.com");
        config.setAllowedOriginPatterns(origines);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Les reponses en streaming (SseEmitter) provoquent un second
                        // passage dans la chaine de filtres, en dispatch ASYNC, alors que
                        // le SecurityContext du thread de requete a deja ete libere.
                        // Sans cette regle l'autorisation echoue sur une reponse deja
                        // committee : le flux se termine sur une erreur servlet. La
                        // decision d'acces a bien eu lieu sur le dispatch REQUEST.
                        .dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.POST,
                                "/api/auth/login",
                                "/api/auth/client-login",
                                "/api/auth/forgot-password",
                                "/api/auth/reset-password")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/agent/download",
                                "/api/media-plans/google-drive/callback",
                                "/api/clients/*/logo",
                                "/uploads/**")
                        .permitAll()
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html")
                        .permitAll()
                        .requestMatchers(HttpMethod.PUT, "/api/comptes/*/password").authenticated()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/comptes/**").hasAnyAuthority("ROLE_ADMIN", "VIEW_COMPTES")
                        .requestMatchers("/api/roles/**").hasAnyAuthority("ROLE_ADMIN", "VIEW_ROLES")
                        .requestMatchers("/api/finance/**").hasAnyAuthority("ROLE_ADMIN", "VIEW_FINANCE")
                        .requestMatchers(HttpMethod.GET, "/api/referentiels/**").authenticated()
                        .requestMatchers("/api/referentiels/**").hasAnyAuthority("ROLE_ADMIN", "VIEW_REFERENTIELS")
                        .requestMatchers("/api/agent/**")
                        .authenticated()
                        // Assistant IA : l'authentification suffit ici, le perimetre fin
                        // (marque, employe, capacite) est applique par AiAccessScope dans
                        // les services et dans chaque outil.
                        .requestMatchers("/api/v1/**").authenticated()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
