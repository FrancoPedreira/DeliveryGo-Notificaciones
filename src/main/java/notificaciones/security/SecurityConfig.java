package notificaciones.security;

import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.HandlerExceptionResolver;

// Reglas de acceso del módulo: quién puede llamar a cada API, y CORS para el Portal Web
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthFilter jwtAuthFilter,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) throws Exception {
        return http
                // API REST sin sesión ni formularios: cada solicitud se autentica con su token
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .cors(Customizer.withDefaults())
                // La consola H2 se muestra dentro de un frame
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .authorizeHttpRequests(auth -> auth
                        // Público: documentación, consola de la base de pruebas y chequeo de disponibilidad
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/v3/api-docs.yaml",
                                "/h2-console/**", "/status")
                        .permitAll()
                        // Solo ADMIN administra plantillas y canales (CU-07 y CU-08)
                        .requestMatchers(HttpMethod.POST, "/api/v1/templates/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/templates/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/channels/**").hasRole("ADMIN")
                        // El resto: cualquier usuario o módulo con un token válido
                        .anyRequest().authenticated())
                // 401 (sin token o inválido) y 403 (sin el rol): los resuelve GlobalExceptionHandler,
                // así salen con el mismo formato de error que el resto de la API
                .exceptionHandling(errores -> errores
                        .authenticationEntryPoint((request, response, e) -> resolver.resolveException(request,
                                response, null, e))
                        .accessDeniedHandler((request, response, e) -> resolver.resolveException(request, response,
                                null, e)))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    // CORS: el navegador solo deja que el Portal Web llame a esta API si su origen está permitido acá
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${notificaciones.cors.origenes-permitidos}") List<String> origenes) {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(origenes);
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Correlation-Id"));
        cors.setExposedHeaders(List.of("X-Correlation-Id"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);
        return source;
    }
}
