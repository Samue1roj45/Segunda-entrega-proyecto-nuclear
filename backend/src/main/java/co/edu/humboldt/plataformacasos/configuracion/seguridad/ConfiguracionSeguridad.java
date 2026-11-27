package co.edu.humboldt.plataformacasos.configuracion.seguridad;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Seguridad de toda la API (módulos 1 a 6): sin estado, con el token Bearer (JWT) que emite el
 * módulo 1. Solo el inicio de sesión y el estado del servicio son públicos; todo lo demás exige
 * autenticación. La autorización por permiso (RF-02) se aplica en los proxies de servicio, no aquí.
 */
@Configuration
@EnableWebSecurity
public class ConfiguracionSeguridad {

    @Bean
    SecurityFilterChain cadenaFiltrosSeguridad(HttpSecurity http, RespuestasErrorSeguridad respuestasError)
            throws Exception {
        http
                // API sin estado que no usa cookies de sesión: CSRF no aplica.
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(reglas -> reglas
                        .requestMatchers(HttpMethod.POST, "/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/salud", "/api/auth/google/config").permitAll()
                        // Invitaciones: quien abre el enlace aún no tiene cuenta; el token del enlace es la credencial.
                        .requestMatchers(HttpMethod.GET, "/api/invitaciones/publica/*").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/invitaciones/publica/*/aceptar").permitAll()
                        // Ecos: el juego se identifica con el código de sesión o con su propio token; lo valida el servicio.
                        .requestMatchers(HttpMethod.POST, "/api/ecos/v1/sesiones/*/canje",
                                "/api/ecos/v1/sesiones/*/eventos", "/api/ecos/v1/sesiones/*/resultado").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(recurso -> recurso
                        .jwt(Customizer.withDefaults())
                        .bearerTokenResolver(resolutorDeToken())
                        .authenticationEntryPoint(respuestasError)
                        .accessDeniedHandler(respuestasError))
                .exceptionHandling(errores -> errores
                        .authenticationEntryPoint(respuestasError)
                        .accessDeniedHandler(respuestasError));
        return http.build();
    }

    private static final java.util.regex.Pattern RUTAS_DEL_JUEGO =
            java.util.regex.Pattern.compile("^/api/ecos/v1/sesiones/[^/]+/(canje|eventos|resultado)$");

    /**
     * El token del juego NO es un JWT web: en las rutas del juego se ignora el encabezado Authorization aquí, para que el
     * filtro web no lo rechace; ServicioEcos lo valida con su propia clave.
     */
    private static org.springframework.security.oauth2.server.resource.web.BearerTokenResolver resolutorDeToken() {
        var porDefecto = new org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver();
        return solicitud -> "POST".equals(solicitud.getMethod()) && RUTAS_DEL_JUEGO.matcher(solicitud.getRequestURI()).matches()
                ? null : porDefecto.resolve(solicitud);
    }

    /** Los frontends Angular (puerto 4200 en desarrollo) llaman a la API desde otro origen. */
    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.origenes:http://localhost:4200}") List<String> origenes) {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(origenes);
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Caso-Version"));
        cors.setExposedHeaders(List.of("Content-Disposition", "X-Caso-Version"));
        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/api/**", cors);
        return fuente;
    }

    /** Para códigos de autorización y contraseñas: siempre se guardan con hash, nunca en claro. */
    @Bean
    PasswordEncoder codificadorClaves() {
        return new BCryptPasswordEncoder();
    }
}
