package co.edu.humboldt.plataformacasos.configuracion.seguridad;

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

/**
 * Seguridad de la API: sin estado, con token Bearer (JWT). Solo los endpoints de inicio
 * de sesión son públicos; todo lo demás exige autenticación. La autorización por permiso
 * (RF-02) se aplica en los proxies de servicio, no aquí.
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
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(reglas -> reglas
                        .requestMatchers(HttpMethod.POST, "/api/auth/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(recurso -> recurso
                        .jwt(Customizer.withDefaults())
                        .authenticationEntryPoint(respuestasError)
                        .accessDeniedHandler(respuestasError))
                .exceptionHandling(errores -> errores
                        .authenticationEntryPoint(respuestasError)
                        .accessDeniedHandler(respuestasError));
        return http.build();
    }

    /** Para códigos de autorización y contraseñas: siempre se guardan con hash, nunca en claro. */
    @Bean
    PasswordEncoder codificadorClaves() {
        return new BCryptPasswordEncoder();
    }
}
