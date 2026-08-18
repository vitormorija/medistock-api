package br.com.medistock.api.config;

import br.com.medistock.api.security.JwtAuthenticationEntryPoint;
import br.com.medistock.api.security.JwtAuthenticationFilter;
import br.com.medistock.api.security.RestAccessDeniedHandler;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    private static final String[] ROTAS_PUBLICAS = {
            "/api/v1/auth/login",
            "/api/v1/auth/registro",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**"
    };

    private final JwtAuthenticationFilter filtroJwt;
    private final JwtAuthenticationEntryPoint tratadorDeNaoAutenticado;
    private final RestAccessDeniedHandler tratadorDeAcessoNegado;
    private final CorsConfigurationSource origensPermitidas;

    public SecurityConfig(JwtAuthenticationFilter filtroJwt,
                          JwtAuthenticationEntryPoint tratadorDeNaoAutenticado,
                          RestAccessDeniedHandler tratadorDeAcessoNegado,
                          @Qualifier("corsConfigurationSource") CorsConfigurationSource origensPermitidas) {
        this.filtroJwt = filtroJwt;
        this.tratadorDeNaoAutenticado = tratadorDeNaoAutenticado;
        this.tratadorDeAcessoNegado = tratadorDeAcessoNegado;
        this.origensPermitidas = origensPermitidas;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(origensPermitidas))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(rotas -> rotas
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(ROTAS_PUBLICAS).permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(erros -> erros
                        .authenticationEntryPoint(tratadorDeNaoAutenticado)
                        .accessDeniedHandler(tratadorDeAcessoNegado))
                .addFilterBefore(filtroJwt, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuracao)
            throws Exception {
        return configuracao.getAuthenticationManager();
    }
}
