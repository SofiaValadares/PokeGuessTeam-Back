package com.svc.pokeguessteam.config;

import com.svc.pokeguessteam.security.DatabaseRoleRefreshFilter;
import com.svc.pokeguessteam.security.JsonAuthenticationEntryPoint;
import com.svc.pokeguessteam.security.SessionIpBindingFilter;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.server.CookieSameSiteSupplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Duration;
import java.util.List;

@Configuration
public class SecurityConfig {

    private final AppCorsProperties corsProperties;
    private final DatabaseRoleRefreshFilter databaseRoleRefreshFilter;
    private final SessionIpBindingFilter sessionIpBindingFilter;

    public SecurityConfig(
            AppCorsProperties corsProperties,
            DatabaseRoleRefreshFilter databaseRoleRefreshFilter,
            SessionIpBindingFilter sessionIpBindingFilter
    ) {
        this.corsProperties = corsProperties;
        this.databaseRoleRefreshFilter = databaseRoleRefreshFilter;
        this.sessionIpBindingFilter = sessionIpBindingFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JsonAuthenticationEntryPoint authenticationEntryPoint
    ) throws Exception {

        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())

                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .sessionFixation(fixation -> fixation.migrateSession())
                        .maximumSessions(1)
                        .maxSessionsPreventsLogin(false)
                        .sessionRegistry(sessionRegistry())
                )

                .authorizeHttpRequests(auth -> auth

                        // AUTENTICAÇÃO


                        .requestMatchers(
                                HttpMethod.POST,
                                "/auth/register"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.POST,
                                "/auth/login"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.POST,
                                "/auth/logout"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/auth/session"
                        ).permitAll()


                        // VERIFICAÇÃO DE E-MAIL

                        .requestMatchers(
                                HttpMethod.POST,
                                "/auth/email/verification/send"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.POST,
                                "/auth/email/verification/confirm"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.POST,
                                "/auth/verification/resend"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.POST,
                                "/auth/verification/confirm"
                        ).permitAll()


                        // RECUPERAÇÃO DE SENHA

                        .requestMatchers(
                                HttpMethod.POST,
                                "/auth/password-reset/request"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.POST,
                                "/auth/password-reset/confirm"
                        ).permitAll()


                        // ROTAS PÚBLICAS

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/meta"
                        ).permitAll()

                        .requestMatchers(
                                "/public/**"
                        ).permitAll()


                        // MASTER ADMIN

                        /*
                         * Alteração de papel de usuário.
                         * Apenas MASTER_ADMIN pode:
                         * USER -> ADMIN
                         * ADMIN -> USER
                         * ADMIN -> MASTER_ADMIN
                         * MASTER_ADMIN -> ADMIN
                         */
                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/admin/users/*/role"
                        ).hasRole("MASTER_ADMIN")


                        /*
                         * Histórico de alterações administrativas.
                         * Somente MASTER_ADMIN pode visualizar.
                         */
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/admin/audit/**"
                        ).hasRole("MASTER_ADMIN")


                        // ADMIN / MASTER ADMIN

                        .requestMatchers(
                                "/api/admin/**"
                        ).hasAnyRole(
                                "ADMIN",
                                "MASTER_ADMIN"
                        )


                        // RESTANTE DA APLICAÇÃO

                        .anyRequest().authenticated()
                )

                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(
                                authenticationEntryPoint
                        )
                )

                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable());

        /*
         * Atualiza as authorities do usuário a partir do banco ANTES de o Spring Security decidir se ele possui permissão para acessar uma determinada rota.
         * O mesmo vale para rebaixamentos.
         */
        http.addFilterBefore(
                sessionIpBindingFilter,
                AuthorizationFilter.class
        );
        http.addFilterBefore(
                databaseRoleRefreshFilter,
                AuthorizationFilter.class
        );

        return http.build();
    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    @Bean
    public FilterRegistrationBean<DatabaseRoleRefreshFilter> databaseRoleRefreshFilterRegistration(
            DatabaseRoleRefreshFilter filter
    ) {
        FilterRegistrationBean<DatabaseRoleRefreshFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<SessionIpBindingFilter> sessionIpBindingFilterRegistration(
            SessionIpBindingFilter filter
    ) {
        FilterRegistrationBean<SessionIpBindingFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration config =
                new CorsConfiguration();

        config.setAllowedOriginPatterns(
                corsProperties.getAllowedOriginPatterns()
        );

        config.setAllowCredentials(true);

        config.setAllowedMethods(
                List.of(
                        "GET",
                        "POST",
                        "PUT",
                        "PATCH",
                        "DELETE",
                        "OPTIONS"
                )
        );

        config.setAllowedHeaders(
                List.of(
                        "Content-Type",
                        "Authorization",
                        "X-Requested-With",
                        "Accept-Language"
                )
        );

        config.setExposedHeaders(
                List.of("Set-Cookie")
        );

        config.setMaxAge(
                Duration.ofHours(1)
        );

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/**",
                config
        );

        return source;
    }

    @Bean
    public CookieSameSiteSupplier applicationCookieSameSiteSupplier() {

        return CookieSameSiteSupplier
                .ofLax()
                .whenHasName("JSESSIONID");
    }

    @Bean
    public PasswordEncoder passwordEncoder() {

        return new BCryptPasswordEncoder();
    }
}