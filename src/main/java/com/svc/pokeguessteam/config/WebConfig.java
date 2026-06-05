package com.svc.pokeguessteam.config;

import com.svc.pokeguessteam.security.SessionBindingInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final SessionBindingInterceptor sessionBindingInterceptor;

    public WebConfig(SessionBindingInterceptor sessionBindingInterceptor) {
        this.sessionBindingInterceptor = sessionBindingInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(sessionBindingInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/auth/login",
                        "/auth/register",
                        "/auth/session",
                        "/auth/logout",
                        "/auth/email/verification/send",
                        "/auth/email/verification/confirm",
                        "/auth/verification/resend",
                        "/auth/verification/confirm",
                        "/auth/password-reset/request",
                        "/auth/password-reset/confirm",
                        "/api/meta",
                        "/public/**",
                        "/error",
                        "/favicon.ico"
                );
    }
}
