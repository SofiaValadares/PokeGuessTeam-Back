package com.svc.pokeguessteam;

import com.svc.pokeguessteam.config.AppAuthProperties;
import com.svc.pokeguessteam.config.AppCorsProperties;
import com.svc.pokeguessteam.config.AppDevToolsProperties;
import com.svc.pokeguessteam.config.AppMailProperties;
import com.svc.pokeguessteam.config.AppMasterAdminProperties;
import com.svc.pokeguessteam.config.AppPusherProperties;
import com.svc.pokeguessteam.config.AppResendProperties;
import com.svc.pokeguessteam.config.DotenvEnvironmentLoader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties({
        AppAuthProperties.class,
        AppCorsProperties.class,
        AppDevToolsProperties.class,
        AppMailProperties.class,
        AppMasterAdminProperties.class,
        AppResendProperties.class,
        AppPusherProperties.class
})
@EnableAsync
@EnableScheduling
public class PokeguessteamApplication {

    public static void main(String[] args) {
        DotenvEnvironmentLoader.load();
        // Logback é inicializado automaticamente pelo Spring Boot (logback-spring.xml).
        SpringApplication.run(PokeguessteamApplication.class, args);
    }

}
