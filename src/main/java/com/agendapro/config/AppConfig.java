package com.agendapro.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class AppConfig {

    /**
     * Relógio da aplicação no fuso do negócio. Os horários de atendimento são "locais"
     * (ex.: 09:00 em São Paulo), então "agora" precisa seguir o mesmo fuso, e não o do container (UTC).
     */
    @Bean
    public Clock clock(@Value("${app.timezone:America/Sao_Paulo}") String timezone) {
        return Clock.system(ZoneId.of(timezone));
    }
}
