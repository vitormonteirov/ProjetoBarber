package dev.vitor.barbearia.ProjetoBarber;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.Clock;
import java.time.ZoneId;
import java.util.TimeZone;

@SpringBootApplication
public class ProjetoBarberApplication {

	public static final ZoneId ZONE_BRASILIA = ZoneId.of("America/Sao_Paulo");

	public static void main(String[] args) {
		TimeZone.setDefault(TimeZone.getTimeZone(ZONE_BRASILIA));
		SpringApplication.run(ProjetoBarberApplication.class, args);
	}

	@PostConstruct
	void configurarTimeZonePadrao() {
		TimeZone.setDefault(TimeZone.getTimeZone(ZONE_BRASILIA));
	}

	@Bean
	public Clock appClock() {
		return Clock.system(ZONE_BRASILIA);
	}

}
