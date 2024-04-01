package ru.dogobot.Dogobot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
public class DogobotApplication {

	public static void main(String[] args) {
		log.info(
				System.lineSeparator() + System.lineSeparator()
				+ "###############################################################" + System.lineSeparator()
				+ "############################ START ############################" + System.lineSeparator()
				+ "###############################################################"
				+ System.lineSeparator()
		);
		SpringApplication.run(DogobotApplication.class, args);
	}

}
