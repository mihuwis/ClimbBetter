package pl.climbbetter.climbbetter_backend;

import org.springframework.boot.SpringApplication;

public class TestClimbbetterBackendApplication {

	public static void main(String[] args) {
		SpringApplication.from(ClimbbetterBackendApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
