package com.pastline;

import org.springframework.boot.SpringApplication;

public class TestPastlineBackendApplication {

	public static void main(String[] args) {
		SpringApplication.from(PastlineBackendApplication::main)
            .with(TestcontainersConfiguration.class)
            .run(args);
	}

}
