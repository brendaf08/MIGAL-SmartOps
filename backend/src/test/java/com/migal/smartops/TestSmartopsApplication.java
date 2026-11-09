package com.migal.smartops;

import org.springframework.boot.SpringApplication;

public class TestSmartopsApplication {

	public static void main(String[] args) {
		SpringApplication.from(SmartopsApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
