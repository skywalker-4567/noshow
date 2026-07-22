package com.hospital.noshow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling   // required for §11's @Scheduled cron jobs to actually fire
public class NoshowApplication {

	public static void main(String[] args) {
		SpringApplication.run(NoshowApplication.class, args);
	}
}