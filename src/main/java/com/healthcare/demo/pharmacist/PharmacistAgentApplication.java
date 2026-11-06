package com.healthcare.demo.pharmacist;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@org.springframework.context.annotation.Import(com.healthcare.demo.shared.AgentConfiguration.class)
@SpringBootApplication
public class PharmacistAgentApplication {
  public static void main(String[] args) {
    SpringApplication.run(PharmacistAgentApplication.class, args);
  }
}
