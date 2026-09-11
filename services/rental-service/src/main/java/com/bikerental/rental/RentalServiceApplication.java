package com.bikerental.rental;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication
public class RentalServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(
                RentalServiceApplication.class,
                args
        );
    }
}