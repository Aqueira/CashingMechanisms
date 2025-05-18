package com.petforwork;

import org.springframework.boot.SpringApplication;

public class TestPetForWorkApplication {

    public static void main(String[] args) {
        SpringApplication.from(PetForWorkApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
