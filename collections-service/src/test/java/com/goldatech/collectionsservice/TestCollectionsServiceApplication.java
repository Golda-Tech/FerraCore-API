package com.goldatech.collectionsservice;

import org.springframework.boot.SpringApplication;

public class TestCollectionsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.from(CollectionsServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
