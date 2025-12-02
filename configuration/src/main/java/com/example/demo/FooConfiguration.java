package com.example.demo;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class FooConfiguration {

    @Bean
    Foo foo() {
        return new Foo();
    }

}
