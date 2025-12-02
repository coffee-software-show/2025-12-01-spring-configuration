package com.example.demo;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class BarConfiguration {

    @Bean
    Bar bar(FooConfiguration foo) {
        return new Bar(foo.foo());
    }

}
