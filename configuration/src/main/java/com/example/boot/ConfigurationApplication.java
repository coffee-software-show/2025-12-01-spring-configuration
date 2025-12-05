package com.example.boot;


import org.jspecify.annotations.NonNull;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.annotation.Id;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.List;
import java.util.Map;

// dependency injection
// portable service abstractions
// aop
// autoconfiguration
@SpringBootApplication
public class ConfigurationApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigurationApplication.class, args);
    }

    @Bean
    CommandLineRunner init(CustomerRepository repository) {
        return _ -> {
            repository.findAll().forEach(IO::println);
            var saved = repository.saveAll(List.of(
                    new Customer(null, "Jane"),
                    new Customer(null, "John")
            ));
            saved.forEach(IO::println);
        };
    }
}

@RestController
class CustomerController {

    private final CustomerRepository repository;

    CustomerController(CustomerRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/customers")
    Collection<Customer> customers() {
        return repository.findAll();
    }

    @GetMapping("/")
    Map<String, String> hello() {
        return Map.of("message", "Hello World");
    }
}

interface CustomerRepository extends ListCrudRepository<@NonNull Customer, @NonNull Long> {
}

record Customer(@Id Integer id, String name) {
}