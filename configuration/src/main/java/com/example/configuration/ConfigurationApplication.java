package com.example.configuration;

import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Collection;
import java.util.List;

// 1. java configuration
// 2. component scanning
// 3. beanregistrars
// 4. autoconfiguration

@SpringBootApplication
public class ConfigurationApplication {

    public static void main(String[] args) throws Exception {
        SpringApplication.run(ConfigurationApplication.class, args);
    }

    @Bean
    ApplicationRunner runner(CustomerRepository repository) {
        return a -> {
            if (repository.findAll().size() == 2) {
                return;
            }
            var all = repository.saveAll(List.of(new Customer(null, "A"),
                    new Customer(null, "B")));
            for (var customer : all) {
                IO.println(customer);
            }
        };
    }
}

@Controller
@ResponseBody
class CustomerController {

    private final CustomerRepository repository;

    CustomerController(CustomerRepository repository) {
        this.repository = repository;
    }

    @GetMapping ("/customers")
    Collection<Customer> getCustomers() {
        return repository.findAll();
    }
}

interface CustomerRepository extends ListCrudRepository<@NonNull Customer, @NonNull Integer> {
}

record Customer(Integer id, String name) {
}