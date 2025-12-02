package com.example.configuration;

import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.SqlArrayValue;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ConfigurationApplication {

    public static void main(String[] args) throws Exception {

        var driverManagerDataSource = new DriverManagerDataSource(
                "jdbc:postgresql://localhost:5432/mydatabase", "myuser", "secret");

        var sqlResource = new ClassPathResource("/schema.sql");
        var resourceDatabasePopulator = new ResourceDatabasePopulator(sqlResource);

        var dataSourceInit = new DataSourceInitializer();
        dataSourceInit.setDataSource(driverManagerDataSource);
        dataSourceInit.setEnabled(true);
        dataSourceInit.setDatabasePopulator(resourceDatabasePopulator);
        dataSourceInit.afterPropertiesSet();

        var jdbc = JdbcClient.create(driverManagerDataSource);

        var jdbcCustomerService = new JdbcCustomerService(jdbc);

        var runner = new CustomerServiceRunner(jdbcCustomerService);
        runner.run(args);

    }

}

// 1. dependency injection
// 2. portable service abstractions
// 3. aspect oriented programming

class CustomerServiceRunner {

    private final CustomerService customerService;

    CustomerServiceRunner(CustomerService customerService) {
        this.customerService = customerService;
    }

    void run(String[] args) throws Exception {
        if (!this.customerService.findAll().isEmpty())
            return;

        IO.println("going to initialize the SQL database");
        var saved = this.customerService.saveAll(
                new Customer(null, "Jane"), new Customer(null, "John"));
        IO.println(saved);
        Assert.state(saved.size() == 2, "there should be two records in the SQL database");

    }
}

class JdbcCustomerService implements CustomerService {

    private final JdbcClient jdbc;

    private final RowMapper<Customer> customerRowMapper = (rs, _) ->
            new Customer(rs.getInt("id"), rs.getString("name"));

    JdbcCustomerService(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Collection<Customer> findAll() {
        return this.jdbc
                .sql("select * from customers")
                .query(this.customerRowMapper)
                .list();
    }

    @Override
    public Collection<Customer> findById(Integer... ids) {
        if (ids.length == 0)
            return List.of();
        return this.jdbc
                .sql("select * from customers where id = any(?)")
                .params(new SqlArrayValue("int4", ids))
                .query(this.customerRowMapper)
                .list();
    }

    @Override
    public Collection<Customer> saveAll(Customer... customers) {
        var ids = new ArrayList<Integer>();
        for (var customer : customers) {
            var kh = new GeneratedKeyHolder();
            this.jdbc
                    .sql("insert into customers (name) values (?)")
                    .params(customer.name())
                    .update(kh);
            var returnedKeys = kh.getKeyList().getFirst();
            if (returnedKeys.get("id") instanceof Number number)
                ids.add(number.intValue());
        }
        return findById(ids.toArray(new Integer[0]));
    }
}

interface CustomerService {

    Collection<Customer> findAll();

    Collection<Customer> findById(Integer... ids);

    Collection<Customer> saveAll(Customer... customers);

}

record Customer(Integer id, String name) {
}