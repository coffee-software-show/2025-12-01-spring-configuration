package com.example.configuration;

//  @SpringBootApplication
//		SpringApplication.run(ConfigurationApplication.class, args);


import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.jdbc.support.SqlArrayValue;
import org.springframework.transaction.support.TransactionTemplate;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

// dependency injection
// portable service abstractions
//
public class ConfigurationApplication {

    public static void main(String[] args) throws Exception {
        var dataSource = (javax.sql.DataSource) new DriverManagerDataSource(
                "jdbc:postgresql://localhost/mydatabase", "myuser", "secret");
        var jdbcClient = JdbcClient.create(dataSource);
        var jdbcTransactionManager = new JdbcTransactionManager(dataSource);
        var transactionTemplate = new TransactionTemplate(jdbcTransactionManager);
        var repository = Transactions
                .transactional(new CustomerRepository(jdbcClient),
                        transactionTemplate);
        var runner = new CustomerRepositoryRunner(repository);
        runner.run(args);


    }

}

class CustomerRepositoryRunner {

    private final CustomerRepository repository;

    CustomerRepositoryRunner(CustomerRepository repository) {
        this.repository = repository;
    }

    void run(String[] args) throws Exception {

        repository.findAll().forEach(IO::println);

        var saved = repository.saveAll(List.of(
                new Customer(null, "Jane"),
                new Customer(null, "John")
        ));

        saved.forEach(IO::println);

    }
}


class Transactions {

    static <T> T transactional(T target,
                               TransactionTemplate transactionTemplate) {

        var pfb = new ProxyFactoryBean();
        pfb.setProxyTargetClass(true);
        pfb.setTarget(target);
        for (var i : target.getClass().getInterfaces()) {
            pfb.addInterface(i);
        }
        pfb.addAdvice((MethodInterceptor) (invocation) -> {
            var method = invocation.getMethod();
            var args = invocation.getArguments();
            return transactionTemplate
                    .execute(_ -> {
                        try {
                            return method.invoke(target, args);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    });

        });
        return (T) pfb.getObject();
    }

}

class TransactionalCustomerRepository  {

    private final CustomerRepository repository;
    private final TransactionTemplate transactionTemplate;

    TransactionalCustomerRepository(CustomerRepository repository,
                                    TransactionTemplate transactionTemplate) {
        this.repository = repository;
        this.transactionTemplate = transactionTemplate;
    }

    public Collection<Customer> findAll() {
        return this.repository.findAll();
    }

    public Collection<Customer> findById(Iterable<Integer> ids) {
        return this.repository.findById(ids);
    }

    public Collection<Customer> saveAll(Iterable<Customer> customers) {
        return this.transactionTemplate.execute(_ ->
                this.repository.saveAll(customers));
    }
}

class CustomerRepository {

    private final JdbcClient db;

    private final RowMapper<Customer> rowMapper =
            (rs, _) -> new Customer(rs.getInt("id"), rs.getString("name"));

    CustomerRepository(JdbcClient db) {
        this.db = db;
    }


    public Collection<Customer> findAll() {
        return this.db
                .sql("select * from customer")
                .query(this.rowMapper)
                .list();
    }

    public Collection<Customer> findById(Iterable<Integer> ids) {
        return this.db
                .sql("select * from customer where id = any(?)")
                .params(new SqlArrayValue("int4", (Object[]) from(ids)))
                .query(this.rowMapper)
                .list();
    }

    private static <T> T[] from(Iterable<T> iterable) {
        var list = new ArrayList<T>();
        for (var item : iterable) {
            list.add(item);
        }
        return list.toArray((T[]) Array.newInstance(Object.class, list.size()));
    }

    public Collection<Customer> saveAll(Iterable<Customer> customers) {
        var ids = new ArrayList<Integer>();
        var counter = 0;
        for (var c : customers) {
            var gkh = new GeneratedKeyHolder();
            if (counter == 1) throw new IllegalStateException("oops!");
            this.db
                    .sql("insert into customer(name) values (?)")
                    .params(c.name())
                    .update(gkh);
            // throw exception here
            var id = ((Number) gkh.getKeyList().getFirst().get("id")).intValue();
            ids.add(id);
            counter += 1;
        }
        return this.findById(ids);
    }
}

record Customer(Integer id, String name) {
}