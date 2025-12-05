package com.example.configuration;

//  @SpringBootApplication
//		SpringApplication.run(ConfigurationApplication.class, args);


import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.aopalliance.intercept.MethodInterceptor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.*;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.AliasFor;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.jdbc.support.SqlArrayValue;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.lang.annotation.*;
import java.lang.reflect.Array;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

// dependency injection
// portable service abstractions
//
public class ConfigurationApplication {

    public static void main(String[] args) throws Exception {
        var applicationContext =
                new AnnotationConfigApplicationContext(CustomerConfiguration.class);
        var runner = applicationContext.getBean(CustomerRepositoryRunner.class);
        runner.run(args);

        Thread.sleep(1000);

        applicationContext.close();
    }
}

interface Tx {
}

@PropertySource("application.properties")
@Import(CustomerBeanRegistrar.class)
@ComponentScan
@Configuration
class CustomerConfiguration {

    @Bean
    CustomerRepository customerRepository(
            TransactionTemplate transactionTemplate, JdbcClient jdbcClient) {
        return new CustomerRepository(jdbcClient);

    }

    @Bean
    JdbcClient jdbcClient(DataSource dataSource) {
        return JdbcClient.create(dataSource);
    }

    @Bean
    DriverManagerDataSource dataSource(Environment environment) {
        var home1 = environment.getProperty("user.home");
        var home2 = environment.getProperty("HOME");
        IO.println(home1 + ":" + home2);
        var user = environment.getProperty("spring.datasource.username");
        var pw = environment.getProperty("spring.datasource.password");
        var url = environment.getProperty("spring.datasource.url");
        return new DriverManagerDataSource(url, user, pw);
    }


}

record Dependency(String name) {

}

@Configuration
class FooBarConfiguration {

    @Bean
    Foo foo() {
        return new Foo();
    }

    @Bean
    Bar bar() {
        var foo = this.foo();
        for (var i = 0; i < 100; i++) {
            this.foo();
            //  Assert.state(foo == this.foo(), "is the same instance");
        }
        return new Bar(foo);
    }
}

class Foo {
    Foo() {
        IO.println("foo");
    }
}

class Bar {
    final Foo foo;

    Bar(Foo foo) {
        this.foo = foo;
    }
}

@Configuration
class DependencyConfiguration {


    static final String D1 = "d1";

    @Bean
    Dependency dependency1() {
        return new Dependency("1");
    }

    @Bean
    @MyFavorite
    Dependency dependency2() {
        return new Dependency("2");
    }

    @Bean
    static MyCustomBeanDefinitionPostProcessor myCustomBeanDefinitionPostProcessor() {
        return new MyCustomBeanDefinitionPostProcessor();
    }
}

class PrintingRunner {
    PrintingRunner() {
        IO.println("PrintingRunner");
    }
}

// events
// environment

record MyEvent(Instant instant) {
}

@Component
class EventConsumer {

    @EventListener
    void on(ApplicationEvent applicationEvent) {
        IO.println("root " + applicationEvent);
    }

    @EventListener
    void on(MyEvent event) {
        IO.println("EventConsumer " + event);
    }
}

@Component
class EventProducer {

    private final ScheduledExecutorService executorService
            = Executors.newScheduledThreadPool(1);

    EventProducer(ApplicationEventPublisher eventPublisher) {
        this.executorService.schedule(() -> eventPublisher.publishEvent(new MyEvent(Instant.now())), 1, TimeUnit.SECONDS);
    }
}


// BeanDefinitions
class MyCustomBeanDefinitionPostProcessor implements
        BeanDefinitionRegistryPostProcessor,
        BeanFactoryPostProcessor {

    @Override
    public void postProcessBeanDefinitionRegistry(@NonNull BeanDefinitionRegistry registry) throws BeansException {

        var bd = BeanDefinitionBuilder
                .rootBeanDefinition(PrintingRunner.class)
                .getBeanDefinition();
        registry.registerBeanDefinition("bd", bd);

    }

    @Override
    public void postProcessBeanFactory(@NonNull ConfigurableListableBeanFactory beanFactory) throws BeansException {

        for (var beanName : beanFactory.getBeanDefinitionNames()) {
            var beanDefinition = beanFactory.getBeanDefinition(beanName);
            var type = beanFactory.getType(beanName);
            IO.println("MyCustomBeanDefinitionPostProcessor.postProcessBeanFactory: " + beanName
                    + ":" + type);
        }

    }
}

@Component
class TxBeanPostProcessor implements BeanPostProcessor {

    private final ObjectProvider<@NonNull TransactionTemplate> provider;

    TxBeanPostProcessor(ObjectProvider<@NonNull TransactionTemplate> objectFactory) {
        this.provider = objectFactory;
    }

    @Override
    public @Nullable Object postProcessAfterInitialization(
            @NonNull Object bean,
            @NonNull String beanName) throws BeansException {

        if (bean instanceof Tx tx) {
            IO.println("TxBeanPostProcessor.postProcessAfterInitialization for bean [" +
                    beanName + "]");
            return Transactions.transactional(tx, this.provider.getIfAvailable());
        }

        return bean;

    }


}

@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@Qualifier
@interface MyFavorite {

    String value() default "";

}


@Component
class Consumer implements InitializingBean, DisposableBean {

    private DataSource dataSource;

    Consumer(Dependency[] dependencies) {
        for (var dependency : dependencies) {
            IO.println(dependency.name());
        }
    }

    @PreDestroy
    void preDestroy() {
        IO.println("PreDestroy");
    }

    @PostConstruct
    void init() {
        IO.println("PostConstruct");
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        IO.println("afterPropertiesSet");
    }

    @Override
    public void destroy() throws Exception {
        IO.println("destroy");
    }
}

class CustomerBeanRegistrar implements BeanRegistrar {

    @Override
    public void register(@NonNull BeanRegistry registry, @Nullable Environment env) {


        registry.registerBean(JdbcTransactionManager.class, s -> s.supplier(
                supplierContext -> new JdbcTransactionManager(supplierContext.bean(DataSource.class))
        ));
        registry.registerBean(TransactionTemplate.class, s -> s.supplier(supplierContext -> new TransactionTemplate(supplierContext.bean(PlatformTransactionManager.class))));
    }
}


@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
@interface CoffeeSoftwareService {

    /**
     * Alias for {@link Component#value}.
     */
    @AliasFor(annotation = Component.class)
    String value() default "";

}

@Component
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

class TransactionalCustomerRepository {

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

class CustomerRepository implements Tx {

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