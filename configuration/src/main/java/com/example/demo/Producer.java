package com.example.demo;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
class Producer {

    private final ApplicationEventPublisher publisher;

    private final ScheduledExecutorService service =
            Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());

    Producer(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
        this.service.schedule(new Runnable() {
            @Override
            public void run() {
                publisher.publishEvent(new MyApplicationEvent(Instant.now()));
            }
        }, 1, TimeUnit.SECONDS);
    }


}
