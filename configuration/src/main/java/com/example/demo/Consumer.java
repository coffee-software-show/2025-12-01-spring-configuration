package com.example.demo;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
class Consumer {

    @EventListener
    void onEvent(MyApplicationEvent event) {
        IO.println("MyApplicationEvent#onEvent()");
    }
}
