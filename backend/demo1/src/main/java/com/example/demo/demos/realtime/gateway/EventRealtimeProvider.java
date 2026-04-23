package com.example.demo.demos.realtime.gateway;

import org.springframework.stereotype.Component;

@Component
public class EventRealtimeProvider extends AbstractPlaceholderRealtimeProvider {

    @Override
    public String getEntityType() {
        return "event";
    }
}
