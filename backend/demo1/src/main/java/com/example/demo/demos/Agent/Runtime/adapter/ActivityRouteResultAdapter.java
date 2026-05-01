package com.example.demo.demos.Agent.Runtime.adapter;

import com.example.demo.demos.Agent.Entity.ApiRoute;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Locale;

@Component
@Order(10)
public class ActivityRouteResultAdapter extends AbstractRouteResultAdapter {

    @Override
    public boolean supports(ApiRoute route) {
        if (route == null) {
            return false;
        }
        if ("event".equalsIgnoreCase(route.getEntityType())) {
            return true;
        }
        String resource = route.getResource();
        return StringUtils.hasText(resource)
                && (resource.toLowerCase(Locale.ROOT).contains("activity")
                || resource.toLowerCase(Locale.ROOT).contains("event"));
    }
}
