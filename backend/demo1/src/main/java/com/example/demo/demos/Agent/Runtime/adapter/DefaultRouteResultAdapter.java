package com.example.demo.demos.Agent.Runtime.adapter;

import com.example.demo.demos.Agent.Entity.ApiRoute;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(100)
public class DefaultRouteResultAdapter extends AbstractRouteResultAdapter {

    @Override
    public boolean supports(ApiRoute route) {
        return true;
    }
}
