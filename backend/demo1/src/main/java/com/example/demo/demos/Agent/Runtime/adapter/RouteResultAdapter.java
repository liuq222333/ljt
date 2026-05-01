package com.example.demo.demos.Agent.Runtime.adapter;

import com.example.demo.demos.Agent.Entity.ApiRoute;
import com.example.demo.demos.Agent.Runtime.NormalizedRouteData;
import com.example.demo.demos.Agent.Service.BackendApiProxyService;

public interface RouteResultAdapter {

    boolean supports(ApiRoute route);

    NormalizedRouteData adapt(ApiRoute route, BackendApiProxyService.InvocationResult result);
}
