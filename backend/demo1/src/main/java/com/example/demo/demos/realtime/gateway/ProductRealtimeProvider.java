package com.example.demo.demos.realtime.gateway;

import com.example.demo.demos.realtime.model.RealtimeQueryRequest;
import com.example.demo.demos.realtime.model.RealtimeQueryResponse;
import com.example.demo.demos.realtime.service.ProductRealtimeFallbackService;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class ProductRealtimeProvider implements RealtimeEntityProvider {

    private final ProductRealtimeFallbackService productRealtimeFallbackService;

    public ProductRealtimeProvider(ProductRealtimeFallbackService productRealtimeFallbackService) {
        this.productRealtimeFallbackService = productRealtimeFallbackService;
    }

    @Override
    public String getEntityType() {
        return "product";
    }

    @Override
    public RealtimeQueryResponse query(RealtimeQueryRequest request) {
        return productRealtimeFallbackService.queryProvider(request, request.getEntityIds(), "internal_product_provider");
    }

    @Override
    public Map<String, Object> health() {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("implemented", true);
        result.put("entityType", getEntityType());
        result.put("directSource", "products_and_search_snapshot");
        return result;
    }
}
