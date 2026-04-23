package com.example.demo.demos.realtime.gateway;

import com.example.demo.demos.common.enums.RealtimeStatus;
import com.example.demo.demos.realtime.model.RealtimeBatchQueryRequest;
import com.example.demo.demos.realtime.model.RealtimeBatchQueryResponse;
import com.example.demo.demos.realtime.model.RealtimeQueryRequest;
import com.example.demo.demos.realtime.model.RealtimeQueryResponse;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InternalRealtimeGatewayServiceTest {

    @Test
    void queryShouldDelegateProductToImplementedProvider() {
        StubProvider product = new StubProvider("product", true, RealtimeStatus.SUCCESS);
        InternalRealtimeGatewayService service = service(product, new StoreRealtimeProvider(), new EventRealtimeProvider());

        RealtimeQueryResponse response = service.query(request("product", 11L));

        assertEquals(RealtimeStatus.SUCCESS, response.getRealtimeStatus());
        assertEquals("product", response.getQueryMeta().get("provider"));
        assertEquals("internal", response.getQueryMeta().get("gatewayMode"));
    }

    @Test
    void queryShouldReturnPlaceholderForStoreProvider() {
        StubProvider product = new StubProvider("product", true, RealtimeStatus.SUCCESS);
        InternalRealtimeGatewayService service = service(product, new StoreRealtimeProvider(), new EventRealtimeProvider());

        RealtimeQueryResponse response = service.query(request("store", 22L));

        assertEquals(RealtimeStatus.DEGRADED, response.getRealtimeStatus());
        assertEquals(Boolean.FALSE, response.getQueryMeta().get("implemented"));
        assertEquals("provider_not_implemented", response.getQueryMeta().get("reason"));
        assertEquals(Collections.singletonList(22L), response.getPartialFailedIds());
    }

    @Test
    void queryShouldFailForUnsupportedEntityType() {
        StubProvider product = new StubProvider("product", true, RealtimeStatus.SUCCESS);
        InternalRealtimeGatewayService service = service(product, new StoreRealtimeProvider(), new EventRealtimeProvider());

        RealtimeQueryResponse response = service.query(request("coupon", 33L));

        assertEquals(RealtimeStatus.FAILED, response.getRealtimeStatus());
        assertEquals("unsupported_entity_type", response.getQueryMeta().get("reason"));
    }

    @Test
    void healthShouldExposeProviderReadiness() {
        StubProvider product = new StubProvider("product", true, RealtimeStatus.SUCCESS);
        InternalRealtimeGatewayService service = service(product, new StoreRealtimeProvider(), new EventRealtimeProvider());

        Map<String, Object> health = service.health();

        assertEquals("UP", health.get("status"));
        assertEquals("internal", health.get("gatewayMode"));
        assertEquals(Boolean.TRUE, health.get("productProviderReady"));
        assertEquals(Boolean.FALSE, health.get("storeProviderReady"));
        assertEquals(Boolean.FALSE, health.get("eventProviderReady"));
    }

    @Test
    void batchQueryShouldAggregateResponses() {
        StubProvider product = new StubProvider("product", true, RealtimeStatus.SUCCESS);
        InternalRealtimeGatewayService service = service(product, new StoreRealtimeProvider(), new EventRealtimeProvider());
        RealtimeBatchQueryRequest request = new RealtimeBatchQueryRequest();
        request.setRequests(Arrays.asList(request("product", 11L), request("event", 77L)));

        RealtimeBatchQueryResponse response = service.batchQuery(request);

        assertEquals(2, response.getResults().size());
        assertEquals(2, response.getBatchMeta().get("requestCount"));
        assertEquals("internal", response.getBatchMeta().get("gatewayMode"));
    }

    private InternalRealtimeGatewayService service(RealtimeEntityProvider... providers) {
        return new InternalRealtimeGatewayService(new RealtimeProviderRegistry(Arrays.asList(providers)));
    }

    private RealtimeQueryRequest request(String entityType, Long id) {
        RealtimeQueryRequest request = new RealtimeQueryRequest();
        request.setEntityType(entityType);
        request.setEntityIds(Collections.singletonList(id));
        request.setQueryType("availability");
        return request;
    }

    private static final class StubProvider implements RealtimeEntityProvider {

        private final String entityType;
        private final boolean implemented;
        private final RealtimeStatus status;

        private StubProvider(String entityType, boolean implemented, RealtimeStatus status) {
            this.entityType = entityType;
            this.implemented = implemented;
            this.status = status;
        }

        @Override
        public String getEntityType() {
            return entityType;
        }

        @Override
        public RealtimeQueryResponse query(RealtimeQueryRequest request) {
            RealtimeQueryResponse response = new RealtimeQueryResponse();
            response.setRealtimeStatus(status);
            response.getQueryMeta().put("implemented", implemented);
            return response;
        }

        @Override
        public Map<String, Object> health() {
            Map<String, Object> result = new LinkedHashMap<String, Object>();
            result.put("implemented", implemented);
            return result;
        }
    }
}
