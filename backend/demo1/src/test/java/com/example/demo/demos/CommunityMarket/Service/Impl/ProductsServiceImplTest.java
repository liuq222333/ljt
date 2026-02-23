package com.example.demo.demos.CommunityMarket.Service.Impl;

import com.example.demo.demos.CommunityMarket.Dao.ProductsMapper;
import com.example.demo.demos.CommunityMarket.DTO.ProductQueryDTO;
import com.example.demo.demos.CommunityMarket.Pojo.Product;
import com.example.demo.demos.PageResponse.PageResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProductsServiceImplTest {

    @Mock
    private ProductsMapper productsMapper;

    @InjectMocks
    private ProductsServiceImpl productsService;

    @Test
    void testDefaultsAndNormalizationWhenParamsMissing() {
        // Arrange: missing params -> expect defaults
        ProductQueryDTO query = new ProductQueryDTO();
        when(productsMapper.getProducts(any(ProductQueryDTO.class))).thenReturn(Collections.emptyList());

        // Act
        PageResponse<Product> resp = productsService.getProducts(query);

        // Assert: mapper called with normalized sort/order
        ArgumentCaptor<ProductQueryDTO> captor = ArgumentCaptor.forClass(ProductQueryDTO.class);
        verify(productsMapper, times(1)).getProducts(captor.capture());
        ProductQueryDTO passed = captor.getValue();
        assertEquals("comprehensive", passed.getSort());
        assertEquals("desc", passed.getOrder());

        // Assert: response shape is valid
        assertNotNull(resp);
        assertNotNull(resp.getItems());
        assertEquals(0, resp.getItems().size());
        assertTrue(resp.getTotal() >= 0);
    }

    @Test
    void testKeepValidSortAndOrder() {
        // Arrange: provide valid sort/order
        ProductQueryDTO query = new ProductQueryDTO();
        query.setKeyword("手机");
        query.setSort("price");
        query.setOrder("asc");

        Product p = new Product();
        p.setId(1L);
        p.setTitle("测试商品");
        when(productsMapper.getProducts(any(ProductQueryDTO.class))).thenReturn(Collections.singletonList(p));

        // Act
        PageResponse<Product> resp = productsService.getProducts(query);

        // Assert: mapper received normalized (should keep provided values)
        ArgumentCaptor<ProductQueryDTO> captor = ArgumentCaptor.forClass(ProductQueryDTO.class);
        verify(productsMapper, times(1)).getProducts(captor.capture());
        ProductQueryDTO passed = captor.getValue();
        assertEquals("price", passed.getSort());
        assertEquals("asc", passed.getOrder());

        // Assert: response contains our stub item
        assertNotNull(resp);
        assertEquals(1, resp.getItems().size());
        assertEquals("1", resp.getItems().get(0).getId());
    }
}
