package com.example.demo.demos.CommunityMarket.Service.Impl;

import com.example.demo.demos.CommunityMarket.Dao.ProductsMapper;
import com.example.demo.demos.CommunityMarket.DTO.ProductQueryDTO;
import com.example.demo.demos.CommunityMarket.Pojo.Product;
import com.example.demo.demos.PageResponse.PageResponse;
import com.example.demo.demos.generic.Resp;
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
        assertEquals(1L, resp.getItems().get(0).getId());
    }

    @Test
    void addProductShouldRejectMissingCategoryBeforeMapperInsert() {
        Product product = new Product();
        product.setSellerId(1);
        product.setTitle("苹果手机");
        product.setPrice(new java.math.BigDecimal("4000"));
        product.setStockQuantity(1);
        product.setLocation("山东省临沂市");
        product.setImageUrls("[\"https://img.example.com/a.jpg\"]");

        Resp<Void> response = productsService.addProduct(product);

        assertNotNull(response);
        assertEquals(400, response.getCode());
        assertEquals("商品分类不能为空", response.getMessage());
        verify(productsMapper, never()).addProduct(any(Product.class));
    }

    @Test
    void addProductShouldDefaultConditionToBrandNew() {
        Product product = new Product();
        product.setSellerId(1);
        product.setCategoryId(15);
        product.setTitle("苹果手机");
        product.setPrice(new java.math.BigDecimal("4000"));
        product.setStockQuantity(1);
        product.setLocation("山东省临沂市");
        product.setImageUrls("[\"https://img.example.com/a.jpg\"]");

        when(productsMapper.addProduct(any(Product.class))).thenReturn(0);

        Resp<Void> response = productsService.addProduct(product);

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productsMapper).addProduct(captor.capture());
        assertEquals("全新", captor.getValue().getCondition());
        assertEquals(500, response.getCode());
        assertEquals("添加商品失败", response.getMessage());
    }
}
