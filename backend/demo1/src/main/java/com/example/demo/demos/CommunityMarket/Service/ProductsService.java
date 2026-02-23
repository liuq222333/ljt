package com.example.demo.demos.CommunityMarket.Service;

import com.example.demo.demos.CommunityMarket.DTO.ProductAndSellerQueryDTO;
import com.example.demo.demos.CommunityMarket.Pojo.Product;
import com.example.demo.demos.CommunityMarket.Pojo.ProductImages;
import com.example.demo.demos.PageResponse.PageResponse;
import com.example.demo.demos.CommunityMarket.DTO.ProductQueryDTO;
import com.example.demo.demos.generic.Resp;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface ProductsService {

    List<Product> getAllProducts();

    PageResponse<Product> getProducts(ProductQueryDTO query);

    ResponseEntity<Product> getProductById(Long id);

    ResponseEntity<ProductAndSellerQueryDTO> getProductAndSeller(Integer id);

    ResponseEntity<List<ProductImages>> getProductImage(Integer id);

    Resp<Void> addProduct(Product product);

    org.springframework.http.ResponseEntity<java.util.List<com.example.demo.demos.CommunityMarket.Pojo.Product>> getMyProducts(String userName);

    com.example.demo.demos.generic.Resp<Void> takeDownProduct(Long productId);

    com.example.demo.demos.generic.Resp<Void> increaseStock(Long productId, Integer delta);

    com.example.demo.demos.generic.Resp<Void> updatePrice(Long productId, java.math.BigDecimal price);

    com.example.demo.demos.generic.Resp<Void> updateLocation(Long productId, String location);

    org.springframework.http.ResponseEntity<java.util.List<com.example.demo.demos.CommunityMarket.DTO.ProductNearbyDTO>> getNearbyProducts(
            double lat,
            double lng,
            java.lang.Double radiusKm,
            java.lang.Integer limit,
            java.lang.Integer offset,
            java.lang.Integer categoryId,
            java.math.BigDecimal minPrice,
            java.math.BigDecimal maxPrice,
            java.lang.String keyword
    );
}
