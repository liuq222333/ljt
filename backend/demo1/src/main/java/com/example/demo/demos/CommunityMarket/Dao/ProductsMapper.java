package com.example.demo.demos.CommunityMarket.Dao;

import com.example.demo.demos.CommunityMarket.DTO.ProductAndSellerQueryDTO;
import com.example.demo.demos.CommunityMarket.Pojo.Product;
import com.example.demo.demos.CommunityMarket.DTO.ProductQueryDTO;
import com.example.demo.demos.CommunityMarket.Pojo.ProductImages;
import com.example.demo.demos.CommunityMarket.Pojo.UserProducts;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProductsMapper {
    List<Product> getAllProducts();

    /**
     * 动态筛选商品（使用 XML 实现，传入 DTO）
     */
    List<Product> getProducts(ProductQueryDTO query);

    Product getProductById(@Param("id") Long id);

    ProductAndSellerQueryDTO getProductAndSellerById(Integer id);

    List<ProductImages> getProductImage(Integer id);

    int addProduct(Product product);

    void addToMyProducts(UserProducts userProducts);

    List<Product> getProductsBySellerId(@Param("sellerId") Long sellerId);

    int takeDownProduct(@Param("id") Long id, @Param("status") String status);

    int increaseStock(@Param("id") Long id, @Param("delta") Integer delta);

    int updatePrice(@Param("id") Long id, @Param("price") java.math.BigDecimal price);

    int updateLocation(@Param("id") Long id, @Param("location") String location);

    int decreaseStockAndMaybeDown(@Param("id") Long id, @Param("delta") Integer delta);

    int setStockZero(@Param("id") Long id);

    java.util.List<com.example.demo.demos.CommunityMarket.DTO.ProductNearbyDTO> getNearbyProducts(
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("radiusKm") Double radiusKm,
            @Param("limit") Integer limit,
            @Param("offset") Integer offset,
            @Param("categoryId") Integer categoryId,
            @Param("minPrice") java.math.BigDecimal minPrice,
            @Param("maxPrice") java.math.BigDecimal maxPrice,
            @Param("keyword") String keyword
    );

    int updateCoordinates(@Param("id") Long id, @Param("latitude") Double latitude, @Param("longitude") Double longitude);
}
