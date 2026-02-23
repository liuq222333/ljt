package com.example.demo.demos.CommunityMarket.Dao;

import com.example.demo.demos.CommunityMarket.DTO.CartShowDTO;
import com.example.demo.demos.CommunityMarket.Pojo.Cart;
import com.example.demo.demos.CommunityMarket.Pojo.Orders;
import com.example.demo.demos.generic.Resp;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CartMapper {
    int addToCart(Cart cart);

    Cart getCartItemByUserNameAndProductId(@Param("userName") String userName,
                                           @Param("productId") Long productId);

    int updateCart(Cart cart);

    List<CartShowDTO> getCartItems(@Param("userName") String userName);

    int deleteCartItem(@Param("userName") String userName, @Param("productId") Long productId);

    int buyCartItems(Orders orders);

    List<Orders> getOrdersByUserId(@Param("userId") String userId);
}
