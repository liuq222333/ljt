package com.example.demo.demos.CommunityMarket.Service;

import com.example.demo.demos.CommunityMarket.DTO.CartShowDTO;
import com.example.demo.demos.CommunityMarket.DTO.PurchaseRequest;
import com.example.demo.demos.CommunityMarket.Pojo.Cart;
import com.example.demo.demos.CommunityMarket.Pojo.Orders;
import com.example.demo.demos.generic.Resp;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface CartService {
    boolean addToCart(Cart cart);

    ResponseEntity<List<CartShowDTO>> getCartItems(String userName);

    Resp<Void> deleteCartItem(String userName, int productId);

    Resp<Void> buyCartItems(String userName, int productId,int quantity);

    Resp<Void> buySelected(PurchaseRequest request);

    ResponseEntity<List<Orders>> getOrders(String userName);
}
