package com.example.demo.demos.CommunityMarket.Controller;

import com.example.demo.demos.CommunityMarket.DTO.CartShowDTO;
import com.example.demo.demos.CommunityMarket.DTO.PurchaseRequest;
import com.example.demo.demos.CommunityMarket.Pojo.Cart;
import com.example.demo.demos.CommunityMarket.Pojo.Orders;
import com.example.demo.demos.CommunityMarket.Service.CartService;
import com.example.demo.demos.generic.Resp;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/carts")
public class CartController {
    @Autowired
    private CartService cartService;

    @Operation(summary="添加到购物车")
    @PostMapping("/add")
    public Resp<Void> addToCart(@RequestBody Cart cart) {
        boolean success = cartService.addToCart(cart);
        if (success) {
            return Resp.success();
        } else {
            return Resp.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "添加到购物车失败");
        }
    }

    @Operation(summary="获取某用户购物车中的商品")
    @RequestMapping("/getCartItems")
    public ResponseEntity<List<CartShowDTO>> getCartItems(@RequestParam(value = "userName") String userName){
        return cartService.getCartItems(userName);
    }

    @Operation(summary="删除购物车中的商品")
    @DeleteMapping("/deleteCartItem")
    public Resp<Void> deleteCartItem(@RequestParam(value = "userName") String userName,
                                     @RequestParam(value = "productId") int productId){
        return cartService.deleteCartItem(userName, productId);
    }
    @Operation(summary="购买购物车中的商品，添加到我的订单")
    @PostMapping("/buyCartItems")
    public Resp<Void> buyCartItems(@RequestParam(value = "userName") String userName,
                                   @RequestParam(value = "productId") int productId,
                                   @RequestParam(value = "quantity") int quantity){
        return cartService.buyCartItems(userName, productId,quantity);
    }

    @Operation(summary="批量购买购物车选中商品并扣减库存")
    @PostMapping("/buySelected")
    public Resp<Void> buySelected(@RequestBody PurchaseRequest request) {
        return cartService.buySelected(request);
    }

    @Operation(summary="获取我的订单列表")
    @GetMapping("/orders")
    public ResponseEntity<List<Orders>> getOrders(@RequestParam(value = "userName") String userName) {
        return cartService.getOrders(userName);
    }
    
}
