package com.example.demo.demos.CommunityMarket.Service.Impl;

import com.example.demo.demos.CommunityMarket.DTO.CartShowDTO;
import com.example.demo.demos.CommunityMarket.DTO.PurchaseItem;
import com.example.demo.demos.CommunityMarket.DTO.PurchaseRequest;
import com.example.demo.demos.CommunityMarket.Dao.CartMapper;
import com.example.demo.demos.CommunityMarket.Dao.ProductsMapper;
import com.example.demo.demos.CommunityMarket.Pojo.Cart;
import com.example.demo.demos.CommunityMarket.Pojo.Orders;
import com.example.demo.demos.CommunityMarket.Pojo.Product;
import com.example.demo.demos.CommunityMarket.Service.CartService;
import com.example.demo.demos.Login.Entity.User;
import com.example.demo.demos.Login.Mapper.LoginMapper;
import com.example.demo.demos.generic.Resp;
import com.example.demo.demos.Notification.Pojo.NotificationMessage;
import com.example.demo.demos.Notification.Service.NotificationSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
public class CartServiceImpl implements CartService {
    @Autowired
    private CartMapper cartMapper;
    @Autowired
    private LoginMapper loginMapper;
    @Autowired
    private ProductsMapper productsMapper;

    @Autowired
    private NotificationSender notificationSender;


    @Override
    @Transactional
    public boolean addToCart(Cart cart) {
        try {
            // 查询当前用户该商品是否已在购物车
            Cart existingCartItem = cartMapper.getCartItemByUserNameAndProductId(cart.getUserName(), cart.getProductId());

            if (existingCartItem != null) {
                // 已存在则累加数量并更新
                int newQuantity = existingCartItem.getQuantity() + cart.getQuantity();
                existingCartItem.setQuantity(newQuantity);
                int updatedRows = cartMapper.updateCart(existingCartItem);
                return updatedRows > 0;
            } else {
                // 不存在则设置创建时间（价格由前端传入）
                cart.setCreatedAt(LocalDateTime.now());
                Product product = productsMapper.getProductById(cart.getProductId());
                cart.setPrice(product.getPrice());

                int insertedRows = cartMapper.addToCart(cart);
                return insertedRows > 0;
            }
        } catch (Exception e) {
            log.error("添加商品到购物车时发生异常", e);
            return false;
        }
    }

    @Override
    public ResponseEntity<List<CartShowDTO>> getCartItems(String userName) {
        List<CartShowDTO> cartItems = cartMapper.getCartItems(userName);
        if (cartItems == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(cartItems);
    }

    @Override
    public Resp<Void> deleteCartItem(String userName, int productId) {
        int rows = cartMapper.deleteCartItem(userName, Long.valueOf(productId));
        if (rows > 0) {
            return Resp.success();
        }
        return Resp.error(500, "删除购物车失败");
    }

    @Override
    @Transactional
    public Resp<Void> buyCartItems(String userName, int productId, int quantity) {
        Product product = productsMapper.getProductById(Long.valueOf(productId));
        if (product == null) {
            return Resp.error(404, "商品不存在");
        }
        java.math.BigDecimal totalPrice = product.getPrice().multiply(java.math.BigDecimal.valueOf(quantity));
        User user = loginMapper.getUserByName(userName);
        if (user == null) {
            return Resp.error(404, "用户不存在");
        }
        
        String timeSuffix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        String orderId = user.getUserId() + String.valueOf(productId) + timeSuffix;
        Orders orders = new Orders();
        orders.setOrderId(orderId);
        orders.setUserId(Long.valueOf(user.getUserId()));
        orders.setReceiverName(userName);
        orders.setReceiverPhone(user.getPhone());
        orders.setReceiverAddress(user.getAddress());
        orders.setProductId(productId);
        orders.setTotalAmount(totalPrice);
        orders.setCreatedAt(LocalDateTime.now());
        orders.setUpdatedAt(LocalDateTime.now());
        orders.setCancelReason(null);
        orders.setRemark(null);
        orders.setStatus(1);
        orders.setCompletedTime(null);
        orders.setCancelledAt(null);
        int affected = productsMapper.decreaseStockAndMaybeDown(Long.valueOf(productId), quantity);
        if (affected <= 0) {
            return Resp.error(500, "商品已售罄");
        }
        int rows;
        try {
            rows = cartMapper.buyCartItems(orders);
        } catch (DuplicateKeyException ex) {
            String fallbackId = orders.getOrderId() + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
            orders.setOrderId(fallbackId);
            rows = cartMapper.buyCartItems(orders);
        }
        if (rows <= 0) {
            throw new RuntimeException("订单创建失败");
        }
        cartMapper.deleteCartItem(userName, Long.valueOf(productId));
        try {
            NotificationMessage toBuyer = new NotificationMessage();
            toBuyer.setKind("ORDER_CREATED");
            toBuyer.setTitle("下单成功");
            String pTitle = product.getTitle() == null ? "" : product.getTitle();
            toBuyer.setContent("已为您创建订单：" + orders.getOrderId() + "，商品：" + pTitle + "，数量：" + quantity);
            toBuyer.setTargetType("USER");
            toBuyer.setTargetUserId(Long.valueOf(user.getUserId()));
            toBuyer.setPriority(5);
            notificationSender.send(toBuyer);

            if (product.getSellerId() != null) {
                NotificationMessage toSeller = new NotificationMessage();
                toSeller.setKind("PRODUCT_PURCHASED");
                toSeller.setTitle("商品已被购买");
                String buyer = userName == null ? "" : userName;
                String p = product.getTitle() == null ? "" : product.getTitle();
                toSeller.setContent("您的商品【" + p + "】被用户" + buyer + "购买，数量：" + quantity);
                toSeller.setTargetType("USER");
                toSeller.setTargetUserId(Long.valueOf(product.getSellerId()));
                toSeller.setPriority(4);
                notificationSender.send(toSeller);
            }
        } catch (Exception ignore) {}
        return Resp.success();
    }

    @Transactional
    @Override
    public Resp<Void> buySelected(PurchaseRequest request) {
        // 检查参数
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            return Resp.error(400, "无有效购买项");
        }
        // 逐个处理
        String userName = request.getUserName();
        for (PurchaseItem it : request.getItems()) {
            if (it == null) continue;
            int pid = it.getProductId() == null ? 0 : it.getProductId();
            int qty = it.getQuantity() == null ? 0 : it.getQuantity();
            if (pid <= 0 || qty <= 0) continue;
            Resp<Void> r = buyCartItems(userName, pid, qty);
            if (r.getCode() != 200) {
                return r;
            }
        }
        return Resp.success();
    }

    @Override
    public ResponseEntity<List<Orders>> getOrders(String userName) {
        User user = loginMapper.getUserByName(userName);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        List<Orders> list = cartMapper.getOrdersByUserId(user.getUserId());
        return ResponseEntity.ok(list == null ? java.util.Collections.emptyList() : list);
    }
}
