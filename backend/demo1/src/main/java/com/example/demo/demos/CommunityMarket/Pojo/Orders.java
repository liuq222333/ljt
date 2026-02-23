package com.example.demo.demos.CommunityMarket.Pojo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Orders {
    private Long id;
    private String orderId;
    private Long userId;
    private Integer productId;
    private java.math.BigDecimal totalAmount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer status;
    private LocalDateTime deliveryTime;
    private LocalDateTime paymentTime;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
    private LocalDateTime completedTime;
    private String cancelReason;
    private LocalDateTime cancelledAt;
    private String remark;
}
