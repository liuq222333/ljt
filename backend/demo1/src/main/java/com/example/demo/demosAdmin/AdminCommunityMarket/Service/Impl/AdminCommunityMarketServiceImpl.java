package com.example.demo.demosAdmin.AdminCommunityMarket.Service.Impl;

import com.example.demo.demosAdmin.AdminCommunityMarket.Dao.AdminCommunityMarketMapper;
import com.example.demo.demosAdmin.AdminCommunityMarket.Service.AdminCommunityMarketService;
import com.example.demo.demos.CommunityMarket.Dao.ProductsMapper;
import com.example.demo.demos.CommunityMarket.Pojo.Product;
import com.example.demo.demos.Notification.Pojo.NotificationMessage;
import com.example.demo.demos.Notification.Service.NotificationSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminCommunityMarketServiceImpl implements AdminCommunityMarketService {

    @Autowired
    private AdminCommunityMarketMapper adminCommunityMarketMapper;

    @Autowired
    private ProductsMapper productsMapper;

    @Autowired
    private NotificationSender notificationSender;

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        Product product = productsMapper.getProductById(id);
        int rows = adminCommunityMarketMapper.deleteProduct(id);
        if (rows > 0 && product != null && product.getSellerId() != null) {
            sendDeleteNotification(product);
        }
    }

    private void sendDeleteNotification(Product product) {
        try {
            NotificationMessage msg = new NotificationMessage();
            msg.setKind("SYSTEM_NOTICE");
            msg.setTitle("商品删除通知");
            msg.setContent("您的商品 \"" + product.getTitle() + "\" 已被管理员删除。");
            msg.setTargetType("USER");
            msg.setTargetUserId(Long.valueOf(product.getSellerId()));
            msg.setPriority(1);
            notificationSender.send(msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
