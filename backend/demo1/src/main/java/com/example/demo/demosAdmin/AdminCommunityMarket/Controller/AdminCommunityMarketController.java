package com.example.demo.demosAdmin.AdminCommunityMarket.Controller;

import com.example.demo.demosAdmin.AdminCommunityMarket.Service.AdminCommunityMarketService;
import com.example.demo.demos.generic.Resp;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/market/products")
public class AdminCommunityMarketController {

    @Autowired
    private AdminCommunityMarketService adminCommunityMarketService;

    @Operation(summary = "管理员删除商品")
    @DeleteMapping("/{id}")
    public Resp<Void> deleteProduct(@PathVariable Long id) {
        try {
            adminCommunityMarketService.deleteProduct(id);
            return Resp.success();
        } catch (DataIntegrityViolationException e) {
            return Resp.error("该商品存在关联数据（如订单、购物车等），无法直接删除。建议先处理关联数据。");
        } catch (Exception e) {
            return Resp.error("删除商品失败: " + e.getMessage());
        }
    }
}
