package com.example.demo.demosAdmin.AdminNeighborSupportTask.Service;

import com.example.demo.demos.LocalActive.Pojo.NeighborSupportTask;

import java.util.List;

public interface AdminNeighborSupportTaskService {
    List<NeighborSupportTask> listReviews(String status, String keyword, int page, int size);

    Long approve(Long adminTaskId);

    void reject(Long adminTaskId);
}
