package com.example.demo.demosAdmin.AdminActivity.Service;

import com.example.demo.demos.LocalActive.Pojo.LocalActivity;

import java.util.List;

public interface AdminActivityService {
    List<LocalActivity> listReviews(String status, String keyword, int page, int size);

    Long approve(Long adminActivityId, String note);

    void reject(Long adminActivityId, String note);
}

