package com.example.demo.demos.LocalActive.Service;

import com.example.demo.demos.LocalActive.DTO.LocalActCreateRequest;
import com.example.demo.demos.LocalActive.DTO.LocalActCreateResponse;

public interface LocalActivityService {
    LocalActCreateResponse createActivity(LocalActCreateRequest request);

    java.util.List<com.example.demo.demos.LocalActive.Pojo.LocalActivity> listActivities(String status, int page, int size);
}
