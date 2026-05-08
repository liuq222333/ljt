package com.example.demo.demos.LocalActive.Service;

import com.example.demo.demos.LocalActive.DTO.LocalActCreateRequest;
import com.example.demo.demos.LocalActive.DTO.LocalActCreateResponse;
import com.example.demo.demos.LocalActive.DTO.LocalActivityDetail;

public interface LocalActivityService {
    LocalActCreateResponse createActivity(LocalActCreateRequest request);

    java.util.List<com.example.demo.demos.LocalActive.Pojo.LocalActivity> listActivities(String status,
                                                                                         String timeState,
                                                                                         int page,
                                                                                         int size);

    java.util.List<com.example.demo.demos.LocalActive.Pojo.LocalActivity> listMyActivities(String username,
                                                                                           String status,
                                                                                           String timeState,
                                                                                           int page,
                                                                                           int size);

    java.util.List<com.example.demo.demos.LocalActive.Pojo.LocalActivity> listFavoriteActivities(String username,
                                                                                                 int page,
                                                                                                 int size);

    LocalActivityDetail getActivityDetail(Long id, String username);

    void favoriteActivity(Long id, String username);

    void unfavoriteActivity(Long id, String username);
}
