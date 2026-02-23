package com.example.demo.demos.LocalActive.Service;

import com.example.demo.demos.LocalActive.DTO.LocalStoryDetail;
import com.example.demo.demos.LocalActive.DTO.LocalStoryListItem;
import com.example.demo.demos.LocalActive.DTO.LocalStoryCreateRequest;

import java.util.List;

public interface NeighborSeriesService {
    List<LocalStoryListItem> listStories(String keyword, String visibility, int page, int size);

    LocalStoryDetail getStoryDetail(Long id);

    Long createStory(LocalStoryCreateRequest request);
}
