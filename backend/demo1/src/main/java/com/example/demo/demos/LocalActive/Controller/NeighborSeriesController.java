package com.example.demo.demos.LocalActive.Controller;

import com.example.demo.demos.LocalActive.DTO.LocalStoryDetail;
import com.example.demo.demos.LocalActive.DTO.LocalStoryListItem;
import com.example.demo.demos.LocalActive.DTO.LocalStoryCreateRequest;
import com.example.demo.demos.LocalActive.Service.NeighborSeriesService;
import com.example.demo.demos.generic.Resp;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/local-act/stories")
@RequiredArgsConstructor
public class NeighborSeriesController {

    private final NeighborSeriesService neighborSeriesService;

    @GetMapping
    public Resp<List<LocalStoryListItem>> listStories(@RequestParam(value = "keyword", required = false) String keyword,
                                                      @RequestParam(value = "visibility", required = false) String visibility,
                                                      @RequestParam(value = "page", defaultValue = "1") int page,
                                                      @RequestParam(value = "size", defaultValue = "10") int size) {
        return Resp.success(neighborSeriesService.listStories(keyword, visibility, page, size));
    }

    @GetMapping("/{id}")
    public Resp<LocalStoryDetail> getStory(@PathVariable("id") Long id) {
        return Resp.success(neighborSeriesService.getStoryDetail(id));
    }

    @PostMapping
    public Resp<Long> createStory(@RequestBody LocalStoryCreateRequest request) {
        return Resp.success(neighborSeriesService.createStory(request));
    }
}
