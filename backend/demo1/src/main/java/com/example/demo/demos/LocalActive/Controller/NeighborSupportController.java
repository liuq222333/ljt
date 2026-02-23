package com.example.demo.demos.LocalActive.Controller;

import com.example.demo.demos.LocalActive.DTO.NeighborSupportTaskDTO;
import com.example.demo.demos.LocalActive.DTO.NeighborSupportTaskRequest;
import com.example.demo.demos.LocalActive.DTO.NeighborSupportTaskResponse;
import com.example.demo.demos.LocalActive.Service.NeighborSupportService;
import com.example.demo.demos.generic.Resp;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/neighbor-support")
@RequiredArgsConstructor
public class NeighborSupportController {

    private final NeighborSupportService neighborSupportService;

    @Operation(summary = "发布邻里互助任务")
    @PostMapping("/tasks")
    public Resp<NeighborSupportTaskResponse> createTask(@RequestBody NeighborSupportTaskRequest request) {
        return Resp.success(neighborSupportService.createTask(request));
    }

    @Operation(summary = "查询邻里互助任务列表")
    @GetMapping("/tasks")
    public Resp<List<NeighborSupportTaskDTO>> listTasks(
            @RequestParam(value = "status", required = false) String status) {
        return Resp.success(neighborSupportService.listTasks(status));
    }
}
