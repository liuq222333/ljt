package com.example.demo.demos.LocalActive.Service;

import com.example.demo.demos.LocalActive.DTO.NeighborSupportTaskDTO;
import com.example.demo.demos.LocalActive.DTO.NeighborSupportTaskRequest;
import com.example.demo.demos.LocalActive.DTO.NeighborSupportTaskResponse;
import java.util.List;

public interface NeighborSupportService {
    NeighborSupportTaskResponse createTask(NeighborSupportTaskRequest request);

    List<NeighborSupportTaskDTO> listTasks(String status);
}
