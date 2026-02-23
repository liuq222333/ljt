package com.example.demo.demos.LocalActive.Dao;

import com.example.demo.demos.LocalActive.DTO.NeighborSupportTaskDTO;
import com.example.demo.demos.LocalActive.Pojo.NeighborSupportTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface NeighborSupportTaskMapper {
    int insertTask(NeighborSupportTask task);

    List<NeighborSupportTaskDTO> listTasks(@Param("status") String status);
}
