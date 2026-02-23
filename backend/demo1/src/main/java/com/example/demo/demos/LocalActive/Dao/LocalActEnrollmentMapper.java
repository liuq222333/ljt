package com.example.demo.demos.LocalActive.Dao;

import com.example.demo.demos.LocalActive.DTO.LocalActEnrollmentQuery;
import com.example.demo.demos.LocalActive.DTO.LocalActEnrollmentRecord;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface LocalActEnrollmentMapper {
    List<LocalActEnrollmentRecord> findEnrollments(LocalActEnrollmentQuery query);
}
