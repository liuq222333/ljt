package com.example.demo.demos.LocalActive.Service;

import com.example.demo.demos.LocalActive.DTO.LocalActEnrollmentListResponse;
import com.example.demo.demos.LocalActive.DTO.LocalActEnrollmentQuery;

public interface LocalActEnrollmentService {
    LocalActEnrollmentListResponse getUserEnrollments(LocalActEnrollmentQuery query);
}
