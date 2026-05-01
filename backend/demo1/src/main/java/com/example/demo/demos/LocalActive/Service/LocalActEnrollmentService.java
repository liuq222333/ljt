package com.example.demo.demos.LocalActive.Service;

import com.example.demo.demos.LocalActive.DTO.LocalActEnrollmentActionRequest;
import com.example.demo.demos.LocalActive.DTO.LocalActEnrollmentActionResponse;
import com.example.demo.demos.LocalActive.DTO.LocalActEnrollmentListResponse;
import com.example.demo.demos.LocalActive.DTO.LocalActEnrollmentQuery;

public interface LocalActEnrollmentService {
    LocalActEnrollmentListResponse getUserEnrollments(LocalActEnrollmentQuery query);

    LocalActEnrollmentActionResponse enroll(Long activityId, LocalActEnrollmentActionRequest request);

    LocalActEnrollmentActionResponse cancelEnrollment(Long activityId, LocalActEnrollmentActionRequest request);
}
