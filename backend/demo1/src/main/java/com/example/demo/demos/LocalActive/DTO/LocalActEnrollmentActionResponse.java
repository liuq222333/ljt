package com.example.demo.demos.LocalActive.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LocalActEnrollmentActionResponse {
    private Long enrollmentId;
    private String status;
    private Integer waitlistRank;
}
