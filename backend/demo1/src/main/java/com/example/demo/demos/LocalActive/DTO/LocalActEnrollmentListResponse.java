package com.example.demo.demos.LocalActive.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class LocalActEnrollmentListResponse {
    private List<LocalActEnrollmentItem> items;
    private LocalActEnrollmentStats stats;
}
