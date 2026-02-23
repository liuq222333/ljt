package com.example.demo.demos.LocalActive.Service;

import com.example.demo.demos.LocalActive.DTO.LocalActScheduleTemplateRequest;
import com.example.demo.demos.LocalActive.DTO.LocalActScheduleTemplateResponse;

public interface LocalScheduleTemplateService {
    LocalActScheduleTemplateResponse createTemplate(LocalActScheduleTemplateRequest request);
}
