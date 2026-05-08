package com.example.demo.demos.LocalActive.Service;

import com.example.demo.demos.LocalActive.DTO.LocalActMediaUploadResponse;
import org.springframework.web.multipart.MultipartFile;

public interface LocalActMediaService {
    LocalActMediaUploadResponse uploadCover(MultipartFile file, String scene);

    String resolveCoverUrl(String storedValue);
}
