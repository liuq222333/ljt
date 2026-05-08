package com.example.demo.demos.LocalActive.Controller;

import com.example.demo.demos.LocalActive.DTO.LocalActMediaUploadResponse;
import com.example.demo.demos.LocalActive.Service.LocalActMediaService;
import com.example.demo.demos.generic.Resp;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/local-act/media")
@RequiredArgsConstructor
public class LocalActMediaController {

    private final LocalActMediaService mediaService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Resp<LocalActMediaUploadResponse> upload(@RequestPart("file") MultipartFile file,
                                                    @RequestParam(value = "scene", defaultValue = "activity") String scene) {
        return Resp.success(mediaService.uploadCover(file, scene));
    }
}
