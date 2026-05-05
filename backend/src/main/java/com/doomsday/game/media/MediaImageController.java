package com.doomsday.game.media;

import com.doomsday.game.common.ApiResponse;
import com.doomsday.game.common.TraceIdSupport;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/media/images")
public class MediaImageController {

    private final MediaImageService mediaImageService;

    public MediaImageController(MediaImageService mediaImageService) {
        this.mediaImageService = mediaImageService;
    }

    @PostMapping("/generate")
    public ApiResponse<GenerateImageResponse> generate(@Valid @RequestBody GenerateImageRequest request) {
        return ApiResponse.ok(mediaImageService.generateImage(request), TraceIdSupport.currentTraceId());
    }

    @GetMapping("/gallery-search")
    public ApiResponse<List<GalleryImageItem>> gallerySearch(
            @RequestParam("q") String query,
            @RequestParam(defaultValue = "5") int limit
    ) {
        int safeLimit = Math.max(1, Math.min(limit, 20));
        return ApiResponse.ok(mediaImageService.searchGallery(query, safeLimit), TraceIdSupport.currentTraceId());
    }
}
