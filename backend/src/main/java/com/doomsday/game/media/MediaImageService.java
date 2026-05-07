package com.doomsday.game.media;

import com.doomsday.game.common.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MediaImageService {

    private static final Logger log = LoggerFactory.getLogger(MediaImageService.class);
    private static final String DASHSCOPE_T2I_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/text2image/image-synthesis";

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    private final String dashscopeApiKey;
    private final String dashscopeModel;
    private final int defaultTimeoutMs;
    private final String pexelsApiKey;
    private final String pexelsBaseUrl;

    public MediaImageService(
            ObjectMapper objectMapper,
            @Value("${spring.ai.dashscope.api-key:not-configured}") String dashscopeApiKey,
            @Value("${game.media.image.dashscope-model:wanx2.1-t2i-turbo}") String dashscopeModel,
            @Value("${game.media.image.timeout-ms:3000}") int defaultTimeoutMs,
            @Value("${game.media.pexels.api-key:not-configured}") String pexelsApiKey,
            @Value("${game.media.pexels.base-url:https://api.pexels.com/v1}") String pexelsBaseUrl
    ) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
        this.dashscopeApiKey = dashscopeApiKey;
        this.dashscopeModel = dashscopeModel;
        this.defaultTimeoutMs = Math.max(1000, defaultTimeoutMs);
        this.pexelsApiKey = pexelsApiKey;
        this.pexelsBaseUrl = pexelsBaseUrl;
    }

    public GenerateImageResponse generateImage(GenerateImageRequest request) {
        long started = System.currentTimeMillis();
        int timeoutMs = resolveTimeout(request.timeoutMs());
        String preferredSource = request.preferredSource() == null
                ? "auto"
                : request.preferredSource().trim().toLowerCase();

        if ("gallery".equals(preferredSource)) {
            List<GalleryImageItem> gallery = searchGallery(request.prompt(), 1);
            if (gallery.isEmpty()) {
                throw new ApiException("INTERNAL_ERROR", "gallery search failed");
            }
            long elapsed = System.currentTimeMillis() - started;
            return new GenerateImageResponse(
                    gallery.get(0).imageUrl(),
                    "gallery",
                    false,
                    null,
                    "pexels",
                    elapsed
            );
        }

        try {
            String generatedUrl = generateWithDashScope(request.prompt(), request.style(), timeoutMs);
            long elapsed = System.currentTimeMillis() - started;
            return new GenerateImageResponse(generatedUrl, "generated", false, null, "dashscope", elapsed);
        } catch (Exception e) {
            String reason = shorten(e.getMessage(), 140);
            log.warn("[MediaImageService] dashscope generate failed, fallback to pexels: {}", reason);
            List<GalleryImageItem> gallery = searchGallery(request.prompt(), 1);
            if (gallery.isEmpty()) {
                throw new ApiException("INTERNAL_ERROR", "image generate and gallery fallback both failed");
            }
            long elapsed = System.currentTimeMillis() - started;
            return new GenerateImageResponse(gallery.get(0).imageUrl(), "gallery", true, reason, "pexels", elapsed);
        }
    }

    public List<GalleryImageItem> searchGallery(String query, int limit) {
        if (query == null || query.isBlank()) {
            throw new ApiException("BAD_REQUEST", "query is required");
        }
        if (isNotConfigured(pexelsApiKey)) {
            throw new ApiException("INTERNAL_ERROR", "PEXELS_API_KEY not configured");
        }

        int safeLimit = Math.max(1, Math.min(limit, 20));
        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = pexelsBaseUrl + "/search?query=" + encoded + "&per_page=" + safeLimit;

        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .header("Authorization", pexelsApiKey)
                    .GET()
                    .timeout(Duration.ofMillis(defaultTimeoutMs))
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 300) {
                throw new IllegalStateException("pexels status=" + resp.statusCode());
            }

            JsonNode root = objectMapper.readTree(resp.body());
            JsonNode photos = root.path("photos");
            if (!photos.isArray() || photos.isEmpty()) {
                return List.of();
            }

            List<GalleryImageItem> result = new ArrayList<>();
            for (JsonNode photo : photos) {
                String imageUrl = firstNonBlank(
                        photo.path("src").path("large2x").asText(null),
                        photo.path("src").path("large").asText(null),
                        photo.path("src").path("original").asText(null),
                        photo.path("src").path("medium").asText(null)
                );
                if (imageUrl == null) {
                    continue;
                }
                result.add(new GalleryImageItem(
                        imageUrl,
                        "pexels",
                        photo.path("photographer").asText("unknown"),
                        "Pexels License"
                ));
            }
            return result;
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException("INTERNAL_ERROR", "pexels search failed: " + shorten(e.getMessage(), 120));
        }
    }

    private String generateWithDashScope(String prompt, String style, int timeoutMs) throws Exception {
        if (isNotConfigured(dashscopeApiKey)) {
            throw new IllegalStateException("DASHSCOPE_API_KEY not configured");
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", dashscopeModel);
        payload.put("input", Map.of("prompt", prompt));

        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("size", "1024*1024");
        if (style != null && !style.isBlank()) {
            parameters.put("style", style);
        }
        payload.put("parameters", parameters);

        HttpRequest req = HttpRequest.newBuilder(URI.create(DASHSCOPE_T2I_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + dashscopeApiKey)
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                .timeout(Duration.ofMillis(timeoutMs))
                .build();

        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 300) {
            throw new IllegalStateException("dashscope status=" + resp.statusCode());
        }

        JsonNode root = objectMapper.readTree(resp.body());
        String imageUrl = firstNonBlank(
                root.path("output").path("results").isArray() && !root.path("output").path("results").isEmpty()
                        ? root.path("output").path("results").get(0).path("url").asText(null)
                        : null,
                root.path("output").path("result_url").asText(null),
                root.path("data").isArray() && !root.path("data").isEmpty()
                        ? root.path("data").get(0).path("url").asText(null)
                        : null
        );
        if (imageUrl == null) {
            throw new IllegalStateException("dashscope no image url returned");
        }
        return imageUrl;
    }

    private boolean isNotConfigured(String value) {
        return value == null || value.isBlank() || "not-configured".equals(value);
    }

    private int resolveTimeout(Integer requestTimeoutMs) {
        if (requestTimeoutMs == null) {
            return defaultTimeoutMs;
        }
        return Math.max(1000, Math.min(requestTimeoutMs, 10_000));
    }

    private String firstNonBlank(String... candidates) {
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank()) {
                return candidate;
            }
        }
        return null;
    }

    private String shorten(String text, int max) {
        if (text == null || text.length() <= max) {
            return text;
        }
        return text.substring(0, max - 1) + "...";
    }
}
