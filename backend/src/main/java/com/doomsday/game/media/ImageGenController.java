package com.doomsday.game.media;

import com.doomsday.game.common.ApiResponse;
import com.doomsday.game.common.TraceIdSupport;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 通义万象 wanx-v1 图片生成接口
 * POST /api/v1/game/image/generate  → 提交任务，返回 taskId
 * GET  /api/v1/game/image/task/{taskId} → 轮询任务状态，SUCCEEDED 后返回图片 URL
 */
@RestController
@RequestMapping("/api/v1/game/image")
public class ImageGenController {

    private final ImageGenService imageGenService;

    public ImageGenController(ImageGenService imageGenService) {
        this.imageGenService = imageGenService;
    }

    /**
     * 提交文生图任务
     * Body: { "prompt": "...", "type": "cabin_bg" | "item", "style": "anime|realistic" }
     */
    @PostMapping("/generate")
    public ApiResponse<ImageGenResponse> generate(@RequestBody Map<String, String> body) {
        String prompt = body.getOrDefault("prompt", "");
        String type   = body.getOrDefault("type", "cabin_bg");
        if (prompt.isBlank()) {
            return ApiResponse.fail("BAD_REQUEST", "prompt 不能为空", null,
                    TraceIdSupport.currentTraceId());
        }
        ImageGenResponse resp = imageGenService.submitTask(prompt, type);
        return ApiResponse.ok(resp, TraceIdSupport.currentTraceId());
    }

    /**
     * 查询任务状态（前端轮询）
     * 返回: { taskId, status: PENDING|RUNNING|SUCCEEDED|FAILED, imageUrl }
     */
    @GetMapping("/task/{taskId}")
    public ApiResponse<ImageGenResponse> getTask(@PathVariable String taskId) {
        ImageGenResponse resp = imageGenService.queryTask(taskId);
        return ApiResponse.ok(resp, TraceIdSupport.currentTraceId());
    }
}
