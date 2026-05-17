package com.doomsday.game.cabin;

import com.doomsday.game.cabin.ItemStoryService.StoryResult;
import com.doomsday.game.cabin.dto.ItemStoryResponse;
import com.doomsday.game.common.ApiResponse;
import com.doomsday.game.common.TraceIdSupport;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 物品叙事 Controller
 * 路径：GET /api/v1/game/sessions/{sessionId}/items/{itemId}/story
 *
 * 查询参数（可选）：
 *   itemType     物品类型（初次触发时传入，影响 RAG 查询词）
 *   itemMetadata 物品元数据 JSON（初次触发时传入）
 *
 * 响应语义：
 *   202 Accepted  → 任务已创建或正在生成（PENDING/RUNNING）
 *   200 OK        → 叙事已生成（DONE）
 */
@RestController
@RequestMapping("/api/v1/game/sessions/{sessionId}/items/{itemId}")
public class ItemStoryController {

    private final ItemStoryService storyService;

    public ItemStoryController(ItemStoryService storyService) {
        this.storyService = storyService;
    }

    @GetMapping("/story")
    public ResponseEntity<ApiResponse<ItemStoryResponse>> getStory(
            @PathVariable String sessionId,
            @PathVariable String itemId,
            @RequestParam(required = false) String itemType,
            @RequestParam(required = false) String itemMetadata) {

        StoryResult result = storyService.getOrTrigger(sessionId, itemId, itemType, itemMetadata);

        if (result.accepted()) {
            // 202: 任务刚创建 / 仍在生成中
            return ResponseEntity
                    .status(HttpStatus.ACCEPTED)
                    .body(ApiResponse.ok(result.response(), TraceIdSupport.currentTraceId()));
        }
        // 200: 叙事已就绪
        return ResponseEntity.ok(ApiResponse.ok(result.response(), TraceIdSupport.currentTraceId()));
    }
}
