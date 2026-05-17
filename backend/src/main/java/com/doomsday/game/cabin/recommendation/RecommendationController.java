package com.doomsday.game.cabin.recommendation;

import com.doomsday.game.cabin.dto.CabinUpdateResponse;
import com.doomsday.game.common.ApiResponse;
import com.doomsday.game.common.TraceIdSupport;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 布局推荐 Controller
 *
 * 路径前缀：/api/v1/game/sessions/{sessionId}/cabin/recommendation
 *
 * GET    /              → 生成并返回推荐布局
 * POST   /{recId}/accept → 接受推荐，应用到小屋状态
 * POST   /{recId}/reject → 拒绝推荐
 */
@RestController
@RequestMapping("/api/v1/game/sessions/{sessionId}/cabin/recommendation")
public class RecommendationController {

    private final RecommendationService service;

    public RecommendationController(RecommendationService service) {
        this.service = service;
    }

    /** GET：生成推荐布局 */
    @GetMapping
    public ApiResponse<LayoutRecommendationResponse> generate(@PathVariable String sessionId) {
        return ApiResponse.ok(service.generate(sessionId), TraceIdSupport.currentTraceId());
    }

    /**
     * POST /{recId}/accept：接受并应用推荐。
     * 请求体：{ "expectedVersion": long }
     * 成功 → 200；版本冲突 → 409
     */
    @PostMapping("/{recId}/accept")
    public ResponseEntity<ApiResponse<CabinUpdateResponse>> accept(
            @PathVariable String sessionId,
            @PathVariable String recId,
            @RequestBody Map<String, Object> body) {

        long expectedVersion = body.containsKey("expectedVersion")
                ? ((Number) body.get("expectedVersion")).longValue() : 0L;

        CabinUpdateResponse resp = service.accept(sessionId, recId, expectedVersion);
        if (resp.isConflict()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.fail("CONFLICT_VERSION", resp.getConflictMessage(),
                            resp, TraceIdSupport.currentTraceId()));
        }
        return ResponseEntity.ok(ApiResponse.ok(resp, TraceIdSupport.currentTraceId()));
    }

    /** POST /{recId}/reject：拒绝推荐 */
    @PostMapping("/{recId}/reject")
    public ApiResponse<Map<String, Boolean>> reject(
            @PathVariable String sessionId,
            @PathVariable String recId) {
        service.reject(sessionId, recId);
        return ApiResponse.ok(Map.of("dismissed", true), TraceIdSupport.currentTraceId());
    }
}
