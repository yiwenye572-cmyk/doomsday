package com.doomsday.game.cabin;

import com.doomsday.game.common.ApiResponse;
import com.doomsday.game.common.TraceIdSupport;
import com.doomsday.game.cabin.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 小屋模块 REST Controller
 * 路径前缀：/api/v1/game/sessions/{sessionId}/cabin
 *
 * P0 接口：
 *   GET    /           - 获取小屋状态（含 version）
 *   POST   /update     - 提交物品变更（Redis Lua CAS；409 = 版本冲突）
 *   POST   /rest       - 休息触发（体力/时间推进）
 *   POST   /take       - 出门携带物品
 *   POST   /return     - 归来放入待整理区
 */
@RestController
@RequestMapping("/api/v1/game/sessions/{sessionId}/cabin")
public class CabinController {

    private final CabinService cabinService;
    private final CabinRestService cabinRestService;

    public CabinController(CabinService cabinService, CabinRestService cabinRestService) {
        this.cabinService = cabinService;
        this.cabinRestService = cabinRestService;
    }

    /** GET: 获取当前小屋状态 */
    @GetMapping
    public ApiResponse<CabinStateResponse> getCabinState(@PathVariable String sessionId) {
        return ApiResponse.ok(cabinService.getState(sessionId), TraceIdSupport.currentTraceId());
    }

    /**
     * POST /update：提交物品变更（Lua CAS 并发控制）
     * 成功 → 200 + newVersion
     * 版本冲突 → 409 + 最新 state（前端自行合并或刷新）
     */
    @PostMapping("/update")
    public ResponseEntity<ApiResponse<CabinUpdateResponse>> updateCabinState(
            @PathVariable String sessionId,
            @Valid @RequestBody CabinUpdateRequest request) {
        CabinUpdateResponse resp = cabinService.updateState(sessionId, request);
        if (resp.isConflict()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.fail("CONFLICT_VERSION", resp.getConflictMessage(), resp, TraceIdSupport.currentTraceId()));
        }
        return ResponseEntity.ok(ApiResponse.ok(resp, TraceIdSupport.currentTraceId()));
    }

    /** POST /rest：休息（体力恢复 + 时间推进） */
    @PostMapping("/rest")
    public ApiResponse<CabinRestResponse> rest(
            @PathVariable String sessionId,
            @Valid @RequestBody CabinRestRequest request) {
        request.setSessionId(sessionId);
        return ApiResponse.ok(cabinRestService.rest(request), TraceIdSupport.currentTraceId());
    }

    /** POST /take：出门携带物品，返回携带快照 */
    @PostMapping("/take")
    public ApiResponse<CabinStateResponse> take(
            @PathVariable String sessionId,
            @Valid @RequestBody CabinTakeRequest request) {
        return ApiResponse.ok(cabinService.takeItems(sessionId, request), TraceIdSupport.currentTraceId());
    }

    /** POST /return：归来放入待整理区，触发故事异步生成（可 202） */
    @PostMapping("/return")
    public ApiResponse<CabinStateResponse> returnItems(
            @PathVariable String sessionId,
            @Valid @RequestBody CabinReturnRequest request) {
        return ApiResponse.ok(cabinService.returnItems(sessionId, request), TraceIdSupport.currentTraceId());
    }
}
