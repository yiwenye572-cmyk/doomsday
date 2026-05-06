package com.doomsday.game.worldfactory;

import com.doomsday.game.common.ApiResponse;
import com.doomsday.game.common.TraceIdSupport;
import com.doomsday.game.worldfactory.dto.DefaultWorldResponse;
import com.doomsday.game.worldfactory.dto.GameWorldInitRequest;
import com.doomsday.game.worldfactory.dto.GameWorldInitResponse;
import com.doomsday.game.worldfactory.dto.WorldFactoryJobResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/game/worlds")
public class GameWorldController {

    private final WorldFactoryJobService worldFactoryJobService;

    public GameWorldController(WorldFactoryJobService worldFactoryJobService) {
        this.worldFactoryJobService = worldFactoryJobService;
    }

    @PostMapping("/initialize")
    public ApiResponse<GameWorldInitResponse> initializeWorld(@Valid @RequestBody GameWorldInitRequest request) {
        WorldFactoryJobResponse job = worldFactoryJobService.createFromBasicProfile(request);
        GameWorldInitResponse response = new GameWorldInitResponse(
                job.worldVersion(),
                job.jobId(),
                job.status(),
                "world factory job accepted"
        );
        return ApiResponse.ok(response, TraceIdSupport.currentTraceId());
    }

    @GetMapping("/default")
    public ApiResponse<DefaultWorldResponse> defaultWorld() {
        String version = worldFactoryJobService.defaultWorldVersion();
        DefaultWorldResponse response = new DefaultWorldResponse(
                version,
                "默认世界书",
                "玩家跳过创建时使用该默认世界书，保障开局可玩。"
        );
        return ApiResponse.ok(response, TraceIdSupport.currentTraceId());
    }
}
