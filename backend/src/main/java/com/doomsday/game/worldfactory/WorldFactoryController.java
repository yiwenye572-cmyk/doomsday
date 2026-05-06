package com.doomsday.game.worldfactory;

import com.doomsday.game.common.ApiResponse;
import com.doomsday.game.common.TraceIdSupport;
import com.doomsday.game.worldfactory.dto.WorldFactoryJobRequest;
import com.doomsday.game.worldfactory.dto.WorldFactoryJobResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/world-factory")
public class WorldFactoryController {

    private final WorldFactoryJobService worldFactoryJobService;

    public WorldFactoryController(WorldFactoryJobService worldFactoryJobService) {
        this.worldFactoryJobService = worldFactoryJobService;
    }

    @PostMapping("/jobs")
    public ApiResponse<WorldFactoryJobResponse> createJob(@Valid @RequestBody WorldFactoryJobRequest request) {
        return ApiResponse.ok(worldFactoryJobService.createJob(request), TraceIdSupport.currentTraceId());
    }

    @GetMapping("/jobs/{jobId}")
    public ApiResponse<WorldFactoryJobResponse> getJob(@PathVariable String jobId) {
        return ApiResponse.ok(worldFactoryJobService.getJob(jobId), TraceIdSupport.currentTraceId());
    }
}
