package com.doomsday.game.cabin;

import com.doomsday.game.common.ApiResponse;
import com.doomsday.game.common.TraceIdSupport;
import com.doomsday.game.cabin.dto.CabinStateRequest;
import com.doomsday.game.cabin.dto.CabinStateResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/game/cabin")
public class CabinController {

    private final CabinService cabinService;

    public CabinController(CabinService cabinService) {
        this.cabinService = cabinService;
    }

    @PostMapping("/state")
    public ApiResponse<CabinStateResponse> updateCabinState(@Valid @RequestBody CabinStateRequest request) {
        return ApiResponse.ok(cabinService.updateState(request), TraceIdSupport.currentTraceId());
    }

    @GetMapping("/state/{sessionId}")
    public ApiResponse<CabinStateResponse> getCabinState(@PathVariable String sessionId) {
        return ApiResponse.ok(cabinService.getState(sessionId), TraceIdSupport.currentTraceId());
    }
}