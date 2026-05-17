package com.doomsday.game.cabin;

import com.doomsday.game.common.ApiResponse;
import com.doomsday.game.common.TraceIdSupport;
import com.doomsday.game.cabin.dto.CabinRestRequest;
import com.doomsday.game.cabin.dto.CabinRestResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/game/cabin")
public class CabinRestController {

    private final CabinRestService cabinRestService;

    public CabinRestController(CabinRestService cabinRestService) {
        this.cabinRestService = cabinRestService;
    }

    @PostMapping("/rest")
    public ApiResponse<CabinRestResponse> rest(@Valid @RequestBody CabinRestRequest request) {
        return ApiResponse.ok(cabinRestService.rest(request), TraceIdSupport.currentTraceId());
    }
}