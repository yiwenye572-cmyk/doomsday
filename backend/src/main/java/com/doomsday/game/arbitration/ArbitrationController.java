package com.doomsday.game.arbitration;

import com.doomsday.game.arbitration.dto.ArbitrationRequest;
import com.doomsday.game.arbitration.dto.ArbitrationResult;
import com.doomsday.game.common.ApiResponse;
import com.doomsday.game.common.TraceIdSupport;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/arbitration")
public class ArbitrationController {

    private final ConflictArbitrator conflictArbitrator;

    public ArbitrationController(ConflictArbitrator conflictArbitrator) {
        this.conflictArbitrator = conflictArbitrator;
    }

    @PostMapping("/evaluate")
    public ApiResponse<ArbitrationResult> evaluate(@Valid @RequestBody ArbitrationRequest request) {
        return ApiResponse.ok(conflictArbitrator.evaluate(request), TraceIdSupport.currentTraceId());
    }
}
