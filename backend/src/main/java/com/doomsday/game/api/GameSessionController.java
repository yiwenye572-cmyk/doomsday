package com.doomsday.game.api;

import com.doomsday.game.common.ApiResponse;
import com.doomsday.game.common.TraceIdSupport;
import com.doomsday.game.domain.GameSessionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/game/sessions")
public class GameSessionController {

    private final GameSessionService service;

    public GameSessionController(GameSessionService service) {
        this.service = service;
    }

    @PostMapping
    public ApiResponse<CreateSessionResponse> createSession(@Valid @RequestBody CreateSessionRequest request) {
        return ApiResponse.ok(service.createSession(request), traceId());
    }

    @GetMapping("/{sessionId}/state")
    public ApiResponse<SessionStateResponse> getState(@PathVariable String sessionId) {
        return ApiResponse.ok(service.getState(sessionId), traceId());
    }

    @PostMapping("/{sessionId}/turns")
    public ApiResponse<SubmitTurnResponse> submitTurn(
            @PathVariable String sessionId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody SubmitTurnRequest request
    ) {
        return ApiResponse.ok(service.submitTurn(sessionId, idempotencyKey, request), traceId());
    }

    @PostMapping("/{sessionId}/turns/{turn}/choose")
    public ApiResponse<ChooseOptionResponse> chooseOption(
            @PathVariable String sessionId,
            @PathVariable int turn,
            @Valid @RequestBody ChooseOptionRequest request
    ) {
        return ApiResponse.ok(service.chooseOption(sessionId, turn, request), traceId());
    }

    @PostMapping("/{sessionId}/comeback-card")
    public ApiResponse<ComebackCardResponse> useComebackCard(
            @PathVariable String sessionId,
            @Valid @RequestBody ComebackCardRequest request
    ) {
        return ApiResponse.ok(service.useComebackCard(sessionId, request), traceId());
    }

    @GetMapping("/{sessionId}/replay")
    public ApiResponse<String> replay(
            @PathVariable String sessionId,
            @RequestParam(required = false) Integer fromTurn,
            @RequestParam(required = false) Integer toTurn
    ) {
        return ApiResponse.ok(service.getReplay(sessionId, fromTurn, toTurn), traceId());
    }

    private String traceId() {
        return TraceIdSupport.currentTraceId();
    }
}
