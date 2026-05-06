package com.doomsday.game.diary;

import com.doomsday.game.common.ApiResponse;
import com.doomsday.game.common.TraceIdSupport;
import com.doomsday.game.diary.dto.ForceSummarizeRequest;
import com.doomsday.game.diary.dto.ForceSummarizeResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/diary")
public class AdminDiaryController {

    private final GameDiaryService gameDiaryService;

    public AdminDiaryController(GameDiaryService gameDiaryService) {
        this.gameDiaryService = gameDiaryService;
    }

    @PostMapping("/force-summarize")
    public ApiResponse<ForceSummarizeResponse> forceSummarize(@Valid @RequestBody ForceSummarizeRequest request) {
        GameDiaryService.SummaryResult result = gameDiaryService.summarizeRange(
                request.sessionId(),
                request.fromTurn(),
                request.toTurn(),
                "MANUAL"
        );
        return ApiResponse.ok(
                new ForceSummarizeResponse(
                        result.sessionId(),
                        result.created(),
                        result.fromTurn(),
                        result.toTurn(),
                        result.level(),
                        result.summary()),
                TraceIdSupport.currentTraceId()
        );
    }
}
