package com.doomsday.game.diary;

import com.doomsday.game.common.ApiResponse;
import com.doomsday.game.common.TraceIdSupport;
import com.doomsday.game.diary.dto.DiaryEntryView;
import com.doomsday.game.diary.dto.DiaryLevel;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/game/sessions")
public class GameDiaryController {

    private final GameDiaryService gameDiaryService;

    public GameDiaryController(GameDiaryService gameDiaryService) {
        this.gameDiaryService = gameDiaryService;
    }

    @GetMapping("/{sessionId}/diary")
    public ApiResponse<List<DiaryEntryView>> getDiary(
            @PathVariable String sessionId,
            @RequestParam(defaultValue = "L0") DiaryLevel level,
            @RequestParam(required = false) Integer fromTurn,
            @RequestParam(required = false) Integer toTurn) {
        return ApiResponse.ok(
                gameDiaryService.queryDiary(sessionId, level, fromTurn, toTurn),
                TraceIdSupport.currentTraceId()
        );
    }
}
