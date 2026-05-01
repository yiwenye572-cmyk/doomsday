package com.doomsday.game.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class GameSessionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldCreateSessionAndGetState() throws Exception {
        String createReq = """
                {
                  "playerId": "u_1001",
                  "difficulty": "SURVIVOR",
                  "worldVersion": "world_v1",
                  "styleProfile": "grim_realism"
                }
                """;

        MvcResult createResult = mockMvc.perform(post("/api/v1/game/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createReq))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andReturn();

        String body = createResult.getResponse().getContentAsString();
        String sessionId = body.split("\\\"sessionId\\\":\\\"")[1].split("\\\"")[0];

        mockMvc.perform(get("/api/v1/game/sessions/{sessionId}/state", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionId").value(sessionId))
                .andExpect(jsonPath("$.data.version").value(1));
    }

    @Test
    void shouldSubmitTurnAndReturnFixed4Options() throws Exception {
        String createReq = """
                {
                  "playerId": "u_1002",
                  "difficulty": "SURVIVOR",
                  "worldVersion": "world_v1",
                  "styleProfile": "grim_realism"
                }
                """;

        MvcResult createResult = mockMvc.perform(post("/api/v1/game/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createReq))
                .andExpect(status().isOk())
                .andReturn();

        String body = createResult.getResponse().getContentAsString();
        String sessionId = body.split("\\\"sessionId\\\":\\\"")[1].split("\\\"")[0];

        String turnReq = """
                {
                  "expectedVersion": 1,
                  "playerInput": "我尝试绕到加油站后门搜刮药品",
                  "clientTime": 1760000000123
                }
                """;

        mockMvc.perform(post("/api/v1/game/sessions/{sessionId}/turns", sessionId)
                        .header("Idempotency-Key", "idem-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(turnReq))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.options.length()").value(4))
                .andExpect(jsonPath("$.data.plot.text").exists());
    }
}
