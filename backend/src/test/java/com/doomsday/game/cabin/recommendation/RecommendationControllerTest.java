package com.doomsday.game.cabin.recommendation;

import com.doomsday.game.cabin.dto.CabinUpdateResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * RecommendationController 单元测试
 *
 * 覆盖：
 *   1. GET /recommendation → 200 + READY 推荐
 *   2. POST /{recId}/accept → 200 + newVersion
 *   3. POST /{recId}/accept → 409 版本冲突
 *   4. POST /{recId}/reject → 200 + dismissed=true
 */
@WebMvcTest(RecommendationController.class)
public class RecommendationControllerTest {

    private static final String SESSION = "sess-rec-1";
    private static final String REC_ID  = "rec-uuid-001";
    private static final String BASE    = "/api/v1/game/sessions/" + SESSION + "/cabin/recommendation";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RecommendationService service;

    // ── Test 1: GET → 200 + READY ─────────────────────────────────────────
    @Test
    void testGenerate_returns200WithReady() throws Exception {
        List<RecommendedItem> items = List.of(
                new RecommendedItem("bed_01", "bed", 192, 160, 128, 64, 0)
        );
        LayoutRecommendationResponse resp = new LayoutRecommendationResponse(
                REC_ID, SESSION, items, "低体力→床置中央", 0.75, "READY");
        when(service.generate(SESSION)).thenReturn(resp);

        mockMvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.status").value("READY"))
                .andExpect(jsonPath("$.data.recommendationId").value(REC_ID))
                .andExpect(jsonPath("$.data.confidence").value(0.75))
                .andExpect(jsonPath("$.data.items[0].type").value("bed"));
    }

    // ── Test 2: POST accept → 200 ─────────────────────────────────────────
    @Test
    void testAccept_success_returns200() throws Exception {
        CabinUpdateResponse updateResp = new CabinUpdateResponse(SESSION, 1L, "{}");
        when(service.accept(eq(SESSION), eq(REC_ID), eq(0L))).thenReturn(updateResp);

        mockMvc.perform(post(BASE + "/" + REC_ID + "/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("expectedVersion", 0))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.newVersion").value(1))
                .andExpect(jsonPath("$.data.conflict").value(false));
    }

    // ── Test 3: POST accept → 409 冲突 ─────────────────────────────────────
    @Test
    void testAccept_conflict_returns409() throws Exception {
        CabinUpdateResponse conflictResp = CabinUpdateResponse.conflict(SESSION, 5L, "{}");
        when(service.accept(eq(SESSION), eq(REC_ID), anyLong())).thenReturn(conflictResp);

        mockMvc.perform(post(BASE + "/" + REC_ID + "/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("expectedVersion", 0))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT_VERSION"));
    }

    // ── Test 4: POST reject → 200 + dismissed ────────────────────────────
    @Test
    void testReject_returns200Dismissed() throws Exception {
        doNothing().when(service).reject(SESSION, REC_ID);

        mockMvc.perform(post(BASE + "/" + REC_ID + "/reject")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.dismissed").value(true));
    }
}
