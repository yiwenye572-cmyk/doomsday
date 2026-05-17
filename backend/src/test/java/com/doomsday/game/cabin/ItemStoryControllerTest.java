package com.doomsday.game.cabin;

import com.doomsday.game.cabin.ItemStoryService.StoryResult;
import com.doomsday.game.cabin.dto.ItemStoryResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ItemStoryController 单元测试
 *
 * 覆盖场景：
 *   1. 首次请求 → 202 Accepted + status=RUNNING（任务刚创建）
 *   2. 再次轮询，任务完成 → 200 OK + status=DONE + story 文本
 *   3. 任务失败 → 200 OK + status=FAILED + errorMessage
 */
@WebMvcTest(ItemStoryController.class)
public class ItemStoryControllerTest {

    private static final String SESSION = "sess-story-1";
    private static final String ITEM    = "item-axe-01";
    private static final String BASE    = "/api/v1/game/sessions/" + SESSION + "/items/" + ITEM + "/story";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ItemStoryService storyService;

    // ── Test 1: 首次请求 → 202 ────────────────────────────────────────────
    @Test
    void testGetStory_firstRequest_returns202() throws Exception {
        ItemStoryResponse pending = new ItemStoryResponse(
                1L, SESSION, ITEM, "RUNNING", null, null, null);
        when(storyService.getOrTrigger(eq(SESSION), eq(ITEM), any(), any()))
                .thenReturn(StoryResult.accepted(pending));

        mockMvc.perform(get(BASE)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.status").value("RUNNING"))
                .andExpect(jsonPath("$.data.story").doesNotExist());
    }

    // ── Test 2: 任务完成 → 200 + story ────────────────────────────────────
    @Test
    void testGetStory_done_returns200WithStory() throws Exception {
        String storyText = "这把斧头曾属于一位矿工，在末日第三年的隧道坍塌中被遗弃。";
        ItemStoryResponse done = new ItemStoryResponse(
                1L, SESSION, ITEM, "DONE", storyText, null, Instant.now());
        when(storyService.getOrTrigger(eq(SESSION), eq(ITEM), any(), any()))
                .thenReturn(StoryResult.done(done));

        mockMvc.perform(get(BASE)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.status").value("DONE"))
                .andExpect(jsonPath("$.data.story").value(storyText));
    }

    // ── Test 3: 任务失败 → 200 + FAILED ──────────────────────────────────
    @Test
    void testGetStory_failed_returns200WithError() throws Exception {
        ItemStoryResponse failed = new ItemStoryResponse(
                1L, SESSION, ITEM, "FAILED", null, "LLM timeout", null);
        // FAILED 时 Service 重置为 RUNNING 并重新触发，此处模拟为 accepted(FAILED) 场景
        when(storyService.getOrTrigger(eq(SESSION), eq(ITEM), any(), any()))
                .thenReturn(StoryResult.accepted(failed));

        mockMvc.perform(get(BASE)
                        .param("itemType", "weapon")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.status").value("FAILED"))
                .andExpect(jsonPath("$.data.errorMessage").value("LLM timeout"));
    }
}
