package com.doomsday.game.cabin;

import com.doomsday.game.cabin.ItemStoryEntity.Status;
import com.doomsday.game.common.LlmTokenEstimator;
import com.doomsday.game.common.RetryExecutor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 物品叙事异步 Worker（RAG + LLM）
 *
 * 流程：
 *   1. 以 "物品类型 + 世界末日" 作为查询词，从 pgvector 语义检索相关 Lorebook 片段
 *   2. 组装 Prompt（Few-shot 模板 + 检索片段 + 物品元数据）
 *   3. 调用 DashScope qwen-plus 生成 100-200 字来源叙事
 *   4. 将结果写回 item_story 表（DONE），失败时写 FAILED + errorMessage
 *
 * 并发安全：调用前 Service 已将 status 置为 RUNNING（CAS），Worker 不会重复触发同一任务。
 */
@Component
public class ItemStoryWorker {

    private static final Logger log = LoggerFactory.getLogger(ItemStoryWorker.class);

    private static final int RAG_TOP_K = 4;
    private static final double RAG_THRESHOLD = 0.50;

    /**
     * 系统提示：简短物品叙事，100-200 字，末日风格，杜绝幻觉。
     */
    private static final String SYSTEM_PROMPT = """
            你是末日废土世界的物品档案员。
            规则：
            1. 为给定物品撰写 100-200 字的"来源叙事"，描述它在废土中的历史与意义。
            2. 只使用【背景资料】中出现的设定，不可凭空添加新地名/人名/事件。
            3. 使用第三人称，文风克制、有质感，末日小说风格。
            4. 结尾用一句话概括该物品对玩家的象征意义。
            5. 不输出任何标题或分段标记，只输出纯叙事文本。
            """;

    private final ItemStoryRepository itemStoryRepository;
    private final VectorStore vectorStore;
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    public ItemStoryWorker(ItemStoryRepository itemStoryRepository,
                           VectorStore vectorStore,
                           ChatClient chatClient,         // @Primary mainChatClient
                           ObjectMapper objectMapper) {
        this.itemStoryRepository = itemStoryRepository;
        this.vectorStore = vectorStore;
        this.chatClient = chatClient;
        this.objectMapper = objectMapper;
    }

    /**
     * 异步触发叙事生成。由 ItemStoryService 在任务入库后调用。
     * @param taskId item_story.id（数据库主键）
     */
    @Async
    public void generateStory(Long taskId) {
        ItemStoryEntity task = itemStoryRepository.findById(taskId).orElse(null);
        if (task == null || task.getStatus() != Status.RUNNING) {
            log.warn("[ItemStoryWorker] taskId={} not found or not RUNNING, skip", taskId);
            return;
        }

        log.info("[ItemStoryWorker] taskId={} sessionId={} itemId={} START",
                taskId, task.getSessionId(), task.getItemId());
        long start = System.currentTimeMillis();

        try {
            // ── Step 1: RAG 语义检索 ─────────────────────────────────────
            String ragQuery = buildRagQuery(task);
            List<Document> docs = List.of();
            try {
                docs = RetryExecutor.run(2, () -> vectorStore.similaritySearch(
                        SearchRequest.builder()
                                .query(ragQuery)
                                .topK(RAG_TOP_K)
                                .similarityThreshold(RAG_THRESHOLD)
                                .build()
                ));
            } catch (Exception e) {
                log.warn("[ItemStoryWorker] taskId={} RAG failed, proceeding without context: {}", taskId, e.getMessage());
            }

            List<String> citations = docs.stream()
                    .map(d -> (String) d.getMetadata().getOrDefault("source", "lorebook") + ":" + d.getId())
                    .collect(Collectors.toList());

            // ── Step 2: 组装 Prompt ─────────────────────────────────────
            String userPrompt = buildUserPrompt(task, docs);

            // ── Step 3: 调用 LLM ─────────────────────────────────────────
            int promptTokens = LlmTokenEstimator.estimatePromptTokens(SYSTEM_PROMPT + "\n" + userPrompt);
            String storyText = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(userPrompt)
                    .call()
                    .content();
            int completionTokens = LlmTokenEstimator.estimateCompletionTokens(storyText);

            log.info("[ItemStoryWorker] taskId={} generated {}ms, promptTokens={}, completionTokens={}",
                    taskId, System.currentTimeMillis() - start, promptTokens, completionTokens);

            // ── Step 4: 持久化结果 ──────────────────────────────────────
            task.setStoryText(storyText.trim());
            task.setRagCitations(objectMapper.writeValueAsString(citations));
            task.setStatus(Status.DONE);
            itemStoryRepository.save(task);

        } catch (Exception e) {
            log.error("[ItemStoryWorker] taskId={} FAILED: {}", taskId, e.getMessage(), e);
            task.setStatus(Status.FAILED);
            task.setErrorMessage(truncate(e.getMessage(), 500));
            itemStoryRepository.save(task);
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────

    private String buildRagQuery(ItemStoryEntity task) {
        // 物品类型 + 末日废土背景，提升向量召回相关性
        String type = task.getItemType() != null ? task.getItemType() : "物品";
        return type + " 废土 来源 历史 末日生存";
    }

    private String buildUserPrompt(ItemStoryEntity task, List<Document> docs) {
        StringBuilder sb = new StringBuilder();

        // 物品信息
        sb.append("【物品信息】\n");
        sb.append("类型: ").append(task.getItemType() != null ? task.getItemType() : "unknown").append("\n");
        sb.append("ID: ").append(task.getItemId()).append("\n");
        if (task.getItemMetadata() != null && !task.getItemMetadata().isBlank()) {
            sb.append("元数据: ").append(task.getItemMetadata()).append("\n");
        }
        sb.append("\n");

        // RAG 检索到的背景资料
        if (!docs.isEmpty()) {
            sb.append("【背景资料】（来自世界观知识库，请严格依据以下内容撰写）\n");
            for (int i = 0; i < docs.size(); i++) {
                sb.append(i + 1).append(". ").append(docs.get(i).getText()).append("\n");
            }
            sb.append("\n");
        } else {
            // 无 RAG 结果时给出基础世界观
            sb.append("【背景资料】\n这是一个核战争后的废土世界，文明崩溃，幸存者在废墟中挣扎求生。\n\n");
        }

        sb.append("请根据以上信息，为该物品撰写来源叙事。");
        return sb.toString();
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max);
    }
}
