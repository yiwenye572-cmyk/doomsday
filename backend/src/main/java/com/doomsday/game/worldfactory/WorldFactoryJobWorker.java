package com.doomsday.game.worldfactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class WorldFactoryJobWorker {

    private static final Logger log = LoggerFactory.getLogger(WorldFactoryJobWorker.class);

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public WorldFactoryJobWorker(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public void enqueue(String jobId, String worldVersion, WorldSourceType sourceType, String rawContent, boolean forceRebuild) {
        executor.submit(() -> process(jobId, worldVersion, sourceType, rawContent, forceRebuild));
    }

    void process(String jobId, String worldVersion, WorldSourceType sourceType, String rawContent, boolean forceRebuild) {
        try {
            updateJob(jobId, "RUNNING", 5, "CHUNKING", null);
            List<String> chunks = chunk(rawContent);
            saveChunks(worldVersion, chunks);

            updateJob(jobId, "RUNNING", 35, "EXTRACT", null);
            saveEntities(worldVersion, rawContent);

            updateJob(jobId, "RUNNING", 60, "TAGGING", null);
            tagChunks(worldVersion);

            updateJob(jobId, "RUNNING", 80, "INDEX", null);
            indexToOnlineTables(worldVersion, chunks, forceRebuild);

            updateJob(jobId, "DONE", 100, "DONE", null);
            log.info("[WorldFactory] job done jobId={} worldVersion={} sourceType={}", jobId, worldVersion, sourceType);
        } catch (Exception e) {
            log.error("[WorldFactory] job failed jobId={} err={}", jobId, e.getMessage(), e);
            updateJob(jobId, "FAILED", 100, "FAILED", e.getMessage());
        }
    }

    private List<String> chunk(String content) {
        if (content == null || content.isBlank()) {
            return List.of("默认世界书：资源稀缺，风险高，行动需谨慎。", "安全区、加油站、地铁废墟构成核心场景。", "玩家需在噪声、感染与补给之间平衡。");
        }
        String normalized = content.replace("\r", "");
        String[] blocks = normalized.split("\n\n+");
        List<String> chunks = new ArrayList<>();
        for (String block : blocks) {
            String t = block.trim();
            if (!t.isBlank()) {
                chunks.add(t.length() > 400 ? t.substring(0, 400) : t);
            }
            if (chunks.size() >= 24) {
                break;
            }
        }
        if (chunks.isEmpty()) {
            chunks.add(normalized.length() > 400 ? normalized.substring(0, 400) : normalized);
        }
        return chunks;
    }

    private void saveChunks(String worldVersion, List<String> chunks) throws JsonProcessingException {
        jdbcTemplate.update("DELETE FROM world_chunk WHERE world_version = ?", worldVersion);
        int seq = 1;
        for (String chunk : chunks) {
            String tagsJson = objectMapper.writeValueAsString(extractTags(chunk));
            jdbcTemplate.update("""
                    INSERT INTO world_chunk(world_version, seq_no, chunk_text, tags_json)
                    VALUES (?, ?, ?, CAST(? AS jsonb))
                    """, worldVersion, seq++, chunk, tagsJson);
        }
    }

    private void saveEntities(String worldVersion, String content) throws JsonProcessingException {
        jdbcTemplate.update("DELETE FROM world_entity WHERE world_version = ?", worldVersion);
        jdbcTemplate.update("DELETE FROM world_rule WHERE world_version = ?", worldVersion);

        String playerEntityId = "entity_player_" + worldVersion;
        String infectedEntityId = "entity_infected_" + worldVersion;

        jdbcTemplate.update("""
                INSERT INTO world_entity(entity_id, world_version, entity_type, attrs_json)
                VALUES (?, ?, ?, CAST(? AS jsonb))
                ON CONFLICT (entity_id) DO UPDATE SET
                  world_version=EXCLUDED.world_version,
                  entity_type=EXCLUDED.entity_type,
                  attrs_json=EXCLUDED.attrs_json
                """, playerEntityId, worldVersion, "PLAYER",
                objectMapper.writeValueAsString(Map.of("states", List.of("NORMAL", "TIRED", "CRITICAL"), "source", "world_factory")));

        jdbcTemplate.update("""
                INSERT INTO world_entity(entity_id, world_version, entity_type, attrs_json)
                VALUES (?, ?, ?, CAST(? AS jsonb))
                ON CONFLICT (entity_id) DO UPDATE SET
                  world_version=EXCLUDED.world_version,
                  entity_type=EXCLUDED.entity_type,
                  attrs_json=EXCLUDED.attrs_json
                """, infectedEntityId, worldVersion, "INFECTED",
                objectMapper.writeValueAsString(Map.of("states", List.of("DORMANT", "ALERT", "HUNTING"), "source", "world_factory")));

        jdbcTemplate.update("""
                INSERT INTO world_rule(rule_id, world_version, scope, expr, priority, action)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT (rule_id) DO UPDATE SET
                  world_version=EXCLUDED.world_version,
                  scope=EXCLUDED.scope,
                  expr=EXCLUDED.expr,
                  priority=EXCLUDED.priority,
                  action=EXCLUDED.action
                """,
                "rule_infection_guard_" + worldVersion,
                worldVersion,
                "turn_guard",
                "infection >= 80",
                10,
                "switch_to_safe_options");

        if (content != null && content.toLowerCase(Locale.ROOT).contains("禁忌")) {
            jdbcTemplate.update("""
                    INSERT INTO world_rule(rule_id, world_version, scope, expr, priority, action)
                    VALUES (?, ?, ?, ?, ?, ?)
                    ON CONFLICT (rule_id) DO UPDATE SET
                      world_version=EXCLUDED.world_version,
                      scope=EXCLUDED.scope,
                      expr=EXCLUDED.expr,
                      priority=EXCLUDED.priority,
                      action=EXCLUDED.action
                    """,
                    "rule_taboo_noise_" + worldVersion,
                    worldVersion,
                    "action_guard",
                    "continuous_high_noise >= 2",
                    20,
                    "reject_action");
        }
    }

    private void tagChunks(String worldVersion) throws JsonProcessingException {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT chunk_id, chunk_text FROM world_chunk WHERE world_version = ?",
                worldVersion
        );
        for (Map<String, Object> row : rows) {
            Long chunkId = ((Number) row.get("chunk_id")).longValue();
            String text = String.valueOf(row.get("chunk_text"));
            String tagsJson = objectMapper.writeValueAsString(extractTags(text));
            jdbcTemplate.update("UPDATE world_chunk SET tags_json = CAST(? AS jsonb) WHERE chunk_id = ?", tagsJson, chunkId);
        }
    }

    private void indexToOnlineTables(String worldVersion, List<String> chunks, boolean forceRebuild) throws JsonProcessingException {
        if (forceRebuild) {
            jdbcTemplate.update("DELETE FROM lorebook_entry WHERE version = ?", worldVersion);
            jdbcTemplate.update("DELETE FROM event_card WHERE version = ?", worldVersion);
        }

        int i = 1;
        for (String chunk : chunks.stream().limit(6).toList()) {
            String entryId = "wf_lb_" + worldVersion + "_" + i;
            String eventId = "wf_ev_" + worldVersion + "_" + i;
            String tagsJson = objectMapper.writeValueAsString(extractTags(chunk));
            String triggerJson = objectMapper.writeValueAsString(Map.of("location", inferLocation(chunk), "source", "world_factory"));
            String effectJson = objectMapper.writeValueAsString(Map.of("risk", inferRisk(chunk), "reward", "dynamic"));

            jdbcTemplate.update("""
                    INSERT INTO lorebook_entry(entry_id, title, body, tags_json, priority, version, updated_at)
                    VALUES (?, ?, ?, CAST(? AS jsonb), ?, ?, NOW())
                    ON CONFLICT (entry_id) DO UPDATE SET
                      title=EXCLUDED.title,
                      body=EXCLUDED.body,
                      tags_json=EXCLUDED.tags_json,
                      priority=EXCLUDED.priority,
                      version=EXCLUDED.version,
                      updated_at=NOW()
                    """, entryId, "世界条目 " + i, chunk, tagsJson, Math.max(30, 90 - i * 8), worldVersion);

            jdbcTemplate.update("""
                    INSERT INTO event_card(event_id, trigger_json, effect_json, constraints_json, rarity, version, updated_at)
                    VALUES (?, CAST(? AS jsonb), CAST(? AS jsonb), CAST(? AS jsonb), ?, ?, NOW())
                    ON CONFLICT (event_id) DO UPDATE SET
                      trigger_json=EXCLUDED.trigger_json,
                      effect_json=EXCLUDED.effect_json,
                      constraints_json=EXCLUDED.constraints_json,
                      rarity=EXCLUDED.rarity,
                      version=EXCLUDED.version,
                      updated_at=NOW()
                    """, eventId, triggerJson, effectJson,
                    objectMapper.writeValueAsString(Map.of("requirement", "state_valid")),
                    i <= 2 ? "RARE" : "COMMON",
                    worldVersion);

            i++;
        }
    }

    private List<String> extractTags(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        Set<String> tags = new java.util.HashSet<>();
        if (lower.contains("加油站") || lower.contains("gas")) {
            tags.add("gas_station");
        }
        if (lower.contains("地铁") || lower.contains("subway")) {
            tags.add("subway");
        }
        if (lower.contains("避难") || lower.contains("safe")) {
            tags.add("safe_house");
        }
        if (lower.contains("感染") || lower.contains("infect")) {
            tags.add("infection");
        }
        if (lower.contains("资源") || lower.contains("supply")) {
            tags.add("resource");
        }
        if (lower.contains("势力") || lower.contains("faction")) {
            tags.add("faction");
        }
        List<String> result = tags.stream().filter(v -> !v.isBlank()).collect(Collectors.toList());
        if (result.isEmpty()) {
            result.add("generic");
        }
        return result;
    }

    private String inferLocation(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.contains("地铁") || lower.contains("subway")) {
            return "subway_ruins";
        }
        if (lower.contains("加油站") || lower.contains("gas")) {
            return "old_gas_station";
        }
        return "safe_house";
    }

    private String inferRisk(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.contains("高风险") || lower.contains("危险") || lower.contains("伏击")) {
            return "HIGH";
        }
        return "MEDIUM";
    }

    private void updateJob(String jobId, String status, int progress, String stage, String errorMessage) {
        jdbcTemplate.update("""
                UPDATE world_factory_job
                SET status = ?, progress = ?, stage = ?, error_message = ?, updated_at = NOW()
                WHERE job_id = ?
                """, status, progress, stage, errorMessage, jobId);
    }
}
