package com.doomsday.game.worldfactory;

import com.doomsday.game.common.ApiException;
import com.doomsday.game.world.LorebookEntryRepository;
import com.doomsday.game.world.EventCardRepository;
import com.doomsday.game.worldfactory.dto.GameWorldInitRequest;
import com.doomsday.game.worldfactory.dto.WorldFactoryJobRequest;
import com.doomsday.game.worldfactory.dto.WorldFactoryJobResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class WorldFactoryJobService {

    public static final String DEFAULT_WORLD_VERSION = "world_v1";

    private final JdbcTemplate jdbcTemplate;
    private final WorldFactoryJobWorker worker;
    private final ObjectMapper objectMapper;
    private final LorebookEntryRepository lorebookRepo;
    private final EventCardRepository eventCardRepo;

    public WorldFactoryJobService(JdbcTemplate jdbcTemplate,
                                  WorldFactoryJobWorker worker,
                                  ObjectMapper objectMapper,
                                  LorebookEntryRepository lorebookRepo,
                                  EventCardRepository eventCardRepo) {
        this.jdbcTemplate = jdbcTemplate;
        this.worker = worker;
        this.objectMapper = objectMapper;
        this.lorebookRepo = lorebookRepo;
        this.eventCardRepo = eventCardRepo;
    }

    public WorldFactoryJobResponse createJob(WorldFactoryJobRequest request) {
        String worldVersion = normalizeWorldVersion(request.worldVersion());
        String rawContent = resolveRawContent(request);
        String jobId = newJobId("wf_job");

        jdbcTemplate.update("""
                INSERT INTO world_factory_job(job_id, world_version, source_type, status, progress, stage, force_rebuild, raw_content)
                VALUES (?, ?, ?, 'PENDING', 0, 'PENDING', ?, ?)
                """,
                jobId, worldVersion, request.sourceType().name(), request.forceRebuild(), rawContent);

        worker.enqueue(jobId, worldVersion, request.sourceType(), rawContent, request.forceRebuild());
        return getJob(jobId);
    }

    public WorldFactoryJobResponse createFromBasicProfile(GameWorldInitRequest request) {
        WorldFactoryJobRequest jobRequest = new WorldFactoryJobRequest(
                "world_" + System.currentTimeMillis(),
                WorldSourceType.BASIC_PROFILE,
                null,
                false,
                Map.of(
                        "worldTheme", request.worldTheme(),
                        "eraStyle", request.eraStyle(),
                        "survivalTone", request.survivalTone(),
                        "keyFaction", safe(request.keyFaction()),
                        "forbiddenRule", safe(request.forbiddenRule())
                )
        );
        return createJob(jobRequest);
    }

    public WorldFactoryJobResponse getJob(String jobId) {
        return jdbcTemplate.query("""
                SELECT job_id, world_version, source_type, status, progress, stage, error_message
                FROM world_factory_job WHERE job_id = ?
                """, rs -> {
            if (!rs.next()) {
                throw new ApiException("NOT_FOUND", "world factory job not found");
            }
            return new WorldFactoryJobResponse(
                    rs.getString("job_id"),
                    rs.getString("world_version"),
                    rs.getString("source_type"),
                    rs.getString("status"),
                    rs.getInt("progress"),
                    rs.getString("stage"),
                    rs.getString("error_message")
            );
        }, jobId);
    }

    public String defaultWorldVersion() {
        long loreCount = lorebookRepo.countByWorldVersion(DEFAULT_WORLD_VERSION);
        long eventCount = eventCardRepo.countByWorldVersion(DEFAULT_WORLD_VERSION);
        if (loreCount <= 0 || eventCount <= 0) {
            throw new ApiException("INTERNAL_ERROR", "default world data not ready");
        }
        return DEFAULT_WORLD_VERSION;
    }

    private String resolveRawContent(WorldFactoryJobRequest request) {
        return switch (request.sourceType()) {
            case DEFAULT_TEMPLATE -> defaultWorldBook();
            case RAW_TEXT -> {
                if (request.content() == null || request.content().isBlank()) {
                    throw new ApiException("BAD_REQUEST", "content is required when sourceType=RAW_TEXT");
                }
                yield request.content();
            }
            // BASIC_PROFILE：序列化 profile JSON，交给后台 Worker 异步调用 AI 展开
            case BASIC_PROFILE -> {
                Map<String, String> profile = request.basicProfile();
                if (profile == null || profile.isEmpty()) {
                    throw new ApiException("BAD_REQUEST", "basicProfile is required for BASIC_PROFILE source");
                }
                try {
                    yield objectMapper.writeValueAsString(profile);
                } catch (Exception e) {
                    yield defaultWorldBook();
                }
            }
        };
    }

    private String defaultWorldBook() {
        return """
                # 世界背景
                文明崩坏后第17年，连续酸雨和能源危机让城市网络彻底碎裂。

                # 区域设定
                - safe_house: 临时避难点，资源稀缺但相对安全。
                - old_gas_station: 可搜刮燃料与药品，伴随高噪声风险。
                - subway_ruins: 高价值物资区，感染者密度高。

                # 势力关系
                - 灰烬商会：掌控药品交换渠道。
                - 夜巡队：提供低强度治安，但要求上缴资源。
                - 无旗流民：随机出现，可能交易也可能掠夺。

                # 资源规则
                - 高价值物资通常伴随高风险事件。
                - 体力低于20时，探索效率下降且受伤概率上升。

                # 风险事件
                - 夜雨伏击、感染扩散、资源争夺、局部塌陷。

                # 实体状态机
                - 玩家：NORMAL -> TIRED -> CRITICAL
                - 感染者：DORMANT -> ALERT -> HUNTING

                # 禁忌规则
                - 不得在高感染区连续停留超过3回合。
                - 高噪声动作不得连续两回合触发。
                """;
    }

    private String normalizeWorldVersion(String raw) {
        if (raw == null || raw.isBlank()) {
            return "world_" + System.currentTimeMillis();
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_\\-]", "_");
        return normalized.length() > 64 ? normalized.substring(0, 64) : normalized;
    }

    private String newJobId(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private String safe(String text) {
        return text == null || text.isBlank() ? "未指定" : text;
    }
}
