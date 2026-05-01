package com.doomsday.game.domain;

import com.doomsday.game.api.ChooseOptionRequest;
import com.doomsday.game.api.ChooseOptionResponse;
import com.doomsday.game.api.ComebackCardRequest;
import com.doomsday.game.api.ComebackCardResponse;
import com.doomsday.game.api.CreateSessionRequest;
import com.doomsday.game.api.CreateSessionResponse;
import com.doomsday.game.api.DifficultyDeltaPayload;
import com.doomsday.game.api.OptionPayload;
import com.doomsday.game.api.PlotPayload;
import com.doomsday.game.api.SessionStateResponse;
import com.doomsday.game.api.StateDeltaPayload;
import com.doomsday.game.api.SubmitTurnRequest;
import com.doomsday.game.api.SubmitTurnResponse;
import com.doomsday.game.common.ApiException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class GameSessionService {

    private static final int MAX_CAS_RETRIES = 3;

    private final SessionRepository sessionRepo;

    public GameSessionService(SessionRepository sessionRepo) {
        this.sessionRepo = sessionRepo;
    }

    public CreateSessionResponse createSession(CreateSessionRequest request) {
        String sessionId = "s_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 6);
        GameSession session = GameSession.create(sessionId, request.difficulty());
        sessionRepo.save(session);

        return new CreateSessionResponse(
                sessionId,
                toStateResponse(session),
                request.difficulty().challengeBand()
        );
    }

    public SessionStateResponse getState(String sessionId) {
        GameSession session = loadSession(sessionId);
        return toStateResponse(session);
    }

    public SubmitTurnResponse submitTurn(String sessionId, String idempotencyKey, SubmitTurnRequest request) {
        String idemKey = sessionId + ":" + idempotencyKey;
        SubmitTurnResponse cached = sessionRepo.findIdempotent(idemKey);
        if (cached != null) {
            return cached;
        }

        GameSession session = loadSession(sessionId);
        ensureVersion(session, request.expectedVersion());

        session.setTurn(session.getTurn() + 1);
        session.setVersion(session.getVersion() + 1);

        double[] band = session.getDifficulty().challengeBand();
        double challengeIndex = calculateChallengeIndex(session);
        session.setChallengeIndex(challengeIndex);
        DifficultyDeltaPayload delta = deriveDifficultyDelta(challengeIndex, band);

        int staminaCost = 6;
        session.setStamina(Math.max(0, session.getStamina() - staminaCost));

        PlotPayload plot = new PlotPayload(
                generateNarration(request.playerInput()),
                List.of("event_card:ev_033", "lorebook:lb_zone_7"),
                0.84
        );

        List<OptionPayload> options = fixedOptions();
        session.setCurrentOptions(options);
        session.setCurrentTurn(session.getTurn());

        SubmitTurnResponse response = new SubmitTurnResponse(
                session.getTurn(),
                session.getVersion(),
                plot,
                options,
                delta,
                new StateDeltaPayload(-staminaCost, 12, List.of("found_medical_trace"))
        );
        sessionRepo.save(session);
        sessionRepo.saveIdempotent(idemKey, response);
        return response;
    }

    public ChooseOptionResponse chooseOption(String sessionId, int turn, ChooseOptionRequest request) {
        GameSession session = loadSession(sessionId);
        ensureVersion(session, request.expectedVersion());

        if (turn != session.getCurrentTurn()) {
            throw new ApiException("BAD_REQUEST", "invalid turn");
        }

        OptionPayload selected = session.getCurrentOptions().stream()
                .filter(o -> o.id().equals(request.optionId()))
                .findFirst()
                .orElseThrow(() -> new ApiException("BAD_REQUEST", "invalid optionId"));

        int staminaDelta = switch (selected.id()) {
            case "opt_a" -> -8;
            case "opt_b" -> -2;
            case "opt_c" -> -4;
            case "opt_d" -> -5;
            default -> -3;
        };

        session.setStamina(Math.max(0, session.getStamina() + staminaDelta));
        session.setVersion(session.getVersion() + 1);
        sessionRepo.save(session);

        return new ChooseOptionResponse(
                turn,
                selected.id(),
                true,
                session.getVersion(),
                new StateDeltaPayload(staminaDelta, 0, List.of())
        );
    }

    public ComebackCardResponse useComebackCard(String sessionId, ComebackCardRequest request) {
        GameSession session = loadSession(sessionId);
        ensureVersion(session, request.expectedVersion());

        if (session.getComebackCardRemaining() <= 0) {
            throw new ApiException("RULE_VIOLATION", "comeback card already used");
        }

        boolean nearDeath = session.getHp() <= 40
                || session.getStamina() <= 20
                || session.getInfection() >= 60;
        if (!nearDeath) {
            throw new ApiException("RULE_VIOLATION", "comeback card can only be used in critical state");
        }

        session.setHp(Math.min(100, session.getHp() + 25));
        session.setStamina(Math.min(100, session.getStamina() + 20));
        session.setComebackCardRemaining(session.getComebackCardRemaining() - 1);
        session.setVersion(session.getVersion() + 1);
        sessionRepo.save(session);

        return new ComebackCardResponse(
                true,
                session.getVersion(),
                Map.of("hp", "+25", "stamina", "+20", "buff", "panic_resist_3_turns"),
                session.getComebackCardRemaining()
        );
    }

    private GameSession loadSession(String sessionId) {
        GameSession session = sessionRepo.findById(sessionId);
        if (session == null) {
            throw new ApiException("NOT_FOUND", "session not found");
        }
        return session;
    }

    private void ensureVersion(GameSession session, long expectedVersion) {
        if (expectedVersion != session.getVersion()) {
            throw new ApiException(
                    "CONFLICT_VERSION",
                    "state version mismatch",
                    Map.of("expectedVersion", expectedVersion, "actualVersion", session.getVersion())
            );
        }
    }

    private List<OptionPayload> fixedOptions() {
        List<OptionPayload> options = new ArrayList<>();
        options.add(new OptionPayload("opt_a", "撬开储物柜，快速搜药后撤离（激进）", "HIGH", "可能获得稀有药品，但噪音显著上升"));
        options.add(new OptionPayload("opt_b", "原地观察 30 秒，先确认是否有感染者巡逻（稳健）", "MEDIUM_LOW", "降低遭遇战概率，但推进速度略慢"));
        options.add(new OptionPayload("opt_c", "拆解附近废铁与布料，先做简易防护（资源导向）", "MEDIUM", "短期收益一般，但可提升后续生存容错"));
        options.add(new OptionPayload("opt_d", "沿油站后墙潜行，尝试发现隐藏入口（探索导向）", "MEDIUM_HIGH", "可能触发支线与高价值线索，也可能遭遇伏击"));
        return options;
    }

    private String generateNarration(String playerInput) {
        return "你压低呼吸，沿着裂开的水泥墙一步一步向前挪动，鞋底碾过玻璃碎粒时发出细微摩擦声。"
                + "空气里混着汽油和腐败物的味道，像一层发黏的雾裹住喉咙。你刚才的决定是：" + playerInput
                + "。昏黄光线从门缝漏进来，映出地面拖拽痕迹，尽头像通往配电间。你知道体力正在下降，"
                + "但这里可能有能救命的药品，也可能埋着会把你拖进深渊的响动源。远处金属管道忽然轻颤，"
                + "像有什么东西正在靠近。你必须在谨慎和速度之间做出下一步选择。";
    }

    private SessionStateResponse toStateResponse(GameSession s) {
        return new SessionStateResponse(
                s.getSessionId(),
                s.getVersion(),
                s.getHp(),
                s.getStamina(),
                s.getInfection(),
                s.getLocation(),
                List.copyOf(s.getInventory()),
                s.getChallengeIndex(),
                s.getDifficulty().challengeBand(),
                s.getTurn()
        );
    }

    private double calculateChallengeIndex(GameSession session) {
        double base = switch (session.getDifficulty()) {
            case SEEKER -> 0.45;
            case SURVIVOR -> 0.58;
            case HELL -> 0.72;
        };
        double staminaFactor = (100 - session.getStamina()) / 200.0;
        double infectionFactor = session.getInfection() / 200.0;
        double value = base + staminaFactor + infectionFactor;
        return Math.max(0.1, Math.min(0.95, value));
    }

    private DifficultyDeltaPayload deriveDifficultyDelta(double index, double[] band) {
        if (index < band[0]) {
            return new DifficultyDeltaPayload(0.10, -0.03, 0.05, 0.00);
        }
        if (index > band[1]) {
            return new DifficultyDeltaPayload(-0.08, 0.07, -0.03, 0.00);
        }
        return new DifficultyDeltaPayload(0.02, -0.01, 0.01, 0.00);
    }
}
