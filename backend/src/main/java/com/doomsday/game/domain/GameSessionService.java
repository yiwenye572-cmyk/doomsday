package com.doomsday.game.domain;

import com.doomsday.game.agent.TurnContext;
import com.doomsday.game.agent.TurnOrchestrator;
import com.doomsday.game.api.ChooseOptionRequest;
import com.doomsday.game.api.ChooseOptionResponse;
import com.doomsday.game.api.ComebackCardRequest;
import com.doomsday.game.api.ComebackCardResponse;
import com.doomsday.game.api.CreateSessionRequest;
import com.doomsday.game.api.CreateSessionResponse;
import com.doomsday.game.api.OptionPayload;
import com.doomsday.game.api.SessionStateResponse;
import com.doomsday.game.api.StateDeltaPayload;
import com.doomsday.game.api.SubmitTurnRequest;
import com.doomsday.game.api.SubmitTurnResponse;
import com.doomsday.game.common.ApiException;
import com.doomsday.game.common.FaultInjectionGuard;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class GameSessionService {

    private final SessionRepository sessionRepo;
    private final TurnOrchestrator orchestrator;
    private final FaultInjectionGuard faultGuard;

    public GameSessionService(SessionRepository sessionRepo,
                              TurnOrchestrator orchestrator,
                              FaultInjectionGuard faultGuard) {
        this.sessionRepo = sessionRepo;
        this.orchestrator = orchestrator;
        this.faultGuard = faultGuard;
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
        faultGuard.check("submitTurn.beforeOrchestrator");

        // ===== P1: 责任链编排 =====
        TurnContext ctx = orchestrator.run(sessionId, session, request.playerInput(), idempotencyKey);
        faultGuard.check("submitTurn.afterOrchestrator");
        // StateCommitAgent 已经写入 Redis（session 引用已被修改），直接组装响应

        SubmitTurnResponse response = new SubmitTurnResponse(
                session.getTurn(),
                session.getVersion(),
                ctx.plot,
                ctx.options,
                ctx.difficultyDelta,
                ctx.stateDelta != null ? ctx.stateDelta : new StateDeltaPayload(-6, 12, List.of())
        );
        sessionRepo.saveIdempotent(idemKey, response);
        return response;
    }

    public ChooseOptionResponse chooseOption(String sessionId, int turn, ChooseOptionRequest request) {
        GameSession session = loadSession(sessionId);
        ensureVersion(session, request.expectedVersion());

        if (turn != session.getCurrentTurn()) {
            throw new ApiException("BAD_REQUEST", "invalid turn");
        }
        faultGuard.check("chooseOption.beforeApply");

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
        faultGuard.check("comeback.beforeApply");

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

}

