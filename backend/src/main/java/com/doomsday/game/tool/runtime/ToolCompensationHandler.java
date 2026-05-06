package com.doomsday.game.tool.runtime;

import com.doomsday.game.domain.GameSession;
import com.doomsday.game.domain.SessionRepository;
import com.doomsday.game.tool.ToolContext;
import java.util.ArrayList;
import org.springframework.stereotype.Component;

@Component
public class ToolCompensationHandler {

    public static final String ENTITY_STATE_PATCH_TOOL = "EntityStatePatchTool";

    private final SessionRepository sessionRepository;

    public ToolCompensationHandler(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    public Object captureSnapshot(String toolName, ToolContext context) {
        if (!ENTITY_STATE_PATCH_TOOL.equals(toolName) || context == null || context.session() == null) {
            return null;
        }
        GameSession s = context.session();
        return new SessionSnapshot(
                s.getVersion(),
                s.getTurn(),
                s.getCurrentTurn(),
                s.getHp(),
                s.getStamina(),
                s.getInfection(),
                s.getLocation(),
                new ArrayList<>(s.getInventory())
        );
    }

    public boolean compensate(String toolName, ToolContext context, Object snapshot) {
        if (!ENTITY_STATE_PATCH_TOOL.equals(toolName)
                || context == null
                || context.session() == null
                || !(snapshot instanceof SessionSnapshot snap)) {
            return false;
        }
        GameSession s = context.session();
        s.setVersion(snap.version());
        s.setTurn(snap.turn());
        s.setCurrentTurn(snap.currentTurn());
        s.setHp(snap.hp());
        s.setStamina(snap.stamina());
        s.setInfection(snap.infection());
        s.setLocation(snap.location());
        s.setInventory(new ArrayList<>(snap.inventory()));
        sessionRepository.save(s);
        return true;
    }

    private record SessionSnapshot(
            long version,
            int turn,
            int currentTurn,
            int hp,
            int stamina,
            int infection,
            String location,
            java.util.List<String> inventory
    ) {
    }
}
