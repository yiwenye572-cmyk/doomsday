package com.doomsday.game.cabin;

import com.doomsday.game.cabin.dto.CabinStateRequest;
import com.doomsday.game.cabin.dto.CabinStateResponse;
import com.doomsday.game.common.exceptions.ResourceNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CabinService {

    private final CabinRepository cabinRepository;

    public CabinService(CabinRepository cabinRepository) {
        this.cabinRepository = cabinRepository;
    }

    public CabinStateResponse updateState(CabinStateRequest request) {
        // 更新小屋状态逻辑
        CabinEntity entity = cabinRepository.findBySessionId(request.getSessionId())
                .orElseThrow(() -> new ResourceNotFoundException("Cabin state not found for session: " + request.getSessionId()));

        entity.setStateData(request.getStateData());
        cabinRepository.save(entity);

        return new CabinStateResponse(entity.getSessionId(), entity.getStateData());
    }

    public CabinStateResponse getState(String sessionId) {
        // 获取小屋状态逻辑
        CabinEntity entity = cabinRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Cabin state not found for session: " + sessionId));

        return new CabinStateResponse(entity.getSessionId(), entity.getStateData());
    }
}