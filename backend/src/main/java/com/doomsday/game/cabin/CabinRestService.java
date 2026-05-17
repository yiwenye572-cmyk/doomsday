package com.doomsday.game.cabin;

import com.doomsday.game.cabin.dto.CabinRestRequest;
import com.doomsday.game.cabin.dto.CabinRestResponse;
import com.doomsday.game.common.exceptions.ResourceNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CabinRestService {

    private final CabinRepository cabinRepository;

    public CabinRestService(CabinRepository cabinRepository) {
        this.cabinRepository = cabinRepository;
    }

    public CabinRestResponse rest(CabinRestRequest request) {
        // 获取小屋状态
        CabinEntity entity = cabinRepository.findBySessionId(request.getSessionId())
                .orElseThrow(() -> new ResourceNotFoundException("Cabin state not found for session: " + request.getSessionId()));

        // 模拟休息逻辑：更新体力、推进时间（每小时恢复 10 点体力）
        int newStamina = entity.getPlayerStamina() + request.getDurationHours() * 10;
        entity.setPlayerStamina(newStamina);
        entity.setTimeOfDay(request.getNewTimeOfDay());

        cabinRepository.save(entity);

        return new CabinRestResponse(entity.getSessionId(), entity.getPlayerStamina(), entity.getTimeOfDay());
    }
}