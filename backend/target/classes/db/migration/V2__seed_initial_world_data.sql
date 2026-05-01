-- V2: 初始化可召回的 Lorebook 与 EventCard 种子数据

INSERT INTO lorebook_entry (entry_id, title, body, tags_json, priority, version)
VALUES
    ('lb_safe_house_protocol', '避难所值守守则',
     'safe_house 的铁门夜间必须双重加固。若听见连续三次敲击，优先观察再开门。',
     '["safe_house","protocol","defense"]'::jsonb, 90, 'world_v1'),
    ('lb_fuel_filter_tip', '燃油过滤技巧',
     '在 old_gas_station 搜刮时，先检查过滤网与沉淀罐，污染燃油会快速损坏发电机。',
     '["old_gas_station","resource","fuel"]'::jsonb, 85, 'world_v1'),
    ('lb_subway_echo_warning', '地铁回声警报',
     'subway_ruins 的金属回声常意味着群体移动，持续回声超过十秒应立即转移。',
     '["subway_ruins","warning","movement"]'::jsonb, 88, 'world_v1'),
    ('lb_medical_trace', '简易医疗线索',
     '药品优先级：止血>抗感染>镇痛。抗感染药需配合清洁水源，否则副作用显著。',
     '["medical","loot","infection"]'::jsonb, 92, 'world_v1'),
    ('lb_silent_explore', '静默探索原则',
     '探索时保持低姿态并沿阴影移动，先确认退路再进入目标区域。',
     '["explore","stealth","risk_control"]'::jsonb, 80, 'world_v1'),
    ('lb_combat_burst', '短促交战建议',
     '交战阶段应避免长时间暴露，建议两次短突进后立刻换位。',
     '["combat","tactic","stamina"]'::jsonb, 78, 'world_v1')
ON CONFLICT (entry_id) DO UPDATE
SET title = EXCLUDED.title,
    body = EXCLUDED.body,
    tags_json = EXCLUDED.tags_json,
    priority = EXCLUDED.priority,
    version = EXCLUDED.version,
    updated_at = NOW();

INSERT INTO event_card (event_id, trigger_json, effect_json, constraints_json, rarity, version)
VALUES
    ('ev_safe_house_knock',
     '{"location":"safe_house","trigger":"night_knock"}'::jsonb,
     '{"hp":0,"stamina":-2,"loot":"none","risk":"medium"}'::jsonb,
     '{"requires":"observation_first"}'::jsonb,
     'RARE', 'world_v1'),
    ('ev_safe_house_generator',
     '{"location":"safe_house","trigger":"generator_failure"}'::jsonb,
     '{"stamina":-3,"loot":"battery_part","risk":"low"}'::jsonb,
     '{"requires":"toolkit"}'::jsonb,
     'COMMON', 'world_v1'),
    ('ev_gas_station_backdoor',
     '{"location":"old_gas_station","trigger":"backdoor_route"}'::jsonb,
     '{"stamina":-4,"loot":"medical_cache","risk":"medium"}'::jsonb,
     '{"requires":"stealth"}'::jsonb,
     'EPIC', 'world_v1'),
    ('ev_gas_station_alarm',
     '{"location":"old_gas_station","trigger":"alarm_echo"}'::jsonb,
     '{"stamina":-6,"infection":5,"risk":"high"}'::jsonb,
     '{"requires":"rapid_exit"}'::jsonb,
     'RARE', 'world_v1'),
    ('ev_subway_shadow_move',
     '{"location":"subway_ruins","trigger":"shadow_move"}'::jsonb,
     '{"stamina":-5,"loot":"map_fragment","risk":"high"}'::jsonb,
     '{"requires":"flashlight"}'::jsonb,
     'RARE', 'world_v1'),
    ('ev_subway_storage_room',
     '{"location":"subway_ruins","trigger":"sealed_storage"}'::jsonb,
     '{"stamina":-3,"loot":"antibiotic","risk":"medium"}'::jsonb,
     '{"requires":"crowbar"}'::jsonb,
     'COMMON', 'world_v1')
ON CONFLICT (event_id) DO UPDATE
SET trigger_json = EXCLUDED.trigger_json,
    effect_json = EXCLUDED.effect_json,
    constraints_json = EXCLUDED.constraints_json,
    rarity = EXCLUDED.rarity,
    version = EXCLUDED.version,
    updated_at = NOW();
