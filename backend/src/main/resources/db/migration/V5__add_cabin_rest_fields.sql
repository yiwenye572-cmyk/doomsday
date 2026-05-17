-- 添加体力和时间字段到 cabin_state 表
ALTER TABLE cabin_state
ADD COLUMN player_stamina INT NOT NULL DEFAULT 100,
ADD COLUMN time_of_day VARCHAR(20) NOT NULL DEFAULT 'morning';