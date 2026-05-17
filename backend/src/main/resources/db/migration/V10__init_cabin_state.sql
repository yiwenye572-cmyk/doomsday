-- V10: 小屋状态表初始化（cabin_state）
-- 包含: session隔离、版本号（CAS用）、玩家状态、物品布局JSON

CREATE TABLE IF NOT EXISTS cabin_state (
    id               BIGSERIAL    PRIMARY KEY,
    session_id       VARCHAR(128) NOT NULL UNIQUE,
    version          BIGINT       NOT NULL DEFAULT 0,
    state_data       TEXT,                         -- 物品布局JSON快照
    player_stamina   INT          NOT NULL DEFAULT 100,
    time_of_day      VARCHAR(32)  NOT NULL DEFAULT 'morning',
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_cabin_session_id ON cabin_state(session_id);

-- 更新触发器：自动维护 updated_at
CREATE OR REPLACE FUNCTION update_cabin_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_cabin_updated_at
    BEFORE UPDATE ON cabin_state
    FOR EACH ROW EXECUTE FUNCTION update_cabin_updated_at();
