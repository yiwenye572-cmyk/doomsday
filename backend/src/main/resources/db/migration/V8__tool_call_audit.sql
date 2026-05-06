-- V8: ReAct Tool Calling 审计表

CREATE TABLE IF NOT EXISTS tool_call_audit (
    id               BIGSERIAL PRIMARY KEY,
    trace_id         VARCHAR(96)  NOT NULL,
    session_id       VARCHAR(96),
    caller_agent     VARCHAR(64),
    tool_name        VARCHAR(64)  NOT NULL,
    status           VARCHAR(16)  NOT NULL,
    retry_count      INT          NOT NULL DEFAULT 0,
    latency_ms       BIGINT       NOT NULL DEFAULT 0,
    error_code       VARCHAR(64),
    error_message    TEXT,
    request_json     TEXT,
    result_json      TEXT,
    compensated      BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_tool_audit_trace_id
    ON tool_call_audit(trace_id);

CREATE INDEX IF NOT EXISTS idx_tool_audit_tool_created
    ON tool_call_audit(tool_name, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_tool_audit_status_created
    ON tool_call_audit(status, created_at DESC);
