import http from "./http";
import type { ApiResponse } from "../types/api";

export interface AgentMetricsSummary {
  agentName: string;
  totalCalls: number;
  successCalls: number;
  failCalls: number;
  avgMs: number;
  avgQueueWaitMs: number;
  avgModelMs: number;
  avgPostProcessMs: number;
  avgPromptTokens: number;
  avgCompletionTokens: number;
  avgTokens: number;
  successRate: number;
}

export interface AgentSpan {
  agentName: string;
  elapsedMs: number;
  status: string;
  errorMessage: string | null;
  queueWaitMs?: number;
  modelMs?: number;
  postProcessMs?: number;
  promptTokens?: number;
  completionTokens?: number;
  totalTokens?: number;
  tokensPerSecond?: number;
  modelName?: string;
}

export interface TraceDetail {
  traceId: string;
  sessionId: string;
  turn: number;
  startedAt: number;
  elapsedMs: number;
  finalStatus: string;
  spans: AgentSpan[];
}

export interface ToolSummary {
  toolName: string;
  totalCalls: number;
  successCalls: number;
  failedCalls: number;
  avgMs: number;
  avgRetry: number;
}

export interface ToolAuditItem {
  traceId: string;
  sessionId: string;
  callerAgent: string;
  toolName: string;
  status: string;
  retryCount: number;
  compensated: boolean;
  latencyMs: number;
  errorCode: string;
  createdAt: string;
}

export interface RegisteredTool {
  toolName: string;
  description: string;
  sideEffect: boolean;
  requiredFields: string[];
}

export async function getAgentMetrics(): Promise<AgentMetricsSummary[]> {
  const res = await http.get<ApiResponse<AgentMetricsSummary[]>>("/admin/metrics/agents");
  return res.data.data;
}

export async function getRecentTraces(limit = 20): Promise<TraceDetail[]> {
  const res = await http.get<ApiResponse<TraceDetail[]>>("/admin/metrics/traces", {
    params: { limit },
  });
  return res.data.data;
}

export async function getTrace(traceId: string): Promise<TraceDetail> {
  const res = await http.get<ApiResponse<TraceDetail>>(`/admin/metrics/traces/${traceId}`);
  return res.data.data;
}

export async function getToolSummary(): Promise<ToolSummary[]> {
  const res = await http.get<ApiResponse<ToolSummary[]>>("/admin/tools/summary");
  return res.data.data;
}

export async function getToolAudits(limit = 20): Promise<ToolAuditItem[]> {
  const res = await http.get<ApiResponse<ToolAuditItem[]>>("/admin/tools/audits", {
    params: { limit },
  });
  return res.data.data;
}

export async function getRegisteredTools(): Promise<RegisteredTool[]> {
  const res = await http.get<ApiResponse<RegisteredTool[]>>("/admin/tools");
  return res.data.data;
}
