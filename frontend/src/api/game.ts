import http from "./http";
import type {
  ApiResponse,
  ChooseOptionRequest,
  ChooseOptionResponse,
  ComebackCardRequest,
  ComebackCardResponse,
  CreateSessionRequest,
  CreateSessionResponse,
  SessionState,
  SubmitTurnRequest,
  SubmitTurnResponse,
} from "../types/api";

function idempotencyKey() {
  const uuid = globalThis.crypto?.randomUUID?.();
  return uuid ?? `idem-${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

export async function createSession(payload: CreateSessionRequest): Promise<CreateSessionResponse> {
  const response = await http.post<ApiResponse<CreateSessionResponse>>("/game/sessions", payload);
  return response.data.data;
}

export async function getSessionState(sessionId: string): Promise<SessionState> {
  const response = await http.get<ApiResponse<SessionState>>(`/game/sessions/${sessionId}/state`);
  return response.data.data;
}

export async function submitTurn(sessionId: string, payload: SubmitTurnRequest): Promise<SubmitTurnResponse> {
  const response = await http.post<ApiResponse<SubmitTurnResponse>>(
    `/game/sessions/${sessionId}/turns`,
    payload,
    {
      headers: {
        "Idempotency-Key": idempotencyKey(),
      },
    },
  );
  return response.data.data;
}

export async function chooseOption(
  sessionId: string,
  turn: number,
  payload: ChooseOptionRequest,
): Promise<ChooseOptionResponse> {
  const response = await http.post<ApiResponse<ChooseOptionResponse>>(
    `/game/sessions/${sessionId}/turns/${turn}/choose`,
    payload,
  );
  return response.data.data;
}

export async function useComebackCard(
  sessionId: string,
  payload: ComebackCardRequest,
): Promise<ComebackCardResponse> {
  const response = await http.post<ApiResponse<ComebackCardResponse>>(
    `/game/sessions/${sessionId}/comeback-card`,
    payload,
  );
  return response.data.data;
}

export async function getReplay(
  sessionId: string,
  range?: { fromTurn?: number; toTurn?: number },
): Promise<string> {
  const response = await http.get<ApiResponse<string>>(`/game/sessions/${sessionId}/replay`, {
    params: range,
  });
  return response.data.data;
}

// ─── Cabin APIs ──────────────────────────────────────────────────────────

/** 小屋状态响应（与 CabinStateResponse 对应） */
export interface CabinState {
  sessionId: string;
  version: number;
  stateData: string;
  playerStamina: number;
  timeOfDay: string;
}

/** 获取小屋状态 */
export async function getCabinState(sessionId: string): Promise<CabinState> {
  const response = await http.get<ApiResponse<CabinState>>(`/game/sessions/${sessionId}/cabin`);
  return response.data.data;
}

/** 提交物品变更（Redis Lua CAS；409 = 版本冲突） */
export async function updateCabinState(
  sessionId: string,
  expectedVersion: number,
  changes: Array<Record<string, unknown>>,
): Promise<{ newVersion: number; stateData: string; conflict: boolean; conflictMessage?: string }> {
  const response = await http.post(
    `/game/sessions/${sessionId}/cabin/update`,
    { idempotencyKey: idempotencyKey(), expectedVersion, changes },
  );
  return (response.data as ApiResponse<any>).data;
}

/** 休息触发 */
export async function restCabin(
  sessionId: string,
  durationHours: number,
  newTimeOfDay: string,
): Promise<{ sessionId: string; updatedStamina: number; updatedTimeOfDay: string }> {
  const response = await http.post(`/game/sessions/${sessionId}/cabin/rest`, {
    sessionId,
    durationHours,
    newTimeOfDay,
  });
  return (response.data as ApiResponse<any>).data;
}

/** 出门携带物品 */
export async function takeCabinItems(
  sessionId: string,
  itemIds: string[],
): Promise<CabinState> {
  const response = await http.post(`/game/sessions/${sessionId}/cabin/take`, {
    idempotencyKey: idempotencyKey(),
    itemIds,
  });
  return (response.data as ApiResponse<CabinState>).data;
}

/** 归来放入待整理区 */
export async function returnCabinItems(
  sessionId: string,
  foundItems: Array<Record<string, unknown>>,
): Promise<CabinState> {
  const response = await http.post(`/game/sessions/${sessionId}/cabin/return`, {
    idempotencyKey: idempotencyKey(),
    foundItems,
  });
  return (response.data as ApiResponse<CabinState>).data;
}

// ─── Item Story API ───────────────────────────────────────────────────────

export interface ItemStoryResult {
  taskId: number | null;
  sessionId: string;
  itemId: string;
  status: "PENDING" | "RUNNING" | "DONE" | "FAILED";
  story: string | null;
  errorMessage: string | null;
  generatedAt: string | null;
}

/**
 * 查询/触发物品叙事生成。
 * - 首次调用：后端创建任务，返回 HTTP 202 + status=PENDING
 * - 轮询调用：返回当前 status（RUNNING/DONE/FAILED）
 * - DONE：story 字段包含叙事文本
 * @param accepted  输出参数，表示是否返回了 202（由 axios 响应状态判断）
 */
export async function getItemStory(
  sessionId: string,
  itemId: string,
  itemType?: string,
  itemMetadata?: string,
): Promise<{ accepted: boolean; data: ItemStoryResult }> {
  const response = await http.get<ApiResponse<ItemStoryResult>>(
    `/game/sessions/${sessionId}/items/${itemId}/story`,
    { params: { itemType, itemMetadata }, validateStatus: (s) => s < 500 },
  );
  return {
    accepted: response.status === 202,
    data: response.data.data,
  };
}

// ─── Layout Recommendation API ────────────────────────────────────────────

export interface RecommendedItem {
  id: string;
  type: string;
  x: number;
  y: number;
  w: number;
  h: number;
  rotation: number;
}

export interface LayoutRecommendation {
  recommendationId: string;
  sessionId: string;
  items: RecommendedItem[];
  reason: string;
  confidence: number;
  status: "READY" | "ACCEPTED" | "REJECTED";
}

/** 生成布局推荐（GET） */
export async function getRecommendation(sessionId: string): Promise<LayoutRecommendation> {
  const response = await http.get<ApiResponse<LayoutRecommendation>>(
    `/game/sessions/${sessionId}/cabin/recommendation`,
  );
  return response.data.data;
}

/** 接受推荐，应用到小屋状态（POST accept） */
export async function acceptRecommendation(
  sessionId: string,
  recommendationId: string,
  expectedVersion: number,
): Promise<{ newVersion: number; stateData: string; conflict: boolean; conflictMessage?: string }> {
  const response = await http.post(
    `/game/sessions/${sessionId}/cabin/recommendation/${recommendationId}/accept`,
    { expectedVersion },
    { validateStatus: (s) => s < 500 },
  );
  return (response.data as ApiResponse<any>).data;
}

/** 拒绝推荐（POST reject） */
export async function rejectRecommendation(
  sessionId: string,
  recommendationId: string,
): Promise<void> {
  await http.post(
    `/game/sessions/${sessionId}/cabin/recommendation/${recommendationId}/reject`,
    {},
  );
}

