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
  return response.data;
}

export async function getSessionState(sessionId: string): Promise<SessionState> {
  const response = await http.get<ApiResponse<SessionState>>(`/game/sessions/${sessionId}/state`);
  return response.data;
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
  return response.data;
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
  return response.data;
}

export async function useComebackCard(
  sessionId: string,
  payload: ComebackCardRequest,
): Promise<ComebackCardResponse> {
  const response = await http.post<ApiResponse<ComebackCardResponse>>(
    `/game/sessions/${sessionId}/comeback-card`,
    payload,
  );
  return response.data;
}

export async function getReplay(
  sessionId: string,
  range?: { fromTurn?: number; toTurn?: number },
): Promise<string> {
  const response = await http.get<ApiResponse<string>>(`/game/sessions/${sessionId}/replay`, {
    params: range,
  });
  return response.data;
}
