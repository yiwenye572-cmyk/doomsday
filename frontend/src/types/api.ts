export interface ApiResponse<T> {
  code: string;
  message: string;
  data: T;
  traceId: string;
  timestamp: number;
}

export interface CreateSessionRequest {
  playerId: string;
  difficulty: "SEEKER" | "SURVIVOR" | "HELL";
  worldVersion: string;
  styleProfile: string;
}

export interface SessionState {
  sessionId: string;
  version: number;
  hp: number;
  stamina: number;
  infection: number;
  location: string;
  inventory: string[];
  challengeIndex: number;
  challengeBand: number[];
  turn: number;
  worldVersion: string;
  dayIndex: number;
  turnInDay: number;
  turnsPerDayTarget: number;
  timePhase: string;
  timePhaseLabel: string;
}

export interface CreateSessionResponse {
  sessionId: string;
  initialState: SessionState;
  challengeBand: number[];
}

export interface SubmitTurnRequest {
  expectedVersion: number;
  playerInput: string;
  clientTime: number;
}

export interface PlotPayload {
  text: string;
  citations: string[];
  confidence: number;
}

export interface OptionPayload {
  id: string;
  text: string;
  riskLevel: string;
  expectedEffect: string;
}

export interface DifficultyDeltaPayload {
  threat: number;
  loot: number;
  eventRate: number;
  bossProbability: number;
}

export interface StateDeltaPayload {
  stamina: number;
  noise: number;
  flagsAdded: string[];
}

export interface SubmitTurnResponse {
  turn: number;
  newVersion: number;
  plot: PlotPayload;
  options: OptionPayload[];
  difficultyDelta: DifficultyDeltaPayload;
  stateDelta: StateDeltaPayload;
}

export interface ChooseOptionRequest {
  expectedVersion: number;
  optionId: string;
}

export interface ChooseOptionResponse {
  turn: number;
  selected: string;
  applied: boolean;
  newVersion: number;
  stateDelta: StateDeltaPayload;
  plot: PlotPayload;
  options: OptionPayload[];
  difficultyDelta: DifficultyDeltaPayload;
}

export interface ComebackCardRequest {
  expectedVersion: number;
}

export interface ComebackCardResponse {
  applied: boolean;
  newVersion: number;
  effect: Record<string, string>;
  remainingCount: number;
}

export interface GenerateImageRequest {
  sessionId?: string;
  traceId?: string;
  prompt: string;
  preferredSource?: "ai" | "gallery" | "auto";
  style?: string;
  timeoutMs?: number;
}

export interface GenerateImageResponse {
  imageUrl: string;
  source: string; // "generated" | "gallery"
  fallback: boolean;
  fallbackReason?: string | null;
  provider?: string | null; // e.g. "dashscope" or "pexels"
  latencyMs?: number;
}

export interface GalleryImageItem {
  imageUrl: string;
  provider: string;
  author?: string;
  license?: string;
}

export type DiaryLevel = "L0" | "L1" | "L2";

export interface DiaryEntryView {
  level: DiaryLevel;
  fromTurn: number;
  toTurn: number;
  dayIndex: number;
  timePhase: string;
  timePhaseLabel: string;
  summary: string;
  tags: string[];
  timestamp: number;
}

export interface GameWorldInitRequest {
  worldTheme: string;
  eraStyle: string;
  survivalTone: string;
  keyFaction?: string;
  forbiddenRule?: string;
}

export interface GameWorldInitResponse {
  worldVersion: string;
  jobId: string;
  status: string;
  message: string;
}

export interface DefaultWorldResponse {
  worldVersion: string;
  title: string;
  description: string;
}

export interface WorldFactoryJobResponse {
  jobId: string;
  worldVersion: string;
  sourceType: string;
  status: string;
  progress: number;
  stage: string;
  errorMessage: string;
}
