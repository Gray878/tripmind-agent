export const API_CONFIG = {
  baseURL: process.env.NEXT_PUBLIC_API_URL || "http://localhost:8123",
  endpoints: {
    planStream: "/api/trip/plan/stream",
    plan: "/api/trip/plan",
    health: "/health",
  },
};

export interface SSEMessage {
  type: "step" | "progress" | "result" | "complete" | "error";
  content?: string;
  result?: string;
  errorMessage?: string;
  success?: boolean;
}

export interface PlanResult {
  success: boolean;
  result?: string;
  errorMessage?: string;
  data?: unknown;
  steps?: string[];
  stepCount?: number;
  tokenUsage?: number;
  duration?: number;
}

const TERMINAL_TYPES: Array<SSEMessage["type"]> = ["complete", "error"];

const parseJsonPayload = (payload: string): SSEMessage | null => {
  if (!payload.trim()) return null;

  try {
    const parsed = JSON.parse(payload);
    if (!parsed || typeof parsed !== "object") {
      return null;
    }

    const candidate = parsed as Partial<SSEMessage>;
    if (
      candidate.type === "step" ||
      candidate.type === "progress" ||
      candidate.type === "result" ||
      candidate.type === "complete" ||
      candidate.type === "error"
    ) {
      return candidate as SSEMessage;
    }
  } catch {
    // Ignore non-JSON payloads.
  }

  return null;
};

const mapEventToMessage = (eventName: string, payload: string): SSEMessage | null => {
  const trimmedPayload = payload.trim();
  const normalizedEvent = eventName.trim().toLowerCase();

  const jsonMessage = parseJsonPayload(payload);
  if (jsonMessage) {
    return jsonMessage;
  }

  switch (normalizedEvent) {
    case "step":
    case "step_start":
      return { type: "step", content: trimmedPayload };

    case "progress":
      return { type: "progress", content: trimmedPayload };

    case "think":
    case "result":
      return { type: "result", content: payload };

    case "error":
    case "failed":
      return {
        type: "error",
        content: payload,
        errorMessage: trimmedPayload || "Unknown error",
      };

    case "finished":
      return { type: "complete", success: true };

    case "complete":
    case "done":
      if (!trimmedPayload || /^(finished|done)$/i.test(trimmedPayload)) {
        return { type: "complete", success: true };
      }
      return { type: "complete", success: true, result: payload };

    case "":
      if (!payload) {
        return null;
      }
      return { type: "result", content: payload };

    default:
      if (!payload) {
        return null;
      }
      return { type: "result", content: payload };
  }
};

export class ApiClient {
  private baseURL: string;

  constructor(baseURL: string = API_CONFIG.baseURL) {
    this.baseURL = baseURL;
  }

  async createPlanStream(
    userPrompt: string,
    userId?: string,
    onMessage?: (data: SSEMessage) => void,
  ): Promise<void> {
    const response = await fetch(`${this.baseURL}${API_CONFIG.endpoints.planStream}`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        userPrompt,
        userId: userId || "guest_user",
      }),
    });

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    const reader = response.body?.getReader();
    const decoder = new TextDecoder();

    if (!reader) {
      throw new Error("Cannot read streaming response body");
    }

    let buffer = "";
    let eventName = "";
    let dataLines: string[] = [];

    const emitEvent = (): SSEMessage | null => {
      if (!eventName && dataLines.length === 0) {
        return null;
      }

      const payload = dataLines.join("\n");
      const mapped = mapEventToMessage(eventName, payload);
      eventName = "";
      dataLines = [];
      return mapped;
    };

    while (true) {
      const { done, value } = await reader.read();

      if (done) {
        buffer += decoder.decode();
      } else if (value) {
        buffer += decoder.decode(value, { stream: true });
      }

      const lines = buffer.split(/\r?\n/);
      if (!done) {
        buffer = lines.pop() || "";
      } else {
        buffer = "";
      }

      for (const rawLine of lines) {
        const line = rawLine.trimEnd();

        if (!line) {
          const message = emitEvent();
          if (message) {
            onMessage?.(message);
            if (TERMINAL_TYPES.includes(message.type)) {
              return;
            }
          }
          continue;
        }

        if (line.startsWith(":")) {
          continue;
        }

        if (line.startsWith("event:")) {
          eventName = line.slice(6).trim();
          continue;
        }

        if (line.startsWith("data:")) {
          let dataLine = line.slice(5);
          if (dataLine.startsWith(" ")) {
            dataLine = dataLine.slice(1);
          }
          dataLines.push(dataLine);
        }
      }

      if (done) {
        break;
      }
    }

    const finalMessage = emitEvent();
    if (finalMessage) {
      onMessage?.(finalMessage);
    }
  }

  async createPlan(userPrompt: string, userId?: string): Promise<PlanResult> {
    const response = await fetch(`${this.baseURL}${API_CONFIG.endpoints.plan}`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        userPrompt,
        userId: userId || "guest_user",
      }),
    });

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    return response.json();
  }

  async healthCheck(): Promise<string> {
    const response = await fetch(`${this.baseURL}${API_CONFIG.endpoints.health}`);
    return response.text();
  }
}

export const apiClient = new ApiClient();
