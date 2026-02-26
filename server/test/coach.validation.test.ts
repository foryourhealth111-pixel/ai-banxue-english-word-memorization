import { describe, expect, it } from "vitest";
import { buildApp } from "../src/index.js";
import { makeTestConfig } from "./helpers.js";

describe("coach validation", () => {
  it("rejects invalid payload", async () => {
    const app = buildApp({
      config: makeTestConfig(),
      disableRateLimit: true,
      generateCoach: async () => "dummy"
    });

    const response = await app.inject({
      method: "POST",
      url: "/v1/coach",
      headers: { "x-client-token": "test-client-token" },
      payload: { word: "1" }
    });

    expect(response.statusCode).toBe(400);
    const data = response.json() as { error: string };
    expect(data.error).toBe("invalid_request");
    await app.close();
  });

  it("accepts valid payload and returns explanation", async () => {
    const app = buildApp({
      config: makeTestConfig(),
      disableRateLimit: true,
      generateCoach: async () => "这是讲解"
    });

    const response = await app.inject({
      method: "POST",
      url: "/v1/coach",
      headers: { "x-client-token": "test-client-token" },
      payload: { word: "abandon" }
    });

    expect(response.statusCode).toBe(200);
    const data = response.json() as { word: string; explanation: string; source: string };
    expect(data.word).toBe("abandon");
    expect(data.explanation).toContain("讲解");
    expect(data.source).toBe("gemini-proxy");
    await app.close();
  });

  it("falls back to local explanation when provider fails", async () => {
    const app = buildApp({
      config: makeTestConfig(),
      disableRateLimit: true,
      generateCoach: async () => {
        throw new Error("provider_error_500");
      }
    });

    const response = await app.inject({
      method: "POST",
      url: "/v1/coach",
      headers: { "x-client-token": "test-client-token" },
      payload: { word: "abandon" }
    });

    expect(response.statusCode).toBe(200);
    const data = response.json() as { word: string; explanation: string; source: string };
    expect(data.word).toBe("abandon");
    expect(data.explanation).toContain("单词与音节拆分");
    expect(data.source).toBe("local-fallback");
    await app.close();
  });

  it("passes provider and prompt overrides to generator", async () => {
    type CapturedArgs = {
      providerBaseUrl?: string;
      providerModel?: string;
      providerApiKey?: string;
      systemPrompt?: string;
      userPrompt?: string;
    };
    let captured: CapturedArgs = {};
    const app = buildApp({
      config: makeTestConfig(),
      disableRateLimit: true,
      generateCoach: async (args) => {
        captured = {
          providerBaseUrl: args.providerBaseUrl,
          providerModel: args.providerModel,
          providerApiKey: args.providerApiKey,
          systemPrompt: args.systemPrompt,
          userPrompt: args.userPrompt
        };
        return "覆盖配置讲解";
      }
    });

    const response = await app.inject({
      method: "POST",
      url: "/v1/coach",
      headers: { "x-client-token": "test-client-token" },
      payload: {
        word: "threshold",
        providerBaseUrl: "https://example.com",
        providerModel: "custom-model",
        providerApiKey: "test-api-key-1234567890",
        systemPrompt: "请严格按我的结构输出，并给出清晰的词源和场景化示例。"
      }
    });

    expect(response.statusCode).toBe(200);
    expect(captured.providerBaseUrl).toBe("https://example.com");
    expect(captured.providerModel).toBe("custom-model");
    expect(captured.providerApiKey).toBe("test-api-key-1234567890");
    expect(captured.systemPrompt).toBe("请严格按我的结构输出，并给出清晰的词源和场景化示例。");
    expect(captured.userPrompt).toContain("threshold");
    await app.close();
  });
});
