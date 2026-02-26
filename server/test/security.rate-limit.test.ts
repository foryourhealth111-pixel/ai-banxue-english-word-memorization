import { describe, expect, it } from "vitest";
import { buildApp } from "../src/index.js";
import { makeTestConfig } from "./helpers.js";

describe("rate limit and auth", () => {
  it("returns 401 without client token", async () => {
    const app = buildApp({
      config: makeTestConfig(),
      disableRateLimit: true,
      generateCoach: async () => "ok"
    });

    const response = await app.inject({
      method: "POST",
      url: "/v1/coach",
      payload: { word: "abandon" }
    });

    expect(response.statusCode).toBe(401);
    await app.close();
  });

  it("returns 429 when burst exceeds limit on protected route", async () => {
    const app = buildApp({
      config: makeTestConfig(),
      generateCoach: async () => "ok"
    });

    const headers = { "x-client-token": "test-client-token" };
    await app.inject({ method: "POST", url: "/v1/coach", headers, payload: { word: "abandon" } });
    await app.inject({ method: "POST", url: "/v1/coach", headers, payload: { word: "abandon" } });
    const response = await app.inject({
      method: "POST",
      url: "/v1/coach",
      headers,
      payload: { word: "abandon" }
    });

    expect(response.statusCode).toBe(429);
    await app.close();
  });
});
