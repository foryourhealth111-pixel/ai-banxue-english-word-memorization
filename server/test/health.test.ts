import { describe, expect, it } from "vitest";
import { buildApp } from "../src/index.js";
import { makeTestConfig } from "./helpers.js";

describe("health route", () => {
  it("returns ok without auth", async () => {
    const app = buildApp({
      config: makeTestConfig(),
      disableRateLimit: true,
      generateCoach: async () => "ok"
    });

    const response = await app.inject({
      method: "GET",
      url: "/health"
    });

    expect(response.statusCode).toBe(200);
    expect(response.json()).toEqual({ ok: true });
    await app.close();
  });

  it("returns build metadata on /version without auth", async () => {
    const app = buildApp({
      config: makeTestConfig(),
      disableRateLimit: true,
      generateCoach: async () => "ok"
    });

    const response = await app.inject({
      method: "GET",
      url: "/version"
    });

    expect(response.statusCode).toBe(200);
    expect(response.json()).toEqual({
      sha: "test-sha",
      buildTime: "2026-02-26T00:00:00Z"
    });
    await app.close();
  });
});
