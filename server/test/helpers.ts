import type { AppConfig } from "../src/config.js";

export function makeTestConfig(): AppConfig {
  return {
    PORT: 8080,
    GEMINI_BASE_URL: "https://example.com",
    GEMINI_MODEL: "test-model",
    GEMINI_API_KEY: "test-api-key-123456789012",
    CLIENT_TOKEN: "test-client-token",
    RATE_LIMIT_PER_MINUTE: 2,
    BUILD_SHA: "test-sha",
    BUILD_TIME: "2026-02-26T00:00:00Z"
  };
}
