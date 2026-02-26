const SECRET_KEYS = ["authorization", "api-key", "x-api-key", "xapikey", "gemini_api_key", "client_token"];

function isSecretKey(key: string): boolean {
  const lower = key.toLowerCase();
  return SECRET_KEYS.some((secret) => lower.includes(secret));
}

export function redactRecord(input: unknown): unknown {
  if (Array.isArray(input)) {
    return input.map(redactRecord);
  }
  if (input && typeof input === "object") {
    const out: Record<string, unknown> = {};
    for (const [k, v] of Object.entries(input as Record<string, unknown>)) {
      out[k] = isSecretKey(k) ? "***redacted***" : redactRecord(v);
    }
    return out;
  }
  return input;
}
