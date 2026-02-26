import { describe, expect, it } from "vitest";
import { redactRecord } from "../src/lib/requestLogger.js";

describe("request logger redaction", () => {
  it("redacts secret fields", () => {
    const input = {
      authorization: "Bearer aaa",
      nested: {
        xApiKey: "bbb",
        normal: "ccc"
      }
    };

    const out = redactRecord(input) as {
      authorization: string;
      nested: { xApiKey: string; normal: string };
    };

    expect(out.authorization).toBe("***redacted***");
    expect(out.nested.xApiKey).toBe("***redacted***");
    expect(out.nested.normal).toBe("ccc");
  });
});
