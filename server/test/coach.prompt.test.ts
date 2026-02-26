import { describe, expect, it } from "vitest";
import { buildCoachSystemPrompt, buildCoachUserPrompt } from "../src/lib/promptBuilder.js";

describe("prompt builder", () => {
  it("contains required teaching structure", () => {
    const system = buildCoachSystemPrompt();
    expect(system).toContain("一句话核心");
    expect(system).toContain("起源故事");
    expect(system).toContain("高频例句");
    expect(system).toContain("300~600字");
  });

  it("injects candidate word in user prompt", () => {
    const user = buildCoachUserPrompt("obscure");
    expect(user).toContain("obscure");
    expect(user).toContain("不要被其他信息误导");
  });
});
