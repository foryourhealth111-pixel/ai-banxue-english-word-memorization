import { z } from "zod";
import { buildCoachSystemPrompt, buildCoachUserPrompt } from "../lib/promptBuilder.js";
import { buildFallbackExplanation } from "../lib/fallbackCoach.js";

const CoachReqSchema = z.object({
  word: z.string().trim().regex(/^[A-Za-z-]{2,25}$/),
  locale: z.string().default("zh-CN"),
  providerBaseUrl: z.string().url().optional(),
  providerModel: z.string().min(1).max(200).optional(),
  providerApiKey: z.string().min(10).max(500).optional(),
  systemPrompt: z.string().min(20).max(12000).optional()
});

const CoachRespSchema = z.object({
  word: z.string(),
  explanation: z.string(),
  source: z.enum(["gemini-proxy", "local-fallback"])
});

export type CoachRouteDeps = {
  generateCoach: (args: {
    systemPrompt: string;
    userPrompt: string;
    providerBaseUrl?: string;
    providerModel?: string;
    providerApiKey?: string;
  }) => Promise<string>;
};

export async function registerCoachRoute(app: any, deps: CoachRouteDeps): Promise<void> {
  app.post("/v1/coach", async (request: any, reply: any) => {
    const parsed = CoachReqSchema.safeParse(request.body);
    if (!parsed.success) {
      return reply.code(400).send({
        error: "invalid_request",
        details: parsed.error.issues.map((it) => it.message)
      });
    }

    const { word, systemPrompt: customSystemPrompt, providerBaseUrl, providerModel, providerApiKey } = parsed.data;
    const systemPrompt = customSystemPrompt?.trim() || buildCoachSystemPrompt();
    const userPrompt = buildCoachUserPrompt(word);
    let explanation = "";
    let source: "gemini-proxy" | "local-fallback" = "gemini-proxy";
    try {
      explanation = await deps.generateCoach({
        systemPrompt,
        userPrompt,
        providerBaseUrl,
        providerModel,
        providerApiKey
      });
    } catch (error) {
      source = "local-fallback";
      explanation = buildFallbackExplanation(word);
      request.log.warn(
        {
          err: error,
          word
        },
        "coach_provider_failed_fallback_applied"
      );
    }

    const response = CoachRespSchema.parse({
      word,
      explanation,
      source
    });

    return reply.send(response);
  });
}
