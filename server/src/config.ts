import { z } from "zod";

export const EnvSchema = z.object({
  PORT: z.coerce.number().default(8080),
  GEMINI_BASE_URL: z.string().url(),
  GEMINI_MODEL: z.string().min(1),
  GEMINI_API_KEY: z.string().min(20),
  CLIENT_TOKEN: z.string().min(12),
  RATE_LIMIT_PER_MINUTE: z.coerce.number().int().positive().default(30),
  BUILD_SHA: z.string().default("dev"),
  BUILD_TIME: z.string().default("unknown")
});

export type AppConfig = z.infer<typeof EnvSchema>;

export function parseConfig(env: NodeJS.ProcessEnv): AppConfig {
  return EnvSchema.parse(env);
}
