import "dotenv/config";
import { fileURLToPath } from "node:url";
import Fastify from "fastify";
import { parseConfig, type AppConfig } from "./config.js";
import { GeminiClient } from "./lib/geminiClient.js";
import { redactRecord } from "./lib/requestLogger.js";
import { createClientAuthHook } from "./plugins/clientAuth.js";
import { createRateLimitHook } from "./plugins/rateLimit.js";
import { registerCoachRoute } from "./routes/coach.js";
import { registerVersionRoute } from "./routes/version.js";

export type BuildAppOptions = {
  config: AppConfig;
  disableAuth?: boolean;
  disableRateLimit?: boolean;
  generateCoach?: (args: {
    systemPrompt: string;
    userPrompt: string;
    providerBaseUrl?: string;
    providerModel?: string;
    providerApiKey?: string;
  }) => Promise<string>;
};

export function buildApp(options: BuildAppOptions) {
  const publicProbePaths = ["/health", "/version"];
  const app = Fastify({
    logger: {
      level: "info",
      serializers: {
        req(request) {
          return redactRecord({
            method: request.method,
            url: request.url,
            headers: request.headers
          }) as Record<string, unknown>;
        }
      }
    }
  });

  app.addHook(
    "onRequest",
    createClientAuthHook({
      token: options.config.CLIENT_TOKEN,
      enabled: !options.disableAuth,
      publicPaths: publicProbePaths
    })
  );

  app.addHook(
    "onRequest",
    createRateLimitHook({
      maxPerMinute: options.config.RATE_LIMIT_PER_MINUTE,
      enabled: !options.disableRateLimit,
      excludePaths: publicProbePaths
    })
  );

  app.get("/health", async () => ({ ok: true }));
  void registerVersionRoute(app, {
    sha: options.config.BUILD_SHA,
    buildTime: options.config.BUILD_TIME
  });

  const defaultProvider = {
    baseUrl: options.config.GEMINI_BASE_URL,
    apiKey: options.config.GEMINI_API_KEY,
    model: options.config.GEMINI_MODEL
  };
  const gemini = new GeminiClient(defaultProvider);

  void registerCoachRoute(app, {
    generateCoach: options.generateCoach ?? (async (args) => {
      const useOverride = Boolean(args.providerBaseUrl || args.providerModel || args.providerApiKey);
      if (!useOverride) {
        return gemini.generateCoach(args.systemPrompt, args.userPrompt);
      }
      const client = new GeminiClient({
        baseUrl: args.providerBaseUrl ?? defaultProvider.baseUrl,
        model: args.providerModel ?? defaultProvider.model,
        apiKey: args.providerApiKey ?? defaultProvider.apiKey
      });
      return client.generateCoach(args.systemPrompt, args.userPrompt);
    })
  });

  return app;
}

async function start() {
  const config = parseConfig(process.env);
  const app = buildApp({ config });
  await app.listen({ port: config.PORT, host: "0.0.0.0" });
}

if (process.argv[1] && fileURLToPath(import.meta.url) === process.argv[1]) {
  start().catch((error) => {
    const message = error instanceof Error ? error.message : String(error);
    const issues = (error as { issues?: unknown[] })?.issues;
    // eslint-disable-next-line no-console
    console.error(`[server_start_failed] ${message}`);
    if (Array.isArray(issues) && issues.length > 0) {
      // eslint-disable-next-line no-console
      console.error("[server_start_failed] validation_issues=", JSON.stringify(issues, null, 2));
    }
    process.exit(1);
  });
}
