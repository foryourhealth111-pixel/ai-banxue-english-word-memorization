import type { FastifyReply, FastifyRequest } from "fastify";

type Bucket = {
  minute: number;
  count: number;
};

type RateLimitOptions = {
  maxPerMinute: number;
  enabled?: boolean;
  excludePaths?: string[];
};

export function createRateLimitHook(options: RateLimitOptions) {
  const enabled = options.enabled ?? true;
  const excludedPaths = new Set(options.excludePaths ?? []);
  const buckets = new Map<string, Bucket>();

  return async (request: FastifyRequest, reply: FastifyReply) => {
    if (!enabled) {
      return;
    }
    if (excludedPaths.has(request.url)) {
      return;
    }

    const nowMinute = Math.floor(Date.now() / 60000);
    const clientToken = String(request.headers["x-client-token"] ?? "anonymous");
    const ip = request.ip ?? "unknown";
    const key = `${ip}:${clientToken}`;
    const prev = buckets.get(key);
    if (!prev || prev.minute !== nowMinute) {
      buckets.set(key, { minute: nowMinute, count: 1 });
      return;
    }

    if (prev.count >= options.maxPerMinute) {
      return reply.code(429).send({ error: "rate_limited" });
    }

    prev.count += 1;
  };
}
