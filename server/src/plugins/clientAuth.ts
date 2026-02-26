import type { FastifyReply, FastifyRequest } from "fastify";

type ClientAuthOptions = {
  token: string;
  enabled?: boolean;
  publicPaths?: string[];
};

export function createClientAuthHook(options: ClientAuthOptions) {
  const enabled = options.enabled ?? true;
  const publicPaths = new Set(options.publicPaths ?? []);
  return async (request: FastifyRequest, reply: FastifyReply) => {
    if (!enabled) {
      return;
    }
    if (publicPaths.has(request.url)) {
      return;
    }
    const token = request.headers["x-client-token"];
    if (token !== options.token) {
      return reply.code(401).send({ error: "unauthorized_client" });
    }
  };
}
