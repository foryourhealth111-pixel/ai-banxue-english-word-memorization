import { request } from "undici";

type ChatCompletionResponse = {
  choices?: Array<{
    message?: {
      content?: string;
    };
  }>;
};

export type GeminiClientOptions = {
  baseUrl: string;
  apiKey: string;
  model: string;
  timeoutMs?: number;
};

export class GeminiClient {
  private readonly baseUrl: string;
  private readonly apiKey: string;
  private readonly model: string;
  private readonly timeoutMs: number;

  constructor(options: GeminiClientOptions) {
    this.baseUrl = options.baseUrl.replace(/\/+$/, "");
    this.apiKey = options.apiKey;
    this.model = options.model;
    this.timeoutMs = options.timeoutMs ?? 8000;
  }

  async generateCoach(systemPrompt: string, userPrompt: string): Promise<string> {
    const url = `${this.baseUrl}/v1/chat/completions`;
    const payload = {
      model: this.model,
      temperature: 0.4,
      messages: [
        { role: "system", content: systemPrompt },
        { role: "user", content: userPrompt }
      ]
    };

    const { statusCode, body } = await request(url, {
      method: "POST",
      headers: {
        Authorization: `Bearer ${this.apiKey}`,
        "Content-Type": "application/json"
      },
      body: JSON.stringify(payload),
      headersTimeout: this.timeoutMs,
      bodyTimeout: this.timeoutMs
    });

    if (statusCode < 200 || statusCode >= 300) {
      const raw = await body.text();
      throw new Error(`provider_error_${statusCode}: ${raw.slice(0, 300)}`);
    }

    const parsed = (await body.json()) as ChatCompletionResponse;
    const content = parsed.choices?.[0]?.message?.content?.trim();
    if (!content) {
      throw new Error("provider_invalid_response");
    }
    return content;
  }
}
