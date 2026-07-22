import {
  createHandler,
  MAX_BODY_BYTES,
  MAX_CATEGORY_COUNT,
  MAX_CATEGORY_LENGTH,
  MAX_TEXT_LENGTH,
  type NoteMetadata,
  type ProcessNoteRequest,
  UpstreamHttpError,
} from "./handler.ts"
import "./index.ts"

function assert(condition: unknown, message: string): asserts condition {
  if (!condition) throw new Error(message)
}

function assertEquals<T>(actual: T, expected: T, message: string): void {
  assert(
    Object.is(actual, expected),
    `${message}: expected ${String(expected)}, received ${String(actual)}`,
  )
}

const validRequest: ProcessNoteRequest = {
  text: "Ricordami la riunione di domani alle 10 in ufficio.",
  categoryNames: ["Lavoro", "Personale"],
  currentDate: "2026-07-22",
}

const validMetadata: NoteMetadata = {
  title: "Riunione ufficio",
  improvedText: "Riunione in ufficio domani alle 10:00.",
  date: "2026-07-23",
  time: "10:00",
  location: "Ufficio",
  category: "Lavoro",
}

function geminiEnvelope(metadata: unknown): unknown {
  return {
    candidates: [{
      content: {
        parts: [{ text: JSON.stringify(metadata) }],
      },
    }],
  }
}

function requestFor(
  body: unknown = validRequest,
  options: { method?: string; authenticated?: boolean; rawBody?: string } = {},
): Request {
  const headers = new Headers({ "Content-Type": "application/json" })
  if (options.authenticated !== false) {
    headers.set("Authorization", "Bearer test-session-placeholder")
  }
  return new Request("https://example.invalid/functions/v1/process-note-ai", {
    method: options.method ?? "POST",
    headers,
    body: options.method === "GET" || options.method === "OPTIONS"
      ? undefined
      : options.rawBody ?? JSON.stringify(body),
  })
}

async function responseBody(response: Response): Promise<Record<string, unknown>> {
  return await response.json() as Record<string, unknown>
}

function successHandler() {
  return createHandler({
    generateNote: () => Promise.resolve(geminiEnvelope(validMetadata)),
  })
}

Deno.test("rejects methods other than POST and OPTIONS", async () => {
  const response = await successHandler()(requestFor(undefined, { method: "GET" }))
  assertEquals(response.status, 405, "unexpected status")
  assertEquals(response.headers.get("allow"), "POST, OPTIONS", "missing Allow header")
})

Deno.test("accepts an empty CORS preflight without invoking the dependency", async () => {
  let invoked = false
  const handler = createHandler({
    generateNote: () => {
      invoked = true
      return Promise.resolve(geminiEnvelope(validMetadata))
    },
  })
  const response = await handler(requestFor(undefined, { method: "OPTIONS" }))
  assertEquals(response.status, 204, "unexpected preflight status")
  assert(!invoked, "preflight invoked the AI dependency")
})

Deno.test("rejects requests without an authorization header", async () => {
  let invoked = false
  const handler = createHandler({
    generateNote: () => {
      invoked = true
      return Promise.resolve(geminiEnvelope(validMetadata))
    },
  })
  const response = await handler(requestFor(validRequest, { authenticated: false }))
  assertEquals(response.status, 401, "unexpected status")
  assert(!invoked, "unauthenticated request invoked the AI dependency")
})

Deno.test("rejects malformed or incomplete bodies", async () => {
  const handler = successHandler()
  const malformed = await handler(requestFor(undefined, { rawBody: "{" }))
  const incomplete = await handler(requestFor({ text: "Nota" }))
  assertEquals(malformed.status, 400, "malformed JSON was accepted")
  assertEquals(incomplete.status, 400, "incomplete body was accepted")
})

Deno.test("rejects sensitive and unexpected body fields", async () => {
  const forbiddenFields = [
    "userId",
    "email",
    "jwt",
    "token",
    "key",
    "apiKey",
    "secret",
    "unexpected",
  ]
  const handler = successHandler()

  for (const field of forbiddenFields) {
    const response = await handler(requestFor({
      ...validRequest,
      [field]: "placeholder",
    }))
    assertEquals(response.status, 400, `field ${field} was accepted`)
  }
})

Deno.test("rejects payloads larger than the byte limit", async () => {
  const oversized = JSON.stringify({
    ...validRequest,
    text: "x".repeat(MAX_BODY_BYTES),
  })
  const response = await successHandler()(
    requestFor(undefined, { rawBody: oversized }),
  )
  assertEquals(response.status, 413, "oversized payload was accepted")
})

Deno.test("rejects text longer than the character limit", async () => {
  const response = await successHandler()(requestFor({
    ...validRequest,
    text: "x".repeat(MAX_TEXT_LENGTH + 1),
  }))
  assertEquals(response.status, 400, "oversized text was accepted")
})

Deno.test("rejects invalid category collections", async () => {
  const handler = successHandler()
  const tooMany = await handler(requestFor({
    ...validRequest,
    categoryNames: Array.from(
      { length: MAX_CATEGORY_COUNT + 1 },
      (_value, index) => `Categoria ${index}`,
    ),
  }))
  const tooLong = await handler(requestFor({
    ...validRequest,
    categoryNames: ["x".repeat(MAX_CATEGORY_LENGTH + 1)],
  }))
  const nonString = await handler(requestFor({
    ...validRequest,
    categoryNames: [123],
  }))
  assertEquals(tooMany.status, 400, "too many categories were accepted")
  assertEquals(tooLong.status, 400, "long category was accepted")
  assertEquals(nonString.status, 400, "non-string category was accepted")
})

Deno.test("returns a controlled timeout response", async () => {
  const handler = createHandler({
    timeoutMs: 5,
    generateNote: (_request, signal) => new Promise((_resolve, reject) => {
      signal.addEventListener("abort", () => {
        reject(new DOMException("aborted", "AbortError"))
      }, { once: true })
    }),
  })
  const response = await handler(requestFor())
  const body = await responseBody(response)
  assertEquals(response.status, 504, "unexpected timeout status")
  assertEquals(
    (body.error as Record<string, unknown>).code,
    "UPSTREAM_TIMEOUT",
    "unexpected timeout code",
  )
})

Deno.test("maps upstream failures without exposing their body", async () => {
  const handler = createHandler({
    generateNote: () => {
      throw new UpstreamHttpError(500)
    },
  })
  const response = await handler(requestFor())
  const text = await response.text()
  assertEquals(response.status, 502, "unexpected upstream status")
  assert(!text.includes("500"), "upstream details leaked into the response")
})

Deno.test("maps upstream rate limiting and preserves safe retry timing", async () => {
  const handler = createHandler({
    generateNote: () => {
      throw new UpstreamHttpError(429, 30)
    },
  })
  const response = await handler(requestFor())
  assertEquals(response.status, 429, "unexpected rate-limit status")
  assertEquals(response.headers.get("retry-after"), "30", "missing retry timing")
})

Deno.test("rejects invalid AI responses", async () => {
  const handler = createHandler({
    generateNote: () => Promise.resolve({ candidates: [] }),
  })
  const response = await handler(requestFor())
  const body = await responseBody(response)
  assertEquals(response.status, 502, "invalid AI response was accepted")
  assertEquals(
    (body.error as Record<string, unknown>).code,
    "INVALID_AI_RESPONSE",
    "unexpected invalid-response code",
  )
})

Deno.test("returns only validated note metadata", async () => {
  const response = await successHandler()(requestFor())
  const body = await responseBody(response)
  assertEquals(response.status, 200, "valid response was rejected")
  assertEquals(JSON.stringify(body), JSON.stringify(validMetadata), "response changed")
  assertEquals(Object.keys(body).length, 6, "response contains extra fields")
})

Deno.test("does not expose sensitive upstream values", async () => {
  const marker = "SHOULD_NOT_REACH_THE_CLIENT"
  const handler = createHandler({
    generateNote: () => Promise.resolve(geminiEnvelope({
      ...validMetadata,
      secret: marker,
    })),
  })
  const response = await handler(requestFor())
  const text = await response.text()
  assertEquals(response.status, 502, "response with extra field was accepted")
  assert(!text.includes(marker), "upstream value leaked into the response")
})

Deno.test("keeps JWT verification enabled for the function", async () => {
  const config = await Deno.readTextFile(
    new URL("../../config.toml", import.meta.url),
  )
  const section = config.match(
    /\[functions\.process-note-ai\]([\s\S]*?)(?=\n\[|$)/,
  )?.[1]
  assert(section !== undefined, "function configuration is missing")
  assert(
    /^\s*verify_jwt\s*=\s*true\s*$/m.test(section),
    "gateway JWT verification is not enabled",
  )
})

Deno.test("keeps credentials server-side and out of the request URL", async () => {
  const source = await Deno.readTextFile(new URL("./index.ts", import.meta.url))
  assert(
    source.includes('Deno.env.get("GEMINI_API_KEY_V2")'),
    "server-side credential lookup is missing",
  )
  assert(source.includes('"x-goog-api-key": credential'), "credential header is missing")
  assert(!source.includes("?key="), "credential is present in the request URL")
  assert(!source.includes("console."), "entrypoint contains logging")
})
