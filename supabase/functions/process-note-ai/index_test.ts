import {
  type AiUsageStore,
  createHandler,
  MAX_BODY_BYTES,
  MAX_CATEGORY_COUNT,
  MAX_CATEGORY_LENGTH,
  MAX_TEXT_LENGTH,
  type AiUsageMetadata,
  type GeneratedNote,
  type HandlerDependencies,
  type NoteMetadata,
  type ProcessNoteRequest,
  type ReservationDecision,
  UpstreamHttpError,
} from "./handler.ts"
import {
  createGeminiRequestBody,
  extractUsageMetadata,
  requireGeminiCredential,
} from "./index.ts"

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
  requestId: "11111111-1111-4111-8111-111111111111",
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

Deno.test("disables Gemini thinking without changing the model request", () => {
  const body = createGeminiRequestBody(validRequest)
  const generationConfig = body.generationConfig as Record<string, unknown>
  const thinkingConfig = generationConfig.thinkingConfig as Record<string, unknown>

  assertEquals(thinkingConfig.thinkingBudget, 0, "thinking is not disabled")
  assertEquals(generationConfig.maxOutputTokens, 1_024, "output limit changed")
})

Deno.test("extracts Gemini usage metadata", () => {
  const usage = extractUsageMetadata({
    usageMetadata: {
      promptTokenCount: 101,
      candidatesTokenCount: 202,
      thoughtsTokenCount: 0,
      cachedContentTokenCount: 33,
      totalTokenCount: 303,
    },
  })

  assert(usage !== null, "usage metadata was not extracted")
  assertEquals(usage.promptTokenCount, 101, "wrong prompt token count")
  assertEquals(usage.candidatesTokenCount, 202, "wrong candidate token count")
  assertEquals(usage.thoughtsTokenCount, 0, "wrong thought token count")
  assertEquals(usage.cachedContentTokenCount, 33, "wrong cached token count")
  assertEquals(usage.totalTokenCount, 303, "wrong total token count")
})

Deno.test("accepts a Gemini response without usage metadata", () => {
  assertEquals(
    extractUsageMetadata({ candidates: [] }),
    null,
    "missing usage metadata did not map to null",
  )
})

function generatedResponse(
  response: unknown,
  usageMetadata: AiUsageMetadata | null = null,
): GeneratedNote {
  return { response, usageMetadata }
}

function geminiEnvelope(
  metadata: unknown,
  usageMetadata: AiUsageMetadata | null = null,
): GeneratedNote {
  return generatedResponse({
    candidates: [{
      content: {
        parts: [{ text: JSON.stringify(metadata) }],
      },
    }],
  }, usageMetadata)
}

const validUsage: AiUsageMetadata = {
  promptTokenCount: 101,
  candidatesTokenCount: 202,
  thoughtsTokenCount: 0,
  cachedContentTokenCount: 33,
  totalTokenCount: 303,
}

const authenticatedUserId = "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"

class FakeUsageStore implements AiUsageStore {
  reservation: ReservationDecision = { decision: "RESERVED" }
  reserveCount = 0
  lastReservedRequestId = ""
  finalizeAttempts = 0
  finalizeFailuresRemaining = 0
  finalizations: Parameters<AiUsageStore["finalize"]>[0][] = []

  reserve(
    _userId: string,
    requestId: string,
    _requestFingerprint: string,
    _inputCharacterCount: number,
  ): Promise<ReservationDecision> {
    this.reserveCount++
    this.lastReservedRequestId = requestId
    return Promise.resolve(this.reservation)
  }

  finalize(
    request: Parameters<AiUsageStore["finalize"]>[0],
  ): Promise<void> {
    this.finalizeAttempts++
    this.finalizations.push(request)
    if (this.finalizeFailuresRemaining > 0) {
      this.finalizeFailuresRemaining--
      return Promise.reject(new Error("transient finalization failure"))
    }
    return Promise.resolve()
  }
}

function testHandler(
  dependencies: Partial<HandlerDependencies> &
    Pick<HandlerDependencies, "generateNote">,
) {
  return createHandler({
    authenticateUser: (request) => Promise.resolve(
      request.headers.has("authorization") ? authenticatedUserId : null,
    ),
    validateAiConfiguration: () => undefined,
    usageStore: new FakeUsageStore(),
    ...dependencies,
  })
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
  return testHandler({
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
  const handler = testHandler({
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
  const handler = testHandler({
    generateNote: () => {
      invoked = true
      return Promise.resolve(geminiEnvelope(validMetadata))
    },
  })
  const response = await handler(requestFor(validRequest, { authenticated: false }))
  assertEquals(response.status, 401, "unexpected status")
  const body = await responseBody(response)
  assertEquals(
    (body.error as Record<string, unknown>).code,
    "AUTHENTICATION_INVALID",
    "unexpected authentication error code",
  )
  assert(!invoked, "unauthenticated request invoked the AI dependency")
})

Deno.test("maps authentication infrastructure failures to service unavailable", async () => {
  const response = await testHandler({
    authenticateUser: () => {
      throw new Error("Supabase Auth unavailable")
    },
    generateNote: () => Promise.resolve(geminiEnvelope(validMetadata)),
  })(requestFor())
  const body = await responseBody(response)

  assertEquals(response.status, 503, "Auth outage was reported as invalid JWT")
  assertEquals(
    (body.error as Record<string, unknown>).code,
    "SERVICE_UNAVAILABLE",
    "unexpected Auth outage code",
  )
})

Deno.test("keeps partial usage metadata and nulls missing fields", () => {
  const usage = extractUsageMetadata({
    usageMetadata: {
      promptTokenCount: 12,
      totalTokenCount: 34,
    },
  })

  assert(usage !== null, "partial usage metadata was discarded")
  assertEquals(usage.promptTokenCount, 12, "valid prompt count was lost")
  assertEquals(usage.candidatesTokenCount, null, "missing candidate count was not null")
  assertEquals(usage.thoughtsTokenCount, null, "missing thought count was not null")
  assertEquals(usage.cachedContentTokenCount, null, "missing cache count was not null")
  assertEquals(usage.totalTokenCount, 34, "valid total count was lost")
})

Deno.test("malformed usage metadata never invalidates a valid note", async () => {
  const malformedCases: unknown[] = [
    "unexpected",
    {
      promptTokenCount: "12",
      candidatesTokenCount: -1,
      thoughtsTokenCount: 1.5,
      cachedContentTokenCount: null,
      totalTokenCount: 1e20,
      futureTokenCount: 999,
    },
  ]

  for (const usageMetadata of malformedCases) {
    const usage = extractUsageMetadata({ usageMetadata })
    assert(usage !== null, "present malformed metadata was discarded")
    assertEquals(usage.promptTokenCount, null, "malformed prompt count was accepted")
    assertEquals(usage.candidatesTokenCount, null, "negative candidate count was accepted")
    assertEquals(usage.thoughtsTokenCount, null, "fractional thought count was accepted")
    assertEquals(usage.cachedContentTokenCount, null, "null cache count changed")
    assertEquals(usage.totalTokenCount, null, "unsafe total count was accepted")

    const response = await testHandler({
      generateNote: () => Promise.resolve(geminiEnvelope(validMetadata, usage)),
    })(requestFor())
    assertEquals(response.status, 200, "malformed telemetry rejected a valid note")
  }
})

Deno.test("missing Gemini key is rejected before reservation", async () => {
  let invoked = false
  const usageStore = new FakeUsageStore()
  const response = await testHandler({
    validateAiConfiguration: () => {
      requireGeminiCredential(() => undefined)
    },
    usageStore,
    generateNote: () => {
      invoked = true
      return Promise.resolve(geminiEnvelope(validMetadata))
    },
  })(requestFor())
  const body = await responseBody(response)

  assertEquals(response.status, 503, "missing key did not fail closed")
  assertEquals(
    (body.error as Record<string, unknown>).code,
    "SERVICE_UNAVAILABLE",
    "missing key returned an unexpected error",
  )
  assertEquals(usageStore.reserveCount, 0, "missing key consumed quota")
  assert(!invoked, "missing key invoked Gemini")
})

Deno.test("Gemini credential is normalized before use", () => {
  assertEquals(
    requireGeminiCredential(() => "  server-key  "),
    "server-key",
    "credential whitespace was sent upstream",
  )
})

Deno.test("maps quota and concurrency decisions without invoking Gemini", async () => {
  const cases: {
    reservation: ReservationDecision
    expectedStatus: number
    expectedCode: string
  }[] = [
    {
      reservation: {
        decision: "MONTHLY_QUOTA_EXHAUSTED",
        retryAfterSeconds: 60,
      },
      expectedStatus: 429,
      expectedCode: "MONTHLY_QUOTA_EXHAUSTED",
    },
    {
      reservation: {
        decision: "DAILY_QUOTA_EXHAUSTED",
        retryAfterSeconds: 30,
      },
      expectedStatus: 429,
      expectedCode: "DAILY_QUOTA_EXHAUSTED",
    },
    {
      reservation: { decision: "RATE_LIMITED", retryAfterSeconds: 12 },
      expectedStatus: 429,
      expectedCode: "RATE_LIMITED",
    },
    {
      reservation: {
        decision: "CONCURRENT_REQUEST",
        retryAfterSeconds: 10,
      },
      expectedStatus: 409,
      expectedCode: "CONCURRENT_REQUEST",
    },
    {
      reservation: {
        decision: "REQUEST_IN_PROGRESS",
        retryAfterSeconds: 9,
      },
      expectedStatus: 409,
      expectedCode: "REQUEST_IN_PROGRESS",
    },
    {
      reservation: { decision: "IDEMPOTENCY_CONFLICT" },
      expectedStatus: 409,
      expectedCode: "IDEMPOTENCY_CONFLICT",
    },
  ]

  for (const testCase of cases) {
    let invoked = false
    const usageStore = new FakeUsageStore()
    usageStore.reservation = testCase.reservation
    const response = await testHandler({
      usageStore,
      generateNote: () => {
        invoked = true
        return Promise.resolve(geminiEnvelope(validMetadata))
      },
    })(requestFor())
    const body = await responseBody(response)
    const error = body.error as Record<string, unknown>

    assertEquals(response.status, testCase.expectedStatus, "unexpected status")
    assertEquals(error.code, testCase.expectedCode, "unexpected quota code")
    assert(!invoked, `${testCase.expectedCode} invoked Gemini`)
  }
})

Deno.test("replays a completed idempotent request without invoking Gemini", async () => {
  let invoked = false
  const usageStore = new FakeUsageStore()
  usageStore.reservation = {
    decision: "REPLAY_SUCCESS",
    resultPayload: validMetadata,
  }
  const response = await testHandler({
    usageStore,
    generateNote: () => {
      invoked = true
      return Promise.resolve(geminiEnvelope(validMetadata))
    },
  })(requestFor())

  assertEquals(response.status, 200, "replayed request failed")
  assert(!invoked, "replayed request invoked Gemini")
  assertEquals(usageStore.finalizations.length, 0, "replay was finalized again")
})

Deno.test("replays a failed idempotent request without invoking Gemini", async () => {
  let invoked = false
  const usageStore = new FakeUsageStore()
  usageStore.reservation = {
    decision: "REPLAY_FAILURE",
    responseStatus: 504,
    errorCode: "UPSTREAM_TIMEOUT",
  }
  const response = await testHandler({
    usageStore,
    generateNote: () => {
      invoked = true
      return Promise.resolve(geminiEnvelope(validMetadata))
    },
  })(requestFor())
  const body = await responseBody(response)

  assertEquals(response.status, 504, "failed request replay changed status")
  assertEquals(
    (body.error as Record<string, unknown>).code,
    "UPSTREAM_TIMEOUT",
    "failed request replay changed error code",
  )
  assert(!invoked, "failed request replay invoked Gemini")
  assertEquals(usageStore.finalizations.length, 0, "failed replay was finalized again")
})

Deno.test("finalizes successful requests with internal token usage", async () => {
  const usageStore = new FakeUsageStore()
  const response = await testHandler({
    usageStore,
    generateNote: () => Promise.resolve(geminiEnvelope(
      validMetadata,
      validUsage,
    )),
  })(requestFor())

  assertEquals(response.status, 200, "request failed")
  assertEquals(usageStore.finalizations.length, 1, "request was not finalized")
  assertEquals(
    usageStore.finalizations[0].usageMetadata?.totalTokenCount,
    validUsage.totalTokenCount,
    "token usage was not finalized",
  )
  const body = await responseBody(response)
  assertEquals(Object.keys(body).length, 6, "token usage reached the client")
})

Deno.test("retries only finalization once without calling Gemini again", async () => {
  const usageStore = new FakeUsageStore()
  usageStore.finalizeFailuresRemaining = 1
  let geminiCalls = 0
  const response = await testHandler({
    usageStore,
    generateNote: () => {
      geminiCalls++
      return Promise.resolve(geminiEnvelope(validMetadata, validUsage))
    },
  })(requestFor())

  assertEquals(response.status, 200, "successful retry changed the response")
  assertEquals(geminiCalls, 1, "Gemini was retried")
  assertEquals(usageStore.finalizeAttempts, 2, "finalization was not retried once")
})

Deno.test("stops after two failed finalizations without retrying Gemini", async () => {
  const usageStore = new FakeUsageStore()
  usageStore.finalizeFailuresRemaining = 2
  let geminiCalls = 0
  const response = await testHandler({
    usageStore,
    generateNote: () => {
      geminiCalls++
      return Promise.resolve(geminiEnvelope(validMetadata, validUsage))
    },
  })(requestFor())

  assertEquals(response.status, 503, "persistent finalization failure was hidden")
  assertEquals(geminiCalls, 1, "Gemini was retried after finalization failure")
  assertEquals(usageStore.finalizeAttempts, 2, "finalization retried more than once")
})

Deno.test("rejects malformed or incomplete bodies", async () => {
  const handler = successHandler()
  const malformed = await handler(requestFor(undefined, { rawBody: "{" }))
  const incomplete = await handler(requestFor({ text: "Nota" }))
  assertEquals(malformed.status, 400, "malformed JSON was accepted")
  assertEquals(incomplete.status, 400, "incomplete body was accepted")
})

Deno.test("protects legacy clients by assigning a server request id", async () => {
  const usageStore = new FakeUsageStore()
  const { requestId: _requestId, ...legacyRequest } = validRequest
  const response = await testHandler({
    usageStore,
    generateNote: () => Promise.resolve(geminiEnvelope(validMetadata)),
  })(requestFor(legacyRequest))

  assertEquals(response.status, 200, "legacy request was rejected")
  assert(
    /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/
      .test(usageStore.lastReservedRequestId),
    "legacy request did not receive a UUID",
  )
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
  assertEquals(MAX_TEXT_LENGTH, 12_000, "unexpected text limit")
  const response = await successHandler()(requestFor({
    ...validRequest,
    text: "x".repeat(12_001),
  }))
  assertEquals(response.status, 400, "oversized text was accepted")
  const body = await responseBody(response)
  assertEquals(
    (body.error as Record<string, unknown>).code,
    "TEXT_TOO_LONG",
    "oversized text returned a generic error",
  )
})

Deno.test("accepts text at the 12000 character limit", async () => {
  const response = await successHandler()(requestFor({
    ...validRequest,
    text: "x".repeat(12_000),
  }))
  assertEquals(MAX_TEXT_LENGTH, 12_000, "unexpected text limit")
  assertEquals(response.status, 200, "text at the limit was rejected")
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
  const handler = testHandler({
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
  const handler = testHandler({
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
  const handler = testHandler({
    generateNote: () => {
      throw new UpstreamHttpError(429, 30)
    },
  })
  const response = await handler(requestFor())
  assertEquals(response.status, 429, "unexpected rate-limit status")
  assertEquals(response.headers.get("retry-after"), "30", "missing retry timing")
})

Deno.test("rejects invalid AI responses", async () => {
  const handler = testHandler({
    generateNote: () => Promise.resolve(generatedResponse({ candidates: [] })),
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
  const handler = testHandler({
    generateNote: () => Promise.resolve(geminiEnvelope(
      validMetadata,
      validUsage,
    )),
  })
  const response = await handler(requestFor())
  const body = await responseBody(response)
  assertEquals(response.status, 200, "valid response was rejected")
  assertEquals(JSON.stringify(body), JSON.stringify(validMetadata), "response changed")
  assertEquals(Object.keys(body).length, 6, "response contains extra fields")
})

Deno.test("does not expose sensitive upstream values", async () => {
  const marker = "SHOULD_NOT_REACH_THE_CLIENT"
  const handler = testHandler({
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
    source.includes('readEnvironment("GEMINI_API_KEY_V2")') &&
      source.includes("requireGeminiCredential()"),
    "server-side credential lookup is missing",
  )
  assert(source.includes('"x-goog-api-key": credential'), "credential header is missing")
  assert(!source.includes("?key="), "credential is present in the request URL")
  assert(!source.includes("console."), "entrypoint contains logging")
})
