import {
  createSupabaseAuthVerifier,
  createSupabaseUsageStore,
} from "./supabase.ts"

function assert(condition: unknown, message: string): asserts condition {
  if (!condition) throw new Error(message)
}

function assertEquals<T>(actual: T, expected: T, message: string): void {
  assert(
    Object.is(actual, expected),
    `${message}: expected ${String(expected)}, received ${String(actual)}`,
  )
}

function abortablePendingFetch(
  _input: string | URL | Request,
  init?: RequestInit,
): Promise<Response> {
  return new Promise((_resolve, reject) => {
    init?.signal?.addEventListener(
      "abort",
      () => reject(new DOMException("aborted", "AbortError")),
      { once: true },
    )
  })
}

function stalledBodyFetch(): Promise<Response> {
  return Promise.resolve(new Response(
    new ReadableStream<Uint8Array>({ start: () => undefined }),
    { status: 200, headers: { "Content-Type": "application/json" } },
  ))
}

Deno.test("auth verifier derives the user id from Supabase Auth", async () => {
  let capturedUrl = ""
  let capturedAuthorization = ""
  const verifier = createSupabaseAuthVerifier({
    url: "https://project.supabase.co",
    anonKey: "public-anon-key",
    serviceRoleKey: "server-only-key",
    fetchImpl: (input, init) => {
      capturedUrl = String(input)
      capturedAuthorization = new Headers(init?.headers).get("authorization") ?? ""
      return Promise.resolve(Response.json({
        id: "AAAAAAAA-AAAA-4AAA-8AAA-AAAAAAAAAAAA",
      }))
    },
  })

  const userId = await verifier(new Request("https://example.invalid", {
    headers: { Authorization: "Bearer verified.jwt.value" },
  }))

  assertEquals(
    userId,
    "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa",
    "verified user id was not normalized",
  )
  assertEquals(
    capturedUrl,
    "https://project.supabase.co/auth/v1/user",
    "request did not use Supabase Auth",
  )
  assertEquals(
    capturedAuthorization,
    "Bearer verified.jwt.value",
    "bearer token was not forwarded for verification",
  )
})

Deno.test("auth verifier rejects malformed credentials without a network call", async () => {
  let invoked = false
  const verifier = createSupabaseAuthVerifier({
    url: "https://project.supabase.co",
    anonKey: "public-anon-key",
    serviceRoleKey: "server-only-key",
    fetchImpl: () => {
      invoked = true
      return Promise.resolve(Response.json({ id: crypto.randomUUID() }))
    },
  })

  const userId = await verifier(new Request("https://example.invalid", {
    headers: { Authorization: "not-a-bearer-token" },
  }))

  assertEquals(userId, null, "malformed credentials were accepted")
  assert(!invoked, "malformed credentials reached Supabase Auth")
})

Deno.test("auth verifier distinguishes invalid JWTs from Auth outages", async () => {
  for (const status of [401, 403]) {
    const verifier = createSupabaseAuthVerifier({
      url: "https://project.supabase.co",
      anonKey: "public-anon-key",
      serviceRoleKey: "server-only-key",
      fetchImpl: () => Promise.resolve(new Response(null, { status })),
    })
    const userId = await verifier(new Request("https://example.invalid", {
      headers: { Authorization: "Bearer rejected.jwt.value" },
    }))
    assertEquals(userId, null, `${status} did not reject the JWT`)
  }

  const unavailableVerifier = createSupabaseAuthVerifier({
    url: "https://project.supabase.co",
    anonKey: "public-anon-key",
    serviceRoleKey: "server-only-key",
    fetchImpl: () => Promise.resolve(new Response(null, { status: 503 })),
  })
  let failedClosed = false
  try {
    await unavailableVerifier(new Request("https://example.invalid", {
      headers: { Authorization: "Bearer valid.jwt.value" },
    }))
  } catch {
    failedClosed = true
  }
  assert(failedClosed, "Auth outage was mislabeled as an invalid JWT")
})

Deno.test("Supabase Auth and RPC calls have bounded timeouts", async () => {
  const configuration = {
    url: "https://project.supabase.co",
    anonKey: "public-anon-key",
    serviceRoleKey: "server-only-key",
    requestTimeoutMs: 5,
    fetchImpl: abortablePendingFetch,
  }
  const verifier = createSupabaseAuthVerifier(configuration)
  const store = createSupabaseUsageStore(configuration)
  const request = new Request("https://example.invalid", {
    headers: { Authorization: "Bearer valid.jwt.value" },
  })

  let authTimedOut = false
  try {
    await verifier(request)
  } catch {
    authTimedOut = true
  }
  assert(authTimedOut, "Supabase Auth call did not time out")

  let rpcTimedOut = false
  try {
    await store.reserve(
      "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa",
      "11111111-1111-4111-8111-111111111111",
      "a".repeat(64),
      100,
    )
  } catch {
    rpcTimedOut = true
  }
  assert(rpcTimedOut, "Supabase RPC call did not time out")
})

Deno.test("Supabase timeout includes stalled response bodies", async () => {
  const configuration = {
    url: "https://project.supabase.co",
    anonKey: "public-anon-key",
    serviceRoleKey: "server-only-key",
    requestTimeoutMs: 5,
    fetchImpl: stalledBodyFetch,
  }

  let authBodyTimedOut = false
  try {
    await createSupabaseAuthVerifier(configuration)(new Request(
      "https://example.invalid",
      { headers: { Authorization: "Bearer valid.jwt.value" } },
    ))
  } catch {
    authBodyTimedOut = true
  }
  assert(authBodyTimedOut, "Supabase Auth body did not time out")

  let rpcBodyTimedOut = false
  try {
    await createSupabaseUsageStore(configuration).reserve(
      "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa",
      "11111111-1111-4111-8111-111111111111",
      "a".repeat(64),
      100,
    )
  } catch {
    rpcBodyTimedOut = true
  }
  assert(rpcBodyTimedOut, "Supabase RPC body did not time out")
})

Deno.test("usage RPCs authenticate only with the service role", async () => {
  const calls: { url: string; headers: Headers; body: Record<string, unknown> }[] = []
  const store = createSupabaseUsageStore({
    url: "https://project.supabase.co",
    anonKey: "public-anon-key",
    serviceRoleKey: "server-only-key",
    fetchImpl: async (input, init) => {
      calls.push({
        url: String(input),
        headers: new Headers(init?.headers),
        body: JSON.parse(String(init?.body)) as Record<string, unknown>,
      })
      return calls.length === 1
        ? Response.json({ decision: "RESERVED" })
        : new Response(null, { status: 204 })
    },
  })

  const decision = await store.reserve(
    "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa",
    "11111111-1111-4111-8111-111111111111",
    "a".repeat(64),
    100,
  )
  await store.finalize({
    userId: "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa",
    requestId: "11111111-1111-4111-8111-111111111111",
    status: "succeeded",
    responseStatus: 200,
    errorCode: null,
    retryAfterSeconds: null,
    resultPayload: {
      title: "Nota",
      improvedText: "Testo",
      date: null,
      time: null,
      location: null,
      category: null,
    },
    usageMetadata: {
      promptTokenCount: 10,
      candidatesTokenCount: 20,
      thoughtsTokenCount: 0,
      cachedContentTokenCount: 0,
      totalTokenCount: 30,
    },
  })

  assertEquals(decision.decision, "RESERVED", "reservation response was not parsed")
  assertEquals(calls.length, 2, "unexpected RPC count")
  for (const call of calls) {
    assertEquals(
      call.headers.get("authorization"),
      "Bearer server-only-key",
      "RPC did not use the service role",
    )
    assert(!JSON.stringify(call.body).includes("server-only-key"), "key leaked into body")
  }
  assertEquals(
    calls[1].body.p_total_token_count,
    30,
    "token usage was not sent for finalization",
  )
})
