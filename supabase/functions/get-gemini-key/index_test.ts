import { handler } from "./index.ts"

function assert(condition: unknown, message: string): asserts condition {
  if (!condition) {
    throw new Error(message)
  }
}

function assertEquals<T>(actual: T, expected: T, message: string): void {
  assert(
    Object.is(actual, expected),
    `${message}: expected ${String(expected)}, received ${String(actual)}`,
  )
}

async function retiredResponse(): Promise<{
  response: Response
  text: string
  body: Record<string, unknown>
}> {
  const response = handler(
    new Request("https://example.invalid/functions/v1/retired", {
      method: "POST",
      headers: { Authorization: "Bearer placeholder" },
    }),
  )
  const text = await response.text()
  return {
    response,
    text,
    body: JSON.parse(text) as Record<string, unknown>,
  }
}

Deno.test("returns a controlled Gone response", async () => {
  const { response, body } = await retiredResponse()

  assertEquals(response.status, 410, "unexpected status")
  assertEquals(
    response.headers.get("content-type"),
    "application/json; charset=utf-8",
    "unexpected content type",
  )
  assertEquals(
    body.code,
    "ENDPOINT_DECOMMISSIONED",
    "unexpected response code",
  )
})

Deno.test("returns only the generic response field", async () => {
  const { text, body } = await retiredResponse()
  const fieldNames = Object.keys(body)
  const forbiddenFieldNames = [
    ["k", "e", "y"].join(""),
    ["a", "p", "i", "K", "e", "y"].join(""),
    ["s", "e", "c", "r", "e", "t"].join(""),
    ["t", "o", "k", "e", "n"].join(""),
  ].map((name) => name.toLowerCase())

  assertEquals(fieldNames.length, 1, "unexpected response field count")
  assertEquals(fieldNames[0], "code", "unexpected response field")
  assert(
    fieldNames.every((name) => !forbiddenFieldNames.includes(name.toLowerCase())),
    "response contains a forbidden field",
  )
  assertEquals(
    text,
    '{"code":"ENDPOINT_DECOMMISSIONED"}',
    "response is not the expected generic JSON",
  )
})

Deno.test("function source does not access sensitive configuration", async () => {
  const source = await Deno.readTextFile(new URL("./index.ts", import.meta.url))
  const forbiddenSourceFragments = [
    ["Deno", "env"].join("."),
    ["GEMINI", "API", "KEY"].join("_"),
    ["GEMINI", "API", "KEY", "V2"].join("_"),
  ]

  for (const fragment of forbiddenSourceFragments) {
    assert(!source.includes(fragment), "function accesses sensitive configuration")
  }
})

Deno.test("function remains protected by gateway JWT verification", async () => {
  const config = await Deno.readTextFile(
    new URL("../../config.toml", import.meta.url),
  )
  const section = config.match(
    /\[functions\.get-gemini-key\]([\s\S]*?)(?=\n\[|$)/,
  )?.[1]

  assert(section !== undefined, "function configuration is missing")
  assert(
    /^\s*verify_jwt\s*=\s*true\s*$/m.test(section),
    "gateway JWT verification is not enabled",
  )
})

Deno.test("preflight response contains no body", async () => {
  const response = handler(
    new Request("https://example.invalid/functions/v1/retired", {
      method: "OPTIONS",
    }),
  )

  assertEquals(response.status, 204, "unexpected preflight status")
  assertEquals(await response.text(), "", "preflight response must be empty")
})
