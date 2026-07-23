export const MAX_BODY_BYTES = 65_536
export const MAX_TEXT_LENGTH = 20_000
export const MAX_CATEGORY_COUNT = 50
export const MAX_CATEGORY_LENGTH = 64

const DEFAULT_TIMEOUT_MS = 25_000
const MAX_TITLE_LENGTH = 120
const MAX_LOCATION_LENGTH = 200

const corsHeaders: Record<string, string> = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers":
    "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
}

const jsonHeaders: Record<string, string> = {
  ...corsHeaders,
  "Content-Type": "application/json; charset=utf-8",
}

export interface ProcessNoteRequest {
  text: string
  categoryNames: string[]
  currentDate: string
}

export interface NoteMetadata {
  title: string
  improvedText: string
  date: string | null
  time: string | null
  location: string | null
  category: string | null
}

export type GenerateNote = (
  request: ProcessNoteRequest,
  signal: AbortSignal,
) => Promise<unknown>

export interface HandlerDependencies {
  generateNote: GenerateNote
  timeoutMs?: number
}

export class UpstreamHttpError extends Error {
  readonly status: number
  readonly retryAfterSeconds?: number

  constructor(
    status: number,
    retryAfterSeconds?: number,
  ) {
    super("Upstream request failed")
    this.name = "UpstreamHttpError"
    this.status = status
    this.retryAfterSeconds = retryAfterSeconds
  }
}

export class ServiceConfigurationError extends Error {
  constructor() {
    super("Service configuration unavailable")
    this.name = "ServiceConfigurationError"
  }
}

export class InvalidAiResponseError extends Error {
  constructor() {
    super("Invalid AI response")
    this.name = "InvalidAiResponseError"
  }
}

class InvalidRequestError extends Error {}
class PayloadTooLargeError extends Error {}
class RequestTimeoutError extends Error {}

function jsonResponse(
  body: unknown,
  status: number,
  additionalHeaders: Record<string, string> = {},
): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: {
      ...jsonHeaders,
      ...additionalHeaders,
    },
  })
}

function errorResponse(
  status: number,
  code: string,
  additionalHeaders: Record<string, string> = {},
): Response {
  return jsonResponse({ error: { code } }, status, additionalHeaders)
}

function hasBearerAuthorization(request: Request): boolean {
  const authorization = request.headers.get("authorization")
  return authorization !== null && /^Bearer\s+\S+$/i.test(authorization)
}

async function readBodyWithLimit(
  request: Request,
  maximumBytes: number,
): Promise<string> {
  const declaredLength = request.headers.get("content-length")
  if (declaredLength !== null) {
    const parsedLength = Number(declaredLength)
    if (Number.isFinite(parsedLength) && parsedLength > maximumBytes) {
      throw new PayloadTooLargeError()
    }
  }

  if (request.body === null) {
    return ""
  }

  const reader = request.body.getReader()
  const chunks: Uint8Array[] = []
  let totalBytes = 0

  while (true) {
    const { done, value } = await reader.read()
    if (done) break
    if (value === undefined) continue

    totalBytes += value.byteLength
    if (totalBytes > maximumBytes) {
      await reader.cancel()
      throw new PayloadTooLargeError()
    }
    chunks.push(value)
  }

  const body = new Uint8Array(totalBytes)
  let offset = 0
  for (const chunk of chunks) {
    body.set(chunk, offset)
    offset += chunk.byteLength
  }

  try {
    return new TextDecoder("utf-8", { fatal: true }).decode(body)
  } catch {
    throw new InvalidRequestError()
  }
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value)
}

function isValidDate(value: string): boolean {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value)
  if (match === null) return false

  const year = Number(match[1])
  const month = Number(match[2])
  const day = Number(match[3])
  const date = new Date(Date.UTC(year, month - 1, day))
  return date.getUTCFullYear() === year &&
    date.getUTCMonth() === month - 1 &&
    date.getUTCDate() === day
}

function validateRequestBody(value: unknown): ProcessNoteRequest {
  if (!isRecord(value)) throw new InvalidRequestError()

  const allowedFields = new Set(["text", "categoryNames", "currentDate"])
  const fields = Object.keys(value)
  if (
    fields.length !== allowedFields.size ||
    fields.some((field) => !allowedFields.has(field))
  ) {
    throw new InvalidRequestError()
  }

  if (
    typeof value.text !== "string" ||
    value.text.trim().length === 0 ||
    value.text.length > MAX_TEXT_LENGTH
  ) {
    throw new InvalidRequestError()
  }

  if (
    !Array.isArray(value.categoryNames) ||
    value.categoryNames.length > MAX_CATEGORY_COUNT
  ) {
    throw new InvalidRequestError()
  }

  const categoryNames: string[] = []
  const normalizedNames = new Set<string>()
  for (const category of value.categoryNames) {
    if (typeof category !== "string") throw new InvalidRequestError()
    const trimmed = category.trim()
    const normalized = trimmed.toLocaleLowerCase("it-IT")
    if (
      trimmed.length === 0 ||
      trimmed.length > MAX_CATEGORY_LENGTH ||
      normalizedNames.has(normalized)
    ) {
      throw new InvalidRequestError()
    }
    normalizedNames.add(normalized)
    categoryNames.push(trimmed)
  }

  if (typeof value.currentDate !== "string" || !isValidDate(value.currentDate)) {
    throw new InvalidRequestError()
  }

  return {
    text: value.text,
    categoryNames,
    currentDate: value.currentDate,
  }
}

function nullableString(
  value: unknown,
  maximumLength: number,
): string | null {
  if (value === null) return null
  if (typeof value !== "string") throw new InvalidAiResponseError()
  const trimmed = value.trim()
  if (trimmed.length === 0 || trimmed.length > maximumLength) {
    throw new InvalidAiResponseError()
  }
  return trimmed
}

function extractGeneratedText(value: unknown): string {
  if (!isRecord(value) || !Array.isArray(value.candidates)) {
    throw new InvalidAiResponseError()
  }
  const candidate = value.candidates[0]
  if (!isRecord(candidate) || !isRecord(candidate.content)) {
    throw new InvalidAiResponseError()
  }
  const parts = candidate.content.parts
  if (!Array.isArray(parts) || parts.length !== 1 || !isRecord(parts[0])) {
    throw new InvalidAiResponseError()
  }
  const text = parts[0].text
  if (typeof text !== "string" || text.length === 0) {
    throw new InvalidAiResponseError()
  }
  return text
}

function validateAiResponse(
  value: unknown,
  allowedCategories: string[],
): NoteMetadata {
  let parsed: unknown
  try {
    parsed = JSON.parse(extractGeneratedText(value))
  } catch (error) {
    if (error instanceof InvalidAiResponseError) throw error
    throw new InvalidAiResponseError()
  }

  if (!isRecord(parsed)) throw new InvalidAiResponseError()

  const expectedFields = [
    "title",
    "improvedText",
    "date",
    "time",
    "location",
    "category",
  ]
  const fields = Object.keys(parsed)
  if (
    fields.length !== expectedFields.length ||
    fields.some((field) => !expectedFields.includes(field))
  ) {
    throw new InvalidAiResponseError()
  }

  if (
    typeof parsed.title !== "string" ||
    parsed.title.trim().length === 0 ||
    parsed.title.trim().length > MAX_TITLE_LENGTH ||
    typeof parsed.improvedText !== "string" ||
    parsed.improvedText.trim().length === 0 ||
    parsed.improvedText.length > MAX_TEXT_LENGTH
  ) {
    throw new InvalidAiResponseError()
  }

  const date = nullableString(parsed.date, 10)
  if (date !== null && !isValidDate(date)) {
    throw new InvalidAiResponseError()
  }

  const time = nullableString(parsed.time, 5)
  if (time !== null && !/^([01]\d|2[0-3]):[0-5]\d$/.test(time)) {
    throw new InvalidAiResponseError()
  }

  const location = nullableString(parsed.location, MAX_LOCATION_LENGTH)
  const proposedCategory = nullableString(parsed.category, MAX_CATEGORY_LENGTH)
  let category: string | null = null
  if (proposedCategory !== null) {
    category = allowedCategories.find((allowed) =>
      allowed.localeCompare(proposedCategory, "it-IT", { sensitivity: "accent" }) === 0
    ) ?? null
    if (category === null) throw new InvalidAiResponseError()
  }

  return {
    title: parsed.title.trim(),
    improvedText: parsed.improvedText.trim(),
    date,
    time,
    location,
    category,
  }
}

function retryAfterHeader(error: UpstreamHttpError): Record<string, string> {
  const seconds = error.retryAfterSeconds
  if (
    seconds === undefined ||
    !Number.isInteger(seconds) ||
    seconds < 1 ||
    seconds > 3_600
  ) {
    return {}
  }
  return { "Retry-After": String(seconds) }
}

export function createHandler(
  dependencies: HandlerDependencies,
): (request: Request) => Promise<Response> {
  const timeoutMs = dependencies.timeoutMs ?? DEFAULT_TIMEOUT_MS
  if (!Number.isFinite(timeoutMs) || timeoutMs <= 0) {
    throw new TypeError("timeoutMs must be positive")
  }

  return async (request: Request): Promise<Response> => {
    if (request.method === "OPTIONS") {
      return new Response(null, { status: 204, headers: corsHeaders })
    }
    if (request.method !== "POST") {
      return errorResponse(405, "METHOD_NOT_ALLOWED", {
        Allow: "POST, OPTIONS",
      })
    }
    if (!hasBearerAuthorization(request)) {
      return errorResponse(401, "UNAUTHENTICATED")
    }

    let validatedRequest: ProcessNoteRequest
    try {
      const rawBody = await readBodyWithLimit(request, MAX_BODY_BYTES)
      validatedRequest = validateRequestBody(JSON.parse(rawBody))
    } catch (error) {
      if (error instanceof PayloadTooLargeError) {
        return errorResponse(413, "PAYLOAD_TOO_LARGE")
      }
      return errorResponse(400, "INVALID_REQUEST")
    }

    const controller = new AbortController()
    let timeoutIdentifier: ReturnType<typeof globalThis.setTimeout> | undefined
    const timeout = new Promise<never>((_resolve, reject) => {
      timeoutIdentifier = globalThis.setTimeout(() => {
        controller.abort()
        reject(new RequestTimeoutError())
      }, timeoutMs)
    })

    try {
      const upstreamResponse = await Promise.race([
        dependencies.generateNote(validatedRequest, controller.signal),
        timeout,
      ])
      const metadata = validateAiResponse(
        upstreamResponse,
        validatedRequest.categoryNames,
      )
      return jsonResponse(metadata, 200)
    } catch (error) {
      if (error instanceof RequestTimeoutError || controller.signal.aborted) {
        return errorResponse(504, "UPSTREAM_TIMEOUT")
      }
      if (error instanceof ServiceConfigurationError) {
        return errorResponse(503, "SERVICE_UNAVAILABLE")
      }
      if (error instanceof UpstreamHttpError) {
        if (error.status === 429) {
          return errorResponse(
            429,
            "UPSTREAM_RATE_LIMITED",
            retryAfterHeader(error),
          )
        }
        return errorResponse(502, "UPSTREAM_ERROR")
      }
      if (error instanceof InvalidAiResponseError) {
        return errorResponse(502, "INVALID_AI_RESPONSE")
      }
      return errorResponse(502, "UPSTREAM_ERROR")
    } finally {
      if (timeoutIdentifier !== undefined) {
        globalThis.clearTimeout(timeoutIdentifier)
      }
    }
  }
}
