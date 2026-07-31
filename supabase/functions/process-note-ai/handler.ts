export const MAX_BODY_BYTES = 65_536
export const MAX_TEXT_LENGTH = 12_000
export const MAX_CATEGORY_COUNT = 50
export const MAX_CATEGORY_LENGTH = 64

const DEFAULT_TIMEOUT_MS = 25_000
const FINALIZATION_RETRY_DELAY_MS = 50
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
  requestId: string
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

export interface AiUsageMetadata {
  promptTokenCount: number | null
  candidatesTokenCount: number | null
  thoughtsTokenCount: number | null
  cachedContentTokenCount: number | null
  totalTokenCount: number | null
}

export interface GeneratedNote {
  response: unknown
  usageMetadata: AiUsageMetadata | null
}

export type GenerateNote = (
  request: ProcessNoteRequest,
  signal: AbortSignal,
) => Promise<GeneratedNote>

export type AuthenticateUser = (request: Request) => Promise<string | null>

export type ReservationDecision =
  | {
    decision: "RESERVED"
  }
  | {
    decision: "REPLAY_SUCCESS"
    resultPayload: NoteMetadata
  }
  | {
    decision: "REPLAY_FAILURE"
    responseStatus: number
    errorCode: string
    retryAfterSeconds?: number
  }
  | {
    decision:
      | "MONTHLY_QUOTA_EXHAUSTED"
      | "DAILY_QUOTA_EXHAUSTED"
      | "RATE_LIMITED"
      | "CONCURRENT_REQUEST"
      | "REQUEST_IN_PROGRESS"
    retryAfterSeconds?: number
  }
  | {
    decision: "IDEMPOTENCY_CONFLICT"
  }

export interface FinalizeAiRequest {
  userId: string
  requestId: string
  status:
    | "succeeded"
    | "upstream_timeout"
    | "upstream_rate_limited"
    | "upstream_error"
    | "invalid_ai_response"
    | "service_unavailable"
  responseStatus: number
  errorCode: string | null
  retryAfterSeconds: number | null
  resultPayload: NoteMetadata | null
  usageMetadata: AiUsageMetadata | null
}

export interface AiUsageStore {
  reserve(
    userId: string,
    requestId: string,
    requestFingerprint: string,
    inputCharacterCount: number,
  ): Promise<ReservationDecision>
  finalize(request: FinalizeAiRequest): Promise<void>
}

export interface HandlerDependencies {
  authenticateUser: AuthenticateUser
  validateAiConfiguration: () => void
  usageStore: AiUsageStore
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
class TextTooLongError extends Error {}
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

type RequestIdDisposition = "RETRY_SAME" | "NEW_REQUEST"

function errorResponse(
  status: number,
  code: string,
  retryAfterSeconds?: number,
  additionalHeaders: Record<string, string> = {},
  requestIdDisposition?: RequestIdDisposition,
): Response {
  const retry = retryAfterSeconds !== undefined && retryAfterSeconds > 0
    ? Math.ceil(retryAfterSeconds)
    : undefined
  return jsonResponse(
    {
      error: {
        code,
        ...(retry === undefined ? {} : { retryAfterSeconds: retry }),
        ...(requestIdDisposition === undefined ? {} : { requestIdDisposition }),
      },
    },
    status,
    {
      ...(retry === undefined ? {} : { "Retry-After": String(retry) }),
      ...additionalHeaders,
    },
  )
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

  const allowedFields = new Set([
    "requestId",
    "text",
    "categoryNames",
    "currentDate",
  ])
  const fields = Object.keys(value)
  if (
    fields.length < allowedFields.size - 1 ||
    fields.length > allowedFields.size ||
    fields.some((field) => !allowedFields.has(field))
  ) {
    throw new InvalidRequestError()
  }

  const requestId = value.requestId === undefined
    ? crypto.randomUUID()
    : value.requestId
  if (
    typeof requestId !== "string" ||
    !/^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i
      .test(requestId)
  ) {
    throw new InvalidRequestError()
  }

  if (
    typeof value.text !== "string" ||
    value.text.trim().length === 0
  ) {
    throw new InvalidRequestError()
  }
  if (value.text.length > MAX_TEXT_LENGTH) throw new TextTooLongError()

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
    requestId: requestId.toLowerCase(),
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

async function requestFingerprint(
  request: ProcessNoteRequest,
): Promise<string> {
  const canonicalPayload = JSON.stringify({
    text: request.text,
    categoryNames: request.categoryNames,
    currentDate: request.currentDate,
  })
  const digest = await crypto.subtle.digest(
    "SHA-256",
    new TextEncoder().encode(canonicalPayload),
  )
  return Array.from(new Uint8Array(digest))
    .map((byte) => byte.toString(16).padStart(2, "0"))
    .join("")
}

function reservationResponse(
  reservation: Exclude<ReservationDecision, { decision: "RESERVED" }>,
  allowedCategories: string[],
): Response {
  switch (reservation.decision) {
    case "REPLAY_SUCCESS": {
      const validatedPayload = validateAiResponse({
        candidates: [{
          content: {
            parts: [{ text: JSON.stringify(reservation.resultPayload) }],
          },
        }],
      }, allowedCategories)
      return jsonResponse(validatedPayload, 200)
    }
    case "REPLAY_FAILURE":
      return errorResponse(
        reservation.responseStatus,
        reservation.errorCode,
        reservation.retryAfterSeconds,
        {},
        "NEW_REQUEST",
      )
    case "MONTHLY_QUOTA_EXHAUSTED":
    case "DAILY_QUOTA_EXHAUSTED":
    case "RATE_LIMITED":
      return errorResponse(
        429,
        reservation.decision,
        reservation.retryAfterSeconds,
      )
    case "CONCURRENT_REQUEST":
    case "REQUEST_IN_PROGRESS":
      return errorResponse(
        409,
        reservation.decision,
        reservation.retryAfterSeconds,
      )
    case "IDEMPOTENCY_CONFLICT":
      return errorResponse(409, reservation.decision)
  }
}

async function finalizeWithOneRetry(
  usageStore: AiUsageStore,
  request: FinalizeAiRequest,
): Promise<void> {
  try {
    await usageStore.finalize(request)
  } catch {
    await new Promise((resolve) =>
      globalThis.setTimeout(resolve, FINALIZATION_RETRY_DELAY_MS)
    )
    await usageStore.finalize(request)
  }
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
      return errorResponse(405, "METHOD_NOT_ALLOWED", undefined, {
        Allow: "POST, OPTIONS",
      })
    }

    let userId: string | null
    try {
      userId = await dependencies.authenticateUser(request)
    } catch {
      return errorResponse(503, "SERVICE_UNAVAILABLE", undefined, {}, "RETRY_SAME")
    }
    if (userId === null) {
      return errorResponse(401, "AUTHENTICATION_INVALID")
    }

    let validatedRequest: ProcessNoteRequest
    try {
      const rawBody = await readBodyWithLimit(request, MAX_BODY_BYTES)
      validatedRequest = validateRequestBody(JSON.parse(rawBody))
    } catch (error) {
      if (error instanceof PayloadTooLargeError) {
        return errorResponse(413, "PAYLOAD_TOO_LARGE")
      }
      if (error instanceof TextTooLongError) {
        return errorResponse(400, "TEXT_TOO_LONG")
      }
      return errorResponse(400, "INVALID_REQUEST")
    }

    try {
      dependencies.validateAiConfiguration()
    } catch {
      return errorResponse(503, "SERVICE_UNAVAILABLE", undefined, {}, "RETRY_SAME")
    }

    let reservation: ReservationDecision
    try {
      reservation = await dependencies.usageStore.reserve(
        userId,
        validatedRequest.requestId,
        await requestFingerprint(validatedRequest),
        validatedRequest.text.length,
      )
    } catch {
      return errorResponse(503, "SERVICE_UNAVAILABLE", undefined, {}, "RETRY_SAME")
    }
    if (reservation.decision !== "RESERVED") {
      try {
        return reservationResponse(
          reservation,
          validatedRequest.categoryNames,
        )
      } catch {
        const disposition = reservation.decision === "REPLAY_SUCCESS" ||
            reservation.decision === "REPLAY_FAILURE"
          ? "NEW_REQUEST"
          : "RETRY_SAME"
        return errorResponse(503, "SERVICE_UNAVAILABLE", undefined, {}, disposition)
      }
    }

    const controller = new AbortController()
    let timeoutIdentifier: ReturnType<typeof globalThis.setTimeout> | undefined
    const timeout = new Promise<never>((_resolve, reject) => {
      timeoutIdentifier = globalThis.setTimeout(() => {
        controller.abort()
        reject(new RequestTimeoutError())
      }, timeoutMs)
    })

    let response: Response
    let finalization: Omit<FinalizeAiRequest, "userId" | "requestId">
    let usageMetadata: AiUsageMetadata | null = null
    try {
      const generatedNote = await Promise.race([
        dependencies.generateNote(validatedRequest, controller.signal),
        timeout,
      ])
      usageMetadata = generatedNote.usageMetadata
      const metadata = validateAiResponse(
        generatedNote.response,
        validatedRequest.categoryNames,
      )
      response = jsonResponse(metadata, 200)
      finalization = {
        status: "succeeded",
        responseStatus: 200,
        errorCode: null,
        retryAfterSeconds: null,
        resultPayload: metadata,
        usageMetadata,
      }
    } catch (error) {
      if (error instanceof RequestTimeoutError || controller.signal.aborted) {
        response = errorResponse(
          504,
          "UPSTREAM_TIMEOUT",
          undefined,
          {},
          "NEW_REQUEST",
        )
        finalization = {
          status: "upstream_timeout",
          responseStatus: 504,
          errorCode: "UPSTREAM_TIMEOUT",
          retryAfterSeconds: null,
          resultPayload: null,
          usageMetadata,
        }
      } else if (error instanceof ServiceConfigurationError) {
        response = errorResponse(
          503,
          "SERVICE_UNAVAILABLE",
          undefined,
          {},
          "NEW_REQUEST",
        )
        finalization = {
          status: "service_unavailable",
          responseStatus: 503,
          errorCode: "SERVICE_UNAVAILABLE",
          retryAfterSeconds: null,
          resultPayload: null,
          usageMetadata,
        }
      } else if (error instanceof UpstreamHttpError && error.status === 429) {
        response = errorResponse(
          429,
          "UPSTREAM_RATE_LIMITED",
          error.retryAfterSeconds,
          {},
          "NEW_REQUEST",
        )
        finalization = {
          status: "upstream_rate_limited",
          responseStatus: 429,
          errorCode: "UPSTREAM_RATE_LIMITED",
          retryAfterSeconds: error.retryAfterSeconds ?? null,
          resultPayload: null,
          usageMetadata,
        }
      } else if (error instanceof InvalidAiResponseError) {
        response = errorResponse(
          502,
          "INVALID_AI_RESPONSE",
          undefined,
          {},
          "NEW_REQUEST",
        )
        finalization = {
          status: "invalid_ai_response",
          responseStatus: 502,
          errorCode: "INVALID_AI_RESPONSE",
          retryAfterSeconds: null,
          resultPayload: null,
          usageMetadata,
        }
      } else {
        response = errorResponse(
          502,
          "UPSTREAM_ERROR",
          undefined,
          {},
          "NEW_REQUEST",
        )
        finalization = {
          status: "upstream_error",
          responseStatus: 502,
          errorCode: "UPSTREAM_ERROR",
          retryAfterSeconds: null,
          resultPayload: null,
          usageMetadata,
        }
      }
    } finally {
      if (timeoutIdentifier !== undefined) {
        globalThis.clearTimeout(timeoutIdentifier)
      }
    }

    try {
      await finalizeWithOneRetry(dependencies.usageStore, {
        userId,
        requestId: validatedRequest.requestId,
        ...finalization,
      })
    } catch {
      return errorResponse(503, "SERVICE_UNAVAILABLE", undefined, {}, "RETRY_SAME")
    }
    return response
  }
}
