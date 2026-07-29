import {
  type AiUsageStore,
  type AuthenticateUser,
  type FinalizeAiRequest,
  type NoteMetadata,
  type ReservationDecision,
  ServiceConfigurationError,
} from "./handler.ts"

type Fetch = typeof fetch
const DEFAULT_REQUEST_TIMEOUT_MS = 5_000

interface SupabaseServerConfiguration {
  url: string
  anonKey: string
  serviceRoleKey: string
  requestTimeoutMs?: number
  fetchImpl?: Fetch
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value)
}

function positiveInteger(value: unknown): number | undefined {
  return Number.isInteger(value) && (value as number) > 0
    ? value as number
    : undefined
}

function reservationDecision(value: unknown): ReservationDecision {
  if (!isRecord(value) || typeof value.decision !== "string") {
    throw new ServiceConfigurationError()
  }

  const retryAfterSeconds = positiveInteger(value.retryAfterSeconds)
  switch (value.decision) {
    case "RESERVED":
    case "IDEMPOTENCY_CONFLICT":
      return { decision: value.decision }
    case "MONTHLY_QUOTA_EXHAUSTED":
    case "DAILY_QUOTA_EXHAUSTED":
    case "RATE_LIMITED":
    case "CONCURRENT_REQUEST":
    case "REQUEST_IN_PROGRESS":
      return { decision: value.decision, retryAfterSeconds }
    case "REPLAY_SUCCESS":
      if (!isRecord(value.resultPayload)) {
        throw new ServiceConfigurationError()
      }
      return {
        decision: value.decision,
        resultPayload: value.resultPayload as unknown as NoteMetadata,
      }
    case "REPLAY_FAILURE":
      if (
        !Number.isInteger(value.responseStatus) ||
        typeof value.errorCode !== "string"
      ) {
        throw new ServiceConfigurationError()
      }
      return {
        decision: value.decision,
        responseStatus: value.responseStatus as number,
        errorCode: value.errorCode,
        retryAfterSeconds,
      }
    default:
      throw new ServiceConfigurationError()
  }
}

async function withRequestTimeout<T>(
  configuration: SupabaseServerConfiguration,
  operation: (signal: AbortSignal) => Promise<T>,
): Promise<T> {
  const timeoutMs = configuration.requestTimeoutMs ??
    DEFAULT_REQUEST_TIMEOUT_MS
  if (!Number.isFinite(timeoutMs) || timeoutMs <= 0) {
    throw new ServiceConfigurationError()
  }

  const controller = new AbortController()
  let timeoutIdentifier: ReturnType<typeof globalThis.setTimeout> | undefined
  const timeout = new Promise<never>((_resolve, reject) => {
    timeoutIdentifier = globalThis.setTimeout(() => {
      controller.abort()
      reject(new ServiceConfigurationError())
    }, timeoutMs)
  })
  try {
    return await Promise.race([
      operation(controller.signal),
      timeout,
    ])
  } catch {
    throw new ServiceConfigurationError()
  } finally {
    if (timeoutIdentifier !== undefined) {
      globalThis.clearTimeout(timeoutIdentifier)
    }
  }
}

async function rpc(
  configuration: SupabaseServerConfiguration,
  name: string,
  body: Record<string, unknown>,
): Promise<unknown> {
  return await withRequestTimeout(configuration, async (signal) => {
    const response = await (configuration.fetchImpl ?? fetch)(
      `${configuration.url}/rest/v1/rpc/${name}`,
      {
        method: "POST",
        signal,
        headers: {
          "Content-Type": "application/json",
          apikey: configuration.serviceRoleKey,
          Authorization: `Bearer ${configuration.serviceRoleKey}`,
        },
        body: JSON.stringify(body),
      },
    )
    if (!response.ok) throw new ServiceConfigurationError()
    if (response.status === 204) return null
    return await response.json()
  })
}

export function createSupabaseAuthVerifier(
  configuration: SupabaseServerConfiguration,
): AuthenticateUser {
  return async (request: Request): Promise<string | null> => {
    const authorization = request.headers.get("authorization")
    if (
      authorization === null ||
      !/^Bearer\s+[A-Za-z0-9._~-]+$/i.test(authorization)
    ) {
      return null
    }

    const body = await withRequestTimeout(configuration, async (signal) => {
      const response = await (configuration.fetchImpl ?? fetch)(
        `${configuration.url}/auth/v1/user`,
        {
          method: "GET",
          signal,
          headers: {
            apikey: configuration.anonKey,
            Authorization: authorization,
          },
        },
      )
      if (response.status === 401 || response.status === 403) return null
      if (!response.ok) throw new ServiceConfigurationError()
      return await response.json() as unknown
    })
    if (body === null) return null
    if (
      !isRecord(body) ||
      typeof body.id !== "string" ||
      !/^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i
        .test(body.id)
    ) {
      throw new ServiceConfigurationError()
    }
    return body.id.toLowerCase()
  }
}

export function createSupabaseUsageStore(
  configuration: SupabaseServerConfiguration,
): AiUsageStore {
  return {
    async reserve(
      userId,
      requestId,
      requestFingerprint,
      inputCharacterCount,
    ) {
      return reservationDecision(await rpc(
        configuration,
        "reserve_ai_request",
        {
          p_user_id: userId,
          p_request_id: requestId,
          p_request_fingerprint: requestFingerprint,
          p_input_character_count: inputCharacterCount,
        },
      ))
    },

    async finalize(request: FinalizeAiRequest): Promise<void> {
      const usage = request.usageMetadata
      await rpc(configuration, "finalize_ai_request", {
        p_user_id: request.userId,
        p_request_id: request.requestId,
        p_status: request.status,
        p_response_status: request.responseStatus,
        p_error_code: request.errorCode,
        p_retry_after_seconds: request.retryAfterSeconds,
        p_result_payload: request.resultPayload,
        p_prompt_token_count: usage?.promptTokenCount ?? null,
        p_candidates_token_count: usage?.candidatesTokenCount ?? null,
        p_thoughts_token_count: usage?.thoughtsTokenCount ?? null,
        p_cached_content_token_count: usage?.cachedContentTokenCount ?? null,
        p_total_token_count: usage?.totalTokenCount ?? null,
      })
    },
  }
}
