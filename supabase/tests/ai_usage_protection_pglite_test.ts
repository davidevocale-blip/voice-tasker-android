import { PGlite } from "npm:@electric-sql/pglite@0.3.14"

function assertEquals<T>(actual: T, expected: T, message: string): void {
  if (!Object.is(actual, expected)) {
    throw new Error(
      `${message}: expected ${String(expected)}, received ${String(actual)}`,
    )
  }
}

async function reserve(
  database: PGlite,
  userId: string,
  requestId: string,
  fingerprint = "a".repeat(64),
): Promise<Record<string, unknown>> {
  const result = await database.query<{ decision: Record<string, unknown> }>(
    `select public.reserve_ai_request($1, $2, $3, 100) as decision`,
    [userId, requestId, fingerprint],
  )
  return result.rows[0].decision
}

async function finalize(
  database: PGlite,
  userId: string,
  requestId: string,
): Promise<void> {
  await database.query(
    `select public.finalize_ai_request(
      $1, $2, 'succeeded', 200, null, null, '{"title":"Nota"}'::jsonb,
      10, 20, 0, 0, 30
    )`,
    [userId, requestId],
  )
}

Deno.test("migration enforces atomic quotas, locks, and idempotency", async () => {
  const database = new PGlite()
  try {
    await database.exec(`
      create role anon;
      create role authenticated;
      create role service_role;
    `)
    const migration = await Deno.readTextFile(
      new URL("../migrations/20260729090000_ai_usage_protection.sql", import.meta.url),
    )
    await database.exec(migration)

    const idempotentUser = "00000000-0000-4000-8000-000000000001"
    const idempotentRequest = "10000000-0000-4000-8000-000000000001"
    assertEquals(
      (await reserve(database, idempotentUser, idempotentRequest)).decision,
      "RESERVED",
      "first request was not reserved",
    )
    assertEquals(
      (await reserve(database, idempotentUser, idempotentRequest)).decision,
      "REQUEST_IN_PROGRESS",
      "active retry was not recognized",
    )
    assertEquals(
      (await reserve(
        database,
        idempotentUser,
        idempotentRequest,
        "b".repeat(64),
      )).decision,
      "IDEMPOTENCY_CONFLICT",
      "changed payload reused an idempotency key",
    )
    await finalize(database, idempotentUser, idempotentRequest)
    assertEquals(
      (await reserve(database, idempotentUser, idempotentRequest)).decision,
      "REPLAY_SUCCESS",
      "completed request was not replayed",
    )

    const concurrentUser = "00000000-0000-4000-8000-000000000002"
    const firstConcurrent = "20000000-0000-4000-8000-000000000001"
    await reserve(database, concurrentUser, firstConcurrent)
    assertEquals(
      (await reserve(
        database,
        concurrentUser,
        "20000000-0000-4000-8000-000000000002",
      )).decision,
      "CONCURRENT_REQUEST",
      "parallel request was not blocked",
    )
    await database.query(
      `update private.ai_usage_state
       set active_until = clock_timestamp() - interval '1 second'
       where user_id = $1`,
      [concurrentUser],
    )
    assertEquals(
      (await reserve(
        database,
        concurrentUser,
        "20000000-0000-4000-8000-000000000003",
      )).decision,
      "RESERVED",
      "expired lock was not reclaimed",
    )

    const rateUser = "00000000-0000-4000-8000-000000000003"
    for (let index = 1; index <= 3; index++) {
      const requestId =
        `30000000-0000-4000-8000-${index.toString().padStart(12, "0")}`
      assertEquals(
        (await reserve(database, rateUser, requestId)).decision,
        "RESERVED",
        `rate-limit request ${index} was rejected`,
      )
      await finalize(database, rateUser, requestId)
    }
    assertEquals(
      (await reserve(
        database,
        rateUser,
        "30000000-0000-4000-8000-000000000004",
      )).decision,
      "RATE_LIMITED",
      "fourth request in sixty seconds was accepted",
    )
    await database.query(
      `update private.ai_usage_request
       set reserved_at = clock_timestamp() - interval '60.001 seconds'
       where user_id = $1`,
      [rateUser],
    )
    assertEquals(
      (await reserve(
        database,
        rateUser,
        "30000000-0000-4000-8000-000000000005",
      )).decision,
      "RESERVED",
      "request beyond the sixty-second window was rejected",
    )

    const today = "(clock_timestamp() at time zone 'UTC')::date"
    const month = "date_trunc('month', clock_timestamp() at time zone 'UTC')::date"
    await database.exec(`
      insert into private.ai_usage_state
        (user_id, month_start, month_request_count, day_start, day_request_count)
      values
        ('00000000-0000-4000-8000-000000000004', ${month}, 199, ${today}, 0),
        ('00000000-0000-4000-8000-000000000005', ${month}, 0, ${today}, 9),
        ('00000000-0000-4000-8000-000000000006', date '2000-01-01', 200,
          date '2000-01-01', 10);
    `)
    assertEquals(
      (await reserve(
        database,
        "00000000-0000-4000-8000-000000000004",
        "40000000-0000-4000-8000-000000000001",
      )).decision,
      "RESERVED",
      "monthly request 200 was rejected",
    )
    await finalize(
      database,
      "00000000-0000-4000-8000-000000000004",
      "40000000-0000-4000-8000-000000000001",
    )
    assertEquals(
      (await reserve(
        database,
        "00000000-0000-4000-8000-000000000004",
        "40000000-0000-4000-8000-000000000002",
      )).decision,
      "MONTHLY_QUOTA_EXHAUSTED",
      "monthly request 201 was accepted",
    )
    assertEquals(
      (await reserve(
        database,
        "00000000-0000-4000-8000-000000000005",
        "50000000-0000-4000-8000-000000000001",
      )).decision,
      "RESERVED",
      "daily request 10 was rejected",
    )
    await finalize(
      database,
      "00000000-0000-4000-8000-000000000005",
      "50000000-0000-4000-8000-000000000001",
    )
    assertEquals(
      (await reserve(
        database,
        "00000000-0000-4000-8000-000000000005",
        "50000000-0000-4000-8000-000000000002",
      )).decision,
      "DAILY_QUOTA_EXHAUSTED",
      "daily request 11 was accepted",
    )
    assertEquals(
      (await reserve(
        database,
        "00000000-0000-4000-8000-000000000006",
        "60000000-0000-4000-8000-000000000001",
      )).decision,
      "RESERVED",
      "UTC period rollover did not reset counters",
    )

    const counters = await database.query<{
      month_request_count: number
      day_request_count: number
    }>(
      `select month_request_count, day_request_count
       from private.ai_usage_state
       where user_id = '00000000-0000-4000-8000-000000000006'`,
    )
    assertEquals(counters.rows[0].month_request_count, 1, "month did not reset")
    assertEquals(counters.rows[0].day_request_count, 1, "day did not reset")

    const tokens = await database.query<{ total_token_count: number }>(
      `select total_token_count
       from private.ai_usage_request
       where user_id = $1 and request_id = $2`,
      [idempotentUser, idempotentRequest],
    )
    assertEquals(tokens.rows[0].total_token_count, 30, "tokens were not stored")
  } finally {
    await database.close()
  }
})
