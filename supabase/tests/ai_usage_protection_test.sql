begin;

create extension if not exists pgtap with schema extensions;

select plan(25);

select is(
  public.reserve_ai_request(
    '00000000-0000-0000-0000-000000000001',
    '10000000-0000-0000-0000-000000000001',
    repeat('a', 64),
    100
  )->>'decision',
  'RESERVED',
  'first request is reserved'
);

select is(
  public.reserve_ai_request(
    '00000000-0000-0000-0000-000000000001',
    '10000000-0000-0000-0000-000000000001',
    repeat('a', 64),
    100
  )->>'decision',
  'REQUEST_IN_PROGRESS',
  'same active request is not reserved twice'
);

select is(
  (
    select month_request_count
    from private.ai_usage_state
    where user_id = '00000000-0000-0000-0000-000000000001'
  ),
  1,
  'duplicate active request increments counters once'
);

select is(
  public.reserve_ai_request(
    '00000000-0000-0000-0000-000000000001',
    '10000000-0000-0000-0000-000000000001',
    repeat('b', 64),
    100
  )->>'decision',
  'IDEMPOTENCY_CONFLICT',
  'same request id with another payload is rejected'
);

select public.finalize_ai_request(
  '00000000-0000-0000-0000-000000000001',
  '10000000-0000-0000-0000-000000000001',
  'succeeded',
  199,
  null,
  null,
  '{"title":"Nota"}'::jsonb,
  10,
  20,
  0,
  0,
  30
);

select is(
  public.reserve_ai_request(
    '00000000-0000-0000-0000-000000000001',
    '10000000-0000-0000-0000-000000000001',
    repeat('a', 64),
    100
  )->>'decision',
  'REPLAY_SUCCESS',
  'completed request is replayed without a new reservation'
);

insert into private.ai_usage_state (
  user_id,
  month_start,
  month_request_count,
  day_start,
  day_request_count
)
values (
  '00000000-0000-0000-0000-000000000002',
  date_trunc('month', now() at time zone 'UTC')::date,
  200,
  (now() at time zone 'UTC')::date,
  1
);

select is(
  public.reserve_ai_request(
    '00000000-0000-0000-0000-000000000002',
    '20000000-0000-0000-0000-000000000001',
    repeat('c', 64),
    100
  )->>'decision',
  'RESERVED',
  'monthly request 200 is accepted'
);

select public.finalize_ai_request(
  '00000000-0000-0000-0000-000000000002',
  '20000000-0000-0000-0000-000000000001',
  'succeeded',
  200,
  null,
  null,
  '{}'::jsonb,
  1,
  1,
  0,
  0,
  2
);

select is(
  public.reserve_ai_request(
    '00000000-0000-0000-0000-000000000002',
    '20000000-0000-0000-0000-000000000002',
    repeat('d', 64),
    100
  )->>'decision',
  'MONTHLY_QUOTA_EXHAUSTED',
  'monthly request 201 is rejected'
);

select ok(
  (
    public.reserve_ai_request(
      '00000000-0000-0000-0000-000000000002',
      '20000000-0000-0000-0000-000000000003',
      repeat('d', 64),
      100
    )->>'retryAfterSeconds'
  )::integer > 0,
  'monthly quota includes retry timing'
);

insert into private.ai_usage_state (
  user_id,
  month_start,
  month_request_count,
  day_start,
  day_request_count
)
values (
  '00000000-0000-0000-0000-000000000003',
  date_trunc('month', now() at time zone 'UTC')::date,
  0,
  (now() at time zone 'UTC')::date,
  9
);

select is(
  public.reserve_ai_request(
    '00000000-0000-0000-0000-000000000003',
    '30000000-0000-0000-0000-000000000001',
    repeat('e', 64),
    100
  )->>'decision',
  'RESERVED',
  'daily request 10 is accepted'
);

select public.finalize_ai_request(
  '00000000-0000-0000-0000-000000000003',
  '30000000-0000-0000-0000-000000000001',
  'succeeded',
  200,
  null,
  null,
  '{}'::jsonb,
  1,
  1,
  0,
  0,
  2
);

select is(
  public.reserve_ai_request(
    '00000000-0000-0000-0000-000000000003',
    '30000000-0000-0000-0000-000000000002',
    repeat('e', 64),
    100
  )->>'decision',
  'DAILY_QUOTA_EXHAUSTED',
  'daily request 11 is rejected'
);

select is(
  public.reserve_ai_request(
    '00000000-0000-0000-0000-000000000004',
    '40000000-0000-0000-0000-000000000001',
    repeat('f', 64),
    100
  )->>'decision',
  'RESERVED',
  'first request in sixty seconds is accepted'
);

select public.finalize_ai_request(
  '00000000-0000-0000-0000-000000000004',
  '40000000-0000-0000-0000-000000000001',
  'succeeded', 200, null, null, '{}'::jsonb, 1, 1, 0, 0, 2
);

select is(
  public.reserve_ai_request(
    '00000000-0000-0000-0000-000000000004',
    '40000000-0000-0000-0000-000000000002',
    repeat('f', 64),
    100
  )->>'decision',
  'RESERVED',
  'second request in sixty seconds is accepted'
);

select public.finalize_ai_request(
  '00000000-0000-0000-0000-000000000004',
  '40000000-0000-0000-0000-000000000002',
  'succeeded', 200, null, null, '{}'::jsonb, 1, 1, 0, 0, 2
);

select is(
  public.reserve_ai_request(
    '00000000-0000-0000-0000-000000000004',
    '40000000-0000-0000-0000-000000000003',
    repeat('f', 64),
    100
  )->>'decision',
  'RESERVED',
  'third request in sixty seconds is accepted'
);

select public.finalize_ai_request(
  '00000000-0000-0000-0000-000000000004',
  '40000000-0000-0000-0000-000000000003',
  'succeeded', 200, null, null, '{}'::jsonb, 1, 1, 0, 0, 2
);

select is(
  public.reserve_ai_request(
    '00000000-0000-0000-0000-000000000004',
    '40000000-0000-0000-0000-000000000004',
    repeat('f', 64),
    100
  )->>'decision',
  'RATE_LIMITED',
  'fourth request in sixty seconds is rate limited'
);

select ok(
  (
    public.reserve_ai_request(
      '00000000-0000-0000-0000-000000000004',
      '40000000-0000-0000-0000-000000000005',
      repeat('f', 64),
      100
    )->>'retryAfterSeconds'
  )::integer > 0,
  'rate limit includes retry timing'
);

update private.ai_usage_request
set reserved_at = clock_timestamp() - interval '60.001 seconds'
where user_id = '00000000-0000-0000-0000-000000000004';

select is(
  public.reserve_ai_request(
    '00000000-0000-0000-0000-000000000004',
    '40000000-0000-0000-0000-000000000006',
    repeat('f', 64),
    100
  )->>'decision',
  'RESERVED',
  'request beyond sixty seconds is accepted'
);

select is(
  public.reserve_ai_request(
    '00000000-0000-0000-0000-000000000005',
    '50000000-0000-0000-0000-000000000001',
    repeat('1', 64),
    100
  )->>'decision',
  'RESERVED',
  'concurrency fixture is reserved'
);

select is(
  public.reserve_ai_request(
    '00000000-0000-0000-0000-000000000005',
    '50000000-0000-0000-0000-000000000002',
    repeat('2', 64),
    100
  )->>'decision',
  'CONCURRENT_REQUEST',
  'another active request is rejected'
);

insert into private.ai_usage_state (
  user_id,
  month_start,
  month_request_count,
  day_start,
  day_request_count,
  active_request_id,
  active_until
)
values (
  '00000000-0000-0000-0000-000000000006',
  date_trunc('month', now() at time zone 'UTC')::date,
  1,
  (now() at time zone 'UTC')::date,
  1,
  '60000000-0000-0000-0000-000000000001',
  now() - interval '1 second'
);

insert into private.ai_usage_request (
  user_id,
  request_id,
  request_fingerprint,
  status,
  input_character_count,
  reserved_at
)
values (
  '00000000-0000-0000-0000-000000000006',
  '60000000-0000-0000-0000-000000000001',
  repeat('3', 64),
  'reserved',
  100,
  now() - interval '50 seconds'
);

select is(
  public.reserve_ai_request(
    '00000000-0000-0000-0000-000000000006',
    '60000000-0000-0000-0000-000000000002',
    repeat('4', 64),
    100
  )->>'decision',
  'RESERVED',
  'expired lock does not block a new request'
);

select is(
  (
    select status
    from private.ai_usage_request
    where user_id = '00000000-0000-0000-0000-000000000006'
      and request_id = '60000000-0000-0000-0000-000000000001'
  ),
  'uncertain',
  'expired active request is finalized as uncertain'
);

insert into private.ai_usage_state (
  user_id,
  month_start,
  month_request_count,
  day_start,
  day_request_count
)
values (
  '00000000-0000-0000-0000-000000000007',
  date '2000-01-01',
  200,
  date '2000-01-01',
  10
);

select is(
  public.reserve_ai_request(
    '00000000-0000-0000-0000-000000000007',
    '70000000-0000-0000-0000-000000000001',
    repeat('5', 64),
    100
  )->>'decision',
  'RESERVED',
  'old UTC day and month counters are reset'
);

select is(
  (
    select month_request_count
    from private.ai_usage_state
    where user_id = '00000000-0000-0000-0000-000000000007'
  ),
  1,
  'monthly counter restarts at one after UTC reset'
);

select is(
  (
    select day_request_count
    from private.ai_usage_state
    where user_id = '00000000-0000-0000-0000-000000000007'
  ),
  1,
  'daily counter restarts at one after UTC reset'
);

select is(
  (
    select prompt_token_count
    from private.ai_usage_request
    where user_id = '00000000-0000-0000-0000-000000000001'
      and request_id = '10000000-0000-0000-0000-000000000001'
  ),
  10::bigint,
  'finalization stores prompt token usage'
);

select is(
  (
    select total_token_count
    from private.ai_usage_request
    where user_id = '00000000-0000-0000-0000-000000000001'
      and request_id = '10000000-0000-0000-0000-000000000001'
  ),
  30::bigint,
  'finalization stores total token usage'
);

select * from finish();

rollback;
