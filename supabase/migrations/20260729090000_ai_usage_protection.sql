create schema if not exists private;

revoke all on schema private from public;

create table private.ai_usage_state (
  user_id uuid primary key,
  month_start date not null,
  month_request_count integer not null default 0
    check (month_request_count >= 0),
  day_start date not null,
  day_request_count integer not null default 0
    check (day_request_count >= 0),
  active_request_id uuid,
  active_until timestamptz,
  updated_at timestamptz not null default clock_timestamp()
);

create table private.ai_usage_request (
  user_id uuid not null,
  request_id uuid not null,
  request_fingerprint text not null
    check (char_length(request_fingerprint) = 64),
  status text not null check (
    status in (
      'reserved',
      'succeeded',
      'upstream_timeout',
      'upstream_rate_limited',
      'upstream_error',
      'invalid_ai_response',
      'service_unavailable',
      'uncertain'
    )
  ),
  response_status integer,
  error_code text,
  retry_after_seconds integer check (
    retry_after_seconds is null or retry_after_seconds >= 0
  ),
  result_payload jsonb,
  input_character_count integer not null check (
    input_character_count between 1 and 12000
  ),
  prompt_token_count bigint check (
    prompt_token_count is null or prompt_token_count >= 0
  ),
  candidates_token_count bigint check (
    candidates_token_count is null or candidates_token_count >= 0
  ),
  thoughts_token_count bigint check (
    thoughts_token_count is null or thoughts_token_count >= 0
  ),
  cached_content_token_count bigint check (
    cached_content_token_count is null or cached_content_token_count >= 0
  ),
  total_token_count bigint check (
    total_token_count is null or total_token_count >= 0
  ),
  reserved_at timestamptz not null default clock_timestamp(),
  finalized_at timestamptz,
  primary key (user_id, request_id)
);

create index ai_usage_request_user_reserved_at_idx
  on private.ai_usage_request (user_id, reserved_at desc);

alter table private.ai_usage_state enable row level security;
alter table private.ai_usage_request enable row level security;

revoke all on private.ai_usage_state from public, anon, authenticated;
revoke all on private.ai_usage_request from public, anon, authenticated;

create or replace function public.reserve_ai_request(
  p_user_id uuid,
  p_request_id uuid,
  p_request_fingerprint text,
  p_input_character_count integer
)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_now timestamptz := clock_timestamp();
  v_today date := (v_now at time zone 'UTC')::date;
  v_month date := date_trunc('month', v_now at time zone 'UTC')::date;
  v_state private.ai_usage_state%rowtype;
  v_existing private.ai_usage_request%rowtype;
  v_window_start timestamptz;
  v_recent_count integer;
  v_retry_after integer;
begin
  if p_user_id is null or p_request_id is null then
    raise exception 'user_id and request_id are required';
  end if;
  if p_request_fingerprint !~ '^[0-9a-f]{64}$' then
    raise exception 'request fingerprint must be lowercase sha256';
  end if;
  if p_input_character_count < 1 or p_input_character_count > 12000 then
    raise exception 'input character count is outside the supported range';
  end if;

  insert into private.ai_usage_state (
    user_id,
    month_start,
    day_start
  )
  values (
    p_user_id,
    v_month,
    v_today
  )
  on conflict (user_id) do nothing;

  select *
  into strict v_state
  from private.ai_usage_state
  where user_id = p_user_id
  for update;

  if v_state.month_start <> v_month then
    v_state.month_start := v_month;
    v_state.month_request_count := 0;
  end if;
  if v_state.day_start <> v_today then
    v_state.day_start := v_today;
    v_state.day_request_count := 0;
  end if;

  if v_state.active_request_id is not null
    and (
      v_state.active_until is null
      or v_state.active_until <= v_now
    )
  then
    update private.ai_usage_request
    set
      status = 'uncertain',
      response_status = 504,
      error_code = 'UPSTREAM_TIMEOUT',
      finalized_at = coalesce(finalized_at, v_now)
    where user_id = p_user_id
      and request_id = v_state.active_request_id
      and status = 'reserved';

    v_state.active_request_id := null;
    v_state.active_until := null;
  end if;

  update private.ai_usage_state
  set
    month_start = v_state.month_start,
    month_request_count = v_state.month_request_count,
    day_start = v_state.day_start,
    day_request_count = v_state.day_request_count,
    active_request_id = v_state.active_request_id,
    active_until = v_state.active_until,
    updated_at = v_now
  where user_id = p_user_id;

  select *
  into v_existing
  from private.ai_usage_request
  where user_id = p_user_id
    and request_id = p_request_id;

  if found then
    if v_existing.request_fingerprint <> p_request_fingerprint then
      return jsonb_build_object('decision', 'IDEMPOTENCY_CONFLICT');
    end if;

    if v_existing.status = 'reserved' then
      v_retry_after := greatest(
        1,
        ceil(extract(epoch from (
          coalesce(v_state.active_until, v_now + interval '1 second') - v_now
        )))::integer
      );
      return jsonb_build_object(
        'decision', 'REQUEST_IN_PROGRESS',
        'retryAfterSeconds', v_retry_after
      );
    end if;

    if v_existing.status = 'succeeded' then
      return jsonb_build_object(
        'decision', 'REPLAY_SUCCESS',
        'resultPayload', v_existing.result_payload
      );
    end if;

    return jsonb_strip_nulls(jsonb_build_object(
      'decision', 'REPLAY_FAILURE',
      'responseStatus', v_existing.response_status,
      'errorCode', v_existing.error_code,
      'retryAfterSeconds', v_existing.retry_after_seconds
    ));
  end if;

  if v_state.active_request_id is not null then
    v_retry_after := greatest(
      1,
      ceil(extract(epoch from (v_state.active_until - v_now)))::integer
    );
    return jsonb_build_object(
      'decision', 'CONCURRENT_REQUEST',
      'retryAfterSeconds', v_retry_after
    );
  end if;

  if v_state.month_request_count >= 200 then
    v_retry_after := greatest(
      1,
      ceil(extract(epoch from (
        ((v_month + interval '1 month') at time zone 'UTC') - v_now
      )))::integer
    );
    return jsonb_build_object(
      'decision', 'MONTHLY_QUOTA_EXHAUSTED',
      'retryAfterSeconds', v_retry_after
    );
  end if;

  if v_state.day_request_count >= 10 then
    v_retry_after := greatest(
      1,
      ceil(extract(epoch from (
        ((v_today + 1)::timestamp at time zone 'UTC') - v_now
      )))::integer
    );
    return jsonb_build_object(
      'decision', 'DAILY_QUOTA_EXHAUSTED',
      'retryAfterSeconds', v_retry_after
    );
  end if;

  select count(*), min(reserved_at)
  into v_recent_count, v_window_start
  from private.ai_usage_request
  where user_id = p_user_id
    and reserved_at > v_now - interval '60 seconds';

  if v_recent_count >= 3 then
    v_retry_after := greatest(
      1,
      ceil(extract(epoch from (
        v_window_start + interval '60 seconds' - v_now
      )))::integer
    );
    return jsonb_build_object(
      'decision', 'RATE_LIMITED',
      'retryAfterSeconds', v_retry_after
    );
  end if;

  insert into private.ai_usage_request (
    user_id,
    request_id,
    request_fingerprint,
    status,
    input_character_count,
    reserved_at
  )
  values (
    p_user_id,
    p_request_id,
    p_request_fingerprint,
    'reserved',
    p_input_character_count,
    v_now
  );

  update private.ai_usage_state
  set
    month_request_count = month_request_count + 1,
    day_request_count = day_request_count + 1,
    active_request_id = p_request_id,
    active_until = v_now + interval '45 seconds',
    updated_at = v_now
  where user_id = p_user_id;

  return jsonb_build_object('decision', 'RESERVED');
end;
$$;

create or replace function public.finalize_ai_request(
  p_user_id uuid,
  p_request_id uuid,
  p_status text,
  p_response_status integer,
  p_error_code text,
  p_retry_after_seconds integer,
  p_result_payload jsonb,
  p_prompt_token_count bigint,
  p_candidates_token_count bigint,
  p_thoughts_token_count bigint,
  p_cached_content_token_count bigint,
  p_total_token_count bigint
)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_now timestamptz := clock_timestamp();
begin
  if p_status not in (
    'succeeded',
    'upstream_timeout',
    'upstream_rate_limited',
    'upstream_error',
    'invalid_ai_response',
    'service_unavailable'
  ) then
    raise exception 'unsupported final status';
  end if;

  -- Match reserve_ai_request's lock order: state first, request ledger second.
  -- This prevents a late finalization from deadlocking with lease reclamation.
  perform user_id
  from private.ai_usage_state
  where user_id = p_user_id
  for update;

  update private.ai_usage_request
  set
    status = p_status,
    response_status = p_response_status,
    error_code = p_error_code,
    retry_after_seconds = p_retry_after_seconds,
    result_payload = p_result_payload,
    prompt_token_count = p_prompt_token_count,
    candidates_token_count = p_candidates_token_count,
    thoughts_token_count = p_thoughts_token_count,
    cached_content_token_count = p_cached_content_token_count,
    total_token_count = p_total_token_count,
    finalized_at = v_now
  where user_id = p_user_id
    and request_id = p_request_id
    and status in ('reserved', 'uncertain');

  update private.ai_usage_state
  set
    active_request_id = null,
    active_until = null,
    updated_at = v_now
  where user_id = p_user_id
    and active_request_id = p_request_id;
end;
$$;

revoke all on function public.reserve_ai_request(
  uuid,
  uuid,
  text,
  integer
) from public, anon, authenticated;

revoke all on function public.finalize_ai_request(
  uuid,
  uuid,
  text,
  integer,
  text,
  integer,
  jsonb,
  bigint,
  bigint,
  bigint,
  bigint,
  bigint
) from public, anon, authenticated;

grant execute on function public.reserve_ai_request(
  uuid,
  uuid,
  text,
  integer
) to service_role;

grant execute on function public.finalize_ai_request(
  uuid,
  uuid,
  text,
  integer,
  text,
  integer,
  jsonb,
  bigint,
  bigint,
  bigint,
  bigint,
  bigint
) to service_role;
