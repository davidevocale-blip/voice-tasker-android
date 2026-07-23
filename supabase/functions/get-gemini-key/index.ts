const corsHeaders: HeadersInit = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers":
    "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
}

const jsonHeaders: HeadersInit = {
  ...corsHeaders,
  "Content-Type": "application/json; charset=utf-8",
}

export function handler(request: Request): Response {
  if (request.method === "OPTIONS") {
    return new Response(null, {
      status: 204,
      headers: corsHeaders,
    })
  }

  return new Response(
    JSON.stringify({ code: "ENDPOINT_DECOMMISSIONED" }),
    {
      status: 410,
      headers: jsonHeaders,
    },
  )
}

if (import.meta.main) {
  Deno.serve(handler)
}
