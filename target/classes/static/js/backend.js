// ============================================================================
// SkillSwap API client
// ----------------------------------------------------------------------------
// This file used to contain an in-browser fake database (localStorage-based
// mock backend) so the frontend could be demoed with no server. That's gone
// now - this frontend is served directly by the Spring Boot backend (see
// src/main/resources/static/ in the backend project), so every "api()" call
// here is a REAL HTTP request to the REAL backend, backed by MySQL/H2.
//
// Auth is a server-side session cookie (JSESSIONID), set automatically by
// the browser when /api/auth/login or /api/auth/register succeeds.
// `credentials: "include"` below is what makes the browser send that cookie
// on every later request - without it, you'd be "logged in" for one request
// and logged out on the next.
// ============================================================================

async function api(path, options = {}) {
  const method = (options.method || "GET").toUpperCase();
  const headers = {};
  const body = options.body;

  // Only set Content-Type for JSON bodies - file uploads (FormData) must be
  // left alone so the browser can set the correct multipart boundary itself.
  if (body && !(body instanceof FormData)) {
    headers["Content-Type"] = "application/json";
  }

  let res;
  try {
    res = await fetch("/api" + path, {
      method,
      headers,
      credentials: "include", // send/receive the session cookie
      body,
    });
  } catch (networkErr) {
    throw new Error("Could not reach the server. Is the SkillSwap backend running?");
  }

  let data = null;
  const contentType = res.headers.get("content-type") || "";
  if (contentType.includes("application/json")) {
    try { data = await res.json(); } catch { data = null; }
  }

  if (!res.ok) {
    const message = (data && data.message) ? data.message : `Request failed (${res.status})`;
    throw new Error(message);
  }

  return data;
}
