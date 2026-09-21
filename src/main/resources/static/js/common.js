// ============================================================================
// SkillSwap common.js
// Shared across every page: the `state` object, auth guards, nav rendering,
// and small formatting helpers. Include this AFTER backend.js and BEFORE
// the page's own script on every page.
//
// Auth here is a server-side session cookie (set by the Spring Boot backend
// on /api/auth/login or /api/auth/register), not a bearer token - so there's
// nothing to store except who's currently logged in, for the UI to react to.
// The browser sends the session cookie automatically on every fetch() as
// long as it's same-origin (this frontend is served BY the same Spring Boot
// app) and credentials are included (see api() in backend.js).
// ============================================================================

const state = {
  user: JSON.parse(localStorage.getItem("skillswap_user") || "null"),
};

function escapeHtml(s) {
  const d = document.createElement("div");
  d.innerText = s == null ? "" : s;
  return d.innerHTML;
}

function avatarUrl(u) {
  const name = (u && (u.fullName || u.otherUserName)) || "?";
  if (u && u.hasImage && u.id) return `/api/users/${u.id}/image`;
  return `https://ui-avatars.com/api/?name=${encodeURIComponent(name)}&background=1e8f5e&color=fff`;
}

const SKILL_ICONS = {
  music: "🎵", guitar: "🎸", piano: "🎹", singing: "🎤", "music theory": "🎼",
  technology: "💻", python: "🐍", javascript: "💻", java: "☕", excel: "📊", "web development": "🌐",
  design: "🎨", "graphic design": "🎨", "ui/ux design": "🖌️",
  lifestyle: "🌿", cooking: "🍳", baking: "🧁",
  creative: "📷", photography: "📷", "video editing": "🎬",
  language: "🗣️", spanish: "🗣️", french: "🗣️", nepali: "🗣️",
  fitness: "🧘", yoga: "🧘", meditation: "🧘", "soft skills": "🎯", "public speaking": "🎯", games: "♟️", chess: "♟️",
};
function iconFor(skillName) { return SKILL_ICONS[(skillName || "").toLowerCase()] || "✨"; }

function timeAgo(iso) {
  if (!iso) return "";
  const diff = Date.now() - new Date(iso).getTime();
  const mins = Math.floor(diff / 60000);
  if (mins < 1) return "just now";
  if (mins < 60) return mins + "m ago";
  const hrs = Math.floor(mins / 60);
  if (hrs < 24) return hrs + "h ago";
  return Math.floor(hrs / 24) + "d ago";
}

// ---------- auth guards ----------
function requireAuth() {
  if (!state.user) { window.location.href = "login.html"; return false; }
  return true;
}
function requireAdmin() {
  if (!requireAuth()) return false;
  if (state.user.role !== "ROLE_ADMIN") { window.location.href = "browse.html"; return false; }
  return true;
}
function logout() {
  api("/auth/logout", { method: "POST" }).catch(() => { /* clear locally regardless */ });
  localStorage.removeItem("skillswap_user");
  window.location.href = "index.html";
}
function persistSession(data) {
  state.user = { id: data.userId, fullName: data.fullName, role: data.role };
  localStorage.setItem("skillswap_user", JSON.stringify(state.user));
}

// ---------- shared nav ----------
const NAV_LINKS = [
  { id: "home", label: "Home", href: "index.html", guestOk: true },
  { id: "browse", label: "Browse", href: "browse.html", guestOk: true },
  { id: "leaderboard", label: "🏆 Leaderboard", href: "leaderboard.html", guestOk: true },
  { id: "rewards", label: "🎁 Rewards", href: "rewards.html" },
  { id: "matches", label: "💬 Matches", href: "matches.html" },
  { id: "profile", label: "Profile", href: "profile.html" },
  { id: "admin", label: "Admin", href: "admin.html", adminOnly: true },
];

async function renderNav(activeId) {
  const el = document.getElementById("site-header");
  if (!el) return;

  let pointsPill = "";
  if (state.user) {
    try {
      const g = await api("/gamification/me");
      pointsPill = `<a href="rewards.html" class="nav-points-pill">⭐ ${g.points} pts · Lv.${g.level} ${escapeHtml(g.title)}</a>`;
    } catch {
      // Session cookie expired or was cleared server-side (e.g. server
      // restarted) - the local "logged in" flag is now stale, so clear it
      // and treat this page load as logged out instead of showing a broken nav.
      state.user = null;
      localStorage.removeItem("skillswap_user");
    }
  }

  const loggedIn = !!state.user;
  const isAdmin = loggedIn && state.user.role === "ROLE_ADMIN";
  const links = NAV_LINKS.filter(l => (loggedIn || l.guestOk) && (!l.adminOnly || isAdmin))
    .map(l => `<a href="${l.href}" class="nav-link ${activeId === l.id ? "active" : ""}">${l.label}</a>`).join("");

  el.innerHTML = `
    <header>
      <a href="${loggedIn ? "browse.html" : "index.html"}" class="brand">🔄 SkillSwap</a>
      <nav>
        ${links}
        ${loggedIn
          ? `${pointsPill}<button id="navLogoutBtn">Log out</button>`
          : `<button id="navLoginBtn">Log in</button><button id="navSignupBtn">Sign up</button>`}
      </nav>
    </header>`;

  if (loggedIn) {
    document.getElementById("navLogoutBtn").onclick = logout;
  } else {
    document.getElementById("navLoginBtn").onclick = () => window.location.href = "login.html";
    document.getElementById("navSignupBtn").onclick = () => window.location.href = "register.html";
  }
}

/** Shown whenever a guest hits something that genuinely requires an account. */
function promptGuestSignup() {
  if (confirm("You'll need a free SkillSwap account to do that. Sign up now?")) {
    window.location.href = "register.html";
  }
}

function qs(name) {
  return new URLSearchParams(window.location.search).get(name);
}
