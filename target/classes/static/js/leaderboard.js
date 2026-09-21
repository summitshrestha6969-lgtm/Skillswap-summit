renderNav("leaderboard");

async function loadMyGamification() {
  if (!state.user) {
    document.getElementById("levelCardWrap").innerHTML = `
      <div class="card" style="text-align:center;">
        <p>Log in to see your points, level, and badges.</p>
        <a class="btn" href="login.html">Log in</a>
        <a class="btn secondary" href="register.html">Sign up free</a>
      </div>`;
    return;
  }
  try {
    const g = await api("/gamification/me");
    document.getElementById("levelCardWrap").innerHTML = `
      <div class="level-card">
        <div class="level-top">
          <div>
            <div class="level-points">Level ${g.level}</div>
            <div class="level-title">${g.icon} ${escapeHtml(g.title)}</div>
          </div>
          <div class="level-points">⭐ ${g.points} points</div>
        </div>
        <div class="progress-track"><div class="progress-fill" style="width:${g.progressPct}%;"></div></div>
        <div class="progress-caption">${g.nextTitle ? `${g.progressPct}% to Level ${g.level + 1}: ${escapeHtml(g.nextTitle)} (${g.nextAt} pts)` : "You've reached the top level!"}</div>
        ${g.streakDays > 0 ? `<div class="streak-tag">🔥 ${g.streakDays}-day login streak</div>` : ""}
      </div>`;

    document.getElementById("badgesSection").classList.remove("hidden");
    document.getElementById("badgeGrid").innerHTML = g.badges.map(b => `
      <div class="badge-card ${b.earned ? "earned" : ""}">
        <div class="badge-icon">${b.icon}</div>
        <h4>${escapeHtml(b.name)}</h4>
        <p>${escapeHtml(b.description)}</p>
      </div>`).join("");
  } catch (err) {
    console.error(err);
  }
}

async function loadLeaderboard() {
  try {
    const rows = await api("/leaderboard");
    const el = document.getElementById("leaderboardList");
    if (!rows.length) { el.innerHTML = `<p class="muted">No ranked members yet.</p>`; return; }
    el.innerHTML = rows.map(r => `
      <div class="leaderboard-row ${r.rank <= 3 ? "top" + r.rank : ""} ${state.user && r.id === state.user.id ? "me" : ""}">
        <div class="rank">${r.rank === 1 ? "🥇" : r.rank === 2 ? "🥈" : r.rank === 3 ? "🥉" : "#" + r.rank}</div>
        <img class="avatar" src="${avatarUrl(r)}" alt="">
        <div class="lb-info">
          <h4>${escapeHtml(r.fullName)}${state.user && r.id === state.user.id ? " (you)" : ""}</h4>
          <div class="sub">Level ${r.level} · ${escapeHtml(r.title)} · ${r.badgeCount} badge${r.badgeCount === 1 ? "" : "s"}</div>
        </div>
        <div class="lb-points">${r.points}<span class="unit">points</span></div>
      </div>`).join("");
  } catch (err) {
    console.error(err);
  }
}

loadMyGamification();
loadLeaderboard();
