if (requireAuth()) {
  renderNav("matches");
  loadMatches();
}

async function loadMatches() {
  const matches = await api("/matches");
  const list = document.getElementById("matchesList");
  if (!matches.length) {
    list.innerHTML = `<div class="empty-state"><p>No matches yet.</p><a class="btn" href="browse.html">Browse people to swap with</a></div>`;
    return;
  }
  list.innerHTML = matches.map(m => `
    <div class="card">
      <h3>${escapeHtml(m.otherUserName)} <span class="status-badge status-${m.status}">${m.status}</span></h3>
      <p class="muted small-text">Match score: ${(m.matchScore * 100).toFixed(0)}% · Expires: ${m.expiresAt ? new Date(m.expiresAt).toLocaleString() : "-"}</p>
      ${m.status === "PENDING" ? `
        <button class="btn small accept-btn" data-id="${m.id}">Accept</button>
        <button class="btn small secondary reject-btn" data-id="${m.id}">Decline</button>` : ""}
      <button class="btn small secondary open-btn" data-id="${m.id}">Open</button>
    </div>
  `).join("");

  list.querySelectorAll(".accept-btn").forEach(b => b.onclick = () => respondMatch(b.dataset.id, true));
  list.querySelectorAll(".reject-btn").forEach(b => b.onclick = () => respondMatch(b.dataset.id, false));
  list.querySelectorAll(".open-btn").forEach(b => b.onclick = () => window.location.href = "match.html?id=" + encodeURIComponent(b.dataset.id));
}

async function respondMatch(matchId, accept) {
  try {
    await api(`/matches/${matchId}/respond`, { method: "PATCH", body: JSON.stringify({ accept }) });
    loadMatches();
  } catch (err) { alert(err.message); }
}
