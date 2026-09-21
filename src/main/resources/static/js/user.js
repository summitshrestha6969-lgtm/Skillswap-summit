renderNav("browse");
document.getElementById("backToFeedBtn").onclick = () => window.location.href = "browse.html";

async function messageUser(userId) {
  try {
    let matches = await api("/matches");
    let match = matches.find(m => String(m.otherUserId) === String(userId));
    if (!match) {
      try {
        await api(`/matches/propose/${userId}`, { method: "POST" });
      } catch (err) {
        if (!/active match already exists/i.test(err.message)) { alert(err.message); return; }
      }
      matches = await api("/matches");
      match = matches.find(m => String(m.otherUserId) === String(userId));
    }
    if (match) window.location.href = "match.html?id=" + encodeURIComponent(match.id);
    else window.location.href = "matches.html";
  } catch (err) {
    alert(err.message);
  }
}

async function loadUserProfile() {
  const userId = qs("id");
  const el = document.getElementById("servicePageContent");
  if (!userId) { el.innerHTML = `<p class="error-text">No user specified.</p>`; return; }
  el.innerHTML = `<p class="muted">Loading profile...</p>`;
  try {
    const u = await api(`/users/${userId}`);
    const offered = u.offeredSkills || [];
    const wanted = u.wantedSkills || [];

    el.innerHTML = `
      <div class="service-banner">
        <div class="service-header">
          <img class="avatar" src="${avatarUrl(u)}" alt="">
          <div>
            <h2>${escapeHtml(u.fullName)}</h2>
            <div class="service-meta">${escapeHtml(u.city || "")} ${u.ratingCount ? `· ★ ${u.avgRating.toFixed(1)} (${u.ratingCount} reviews)` : "· No reviews yet"} ${u.totalEnrollments > 0 ? `· ${u.totalEnrollments} students taught` : ""} · Lv.${u.level} ${escapeHtml(u.levelTitle)} (${u.points} pts)</div>
          </div>
        </div>
      </div>
      <div class="service-actions">
        <button class="btn" id="serviceProposeBtn">🔁 Propose a skill swap</button>
        <button class="btn secondary" id="serviceMessageBtn">💬 Message</button>
      </div>

      ${u.bio ? `<div class="service-section"><h3>About</h3><p>${escapeHtml(u.bio)}</p></div>` : ""}

      <div class="service-section">
        <h3>Services offered — free skill swaps</h3>
        <div class="service-grid">
          ${offered.length ? offered.map(s => `
            <div class="service-item-card">
              <div class="icon">${iconFor(s)}</div>
              <h4>${escapeHtml(s)}</h4>
              <span class="free-tag">Free swap</span>
            </div>`).join("") : `<p class="muted">No skills listed yet.</p>`}
        </div>
      </div>

      <div class="service-section">
        <h3>Looking to learn</h3>
        <div>${wanted.length ? wanted.map(s => `<span class="pill wanted">${escapeHtml(s)}</span>`).join("") : `<p class="muted">Nothing listed yet.</p>`}</div>
      </div>
    `;

    document.getElementById("serviceProposeBtn").onclick = async (e) => {
      if (!state.user) { promptGuestSignup(); return; }
      e.target.disabled = true;
      try {
        await api(`/matches/propose/${userId}`, { method: "POST" });
        e.target.textContent = "Match proposed!";
      } catch (err) {
        alert(err.message);
        e.target.disabled = false;
      }
    };
    document.getElementById("serviceMessageBtn").onclick = () => {
      if (!state.user) { promptGuestSignup(); return; }
      messageUser(userId);
    };
  } catch (err) {
    el.innerHTML = `<p class="error-text">${escapeHtml(err.message)}</p>`;
  }
}

loadUserProfile();
