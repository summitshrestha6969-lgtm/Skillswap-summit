renderNav("browse");

let lastBrowseResults = [];

function feedCard(u) {
  const offered = (u.offeredSkills || []).slice(0, 4).map(s => `<span class="pill">${escapeHtml(s)}</span>`).join("");
  const wanted = (u.wantedSkills || []).slice(0, 3).map(s => `<span class="pill wanted">${escapeHtml(s)}</span>`).join("");
  return `<div class="feed-card">
    <div class="feed-card-head">
      <img class="avatar" src="${avatarUrl(u)}" alt="">
      <div>
        <h4>${escapeHtml(u.fullName)} <span class="small-text muted">Lv.${u.level}</span></h4>
        <div class="sub">${escapeHtml(u.city || "")} ${u.ratingCount ? `· ★ ${u.avgRating.toFixed(1)} (${u.ratingCount})` : "· No reviews yet"}</div>
      </div>
    </div>
    ${offered ? `<div class="skills-block"><span class="skills-label">Teaches</span>${offered}</div>` : ""}
    ${wanted ? `<div class="skills-block"><span class="skills-label">Wants to learn</span>${wanted}</div>` : ""}
    <div class="feed-card-actions">
      <button class="btn small outline view-profile-btn" data-user="${u.id}">View profile</button>
      <button class="btn small message-btn" data-user="${u.id}" data-name="${escapeHtml(u.fullName)}">💬 Message</button>
    </div>
  </div>`;
}

async function loadBrowse() {
  try {
    if (state.user) {
      await renderMyProfileMini();
      const suggestions = await api("/matches/suggestions");
      renderSuggestionsStrip(suggestions);
      renderSimilarSidebar(suggestions);
    } else {
      renderGuestSidebars();
    }
    await fetchBrowseWithFilters();
  } catch (err) {
    console.error(err);
  }
}

function renderGuestSidebars() {
  document.getElementById("accountShortcuts").classList.add("hidden");
  document.getElementById("suggestionsStrip").innerHTML = "";
  document.getElementById("composerAvatar").src = "https://ui-avatars.com/api/?name=Guest&background=1e8f5e&color=fff";
  document.getElementById("myProfileMini").innerHTML = `
    <div class="feature-icon" style="margin:0 auto 10px;">👋</div>
    <h4>Join to get matched</h4>
    <p class="mini-meta">Create a free account to see your personalized matches and message people.</p>
    <button class="btn small" id="guestSignupBtnLeft">Sign up free</button>`;
  document.getElementById("guestSignupBtnLeft").onclick = () => window.location.href = "register.html";

  document.getElementById("similarList").innerHTML = `
    <p class="muted small-text">Sign up and add your skills to see people with similar interests here.</p>
    <button class="btn small top-gap" id="guestSignupBtnRight">Sign up free</button>`;
  document.getElementById("guestSignupBtnRight").onclick = () => window.location.href = "register.html";
}

async function renderMyProfileMini() {
  try {
    document.getElementById("accountShortcuts").classList.remove("hidden");
    const me = await api("/users/me");
    document.getElementById("composerAvatar").src = avatarUrl(me);
    document.getElementById("myProfileMini").innerHTML = `
      <img class="avatar" src="${avatarUrl(me)}" alt="">
      <h4>${escapeHtml(me.fullName)}</h4>
      <div class="mini-meta">${escapeHtml(me.city || "")} ${me.ratingCount ? `· ★ ${me.avgRating.toFixed(1)}` : ""} · Lv.${me.level} ${escapeHtml(me.levelTitle)}</div>
      <div class="mini-skills">${(me.offeredSkills || []).slice(0, 4).map(s => `<span class="pill">${escapeHtml(s)}</span>`).join("")}</div>`;
  } catch (err) { console.error(err); }
}

function renderSuggestionsStrip(suggestions) {
  const el = document.getElementById("suggestionsStrip");
  if (!suggestions.length) { el.innerHTML = ""; return; }
  el.innerHTML = `<h3 class="feed-heading">Suggested for you</h3>
    <div class="suggestion-strip-inner">
      ${suggestions.map(s => `
        <div class="suggestion-chip-card">
          <img class="avatar" src="/api/users/${s.otherUserId}/image"
               onerror="this.onerror=null;this.src='https://ui-avatars.com/api/?name=${encodeURIComponent(s.otherUserName)}&background=1e8f5e&color=fff';" alt="">
          <h5>${escapeHtml(s.otherUserName)}</h5>
          <span class="score-tag">${(s.matchScore * 100).toFixed(0)}% match</span>
          <button class="btn small propose-btn" data-user="${s.otherUserId}">Propose</button>
        </div>`).join("")}
    </div>`;
  bindProposeButtons();
}

function renderSimilarSidebar(suggestions) {
  const el = document.getElementById("similarList");
  if (!suggestions.length) { el.innerHTML = `<p class="muted small-text">Add some skills to your profile to see similar people here.</p>`; return; }
  el.innerHTML = suggestions.slice(0, 6).map(s => `
    <div class="similar-item">
      <img class="avatar" src="/api/users/${s.otherUserId}/image"
           onerror="this.onerror=null;this.src='https://ui-avatars.com/api/?name=${encodeURIComponent(s.otherUserName)}&background=1e8f5e&color=fff';" alt="">
      <div class="info">
        <h5>${escapeHtml(s.otherUserName)}</h5>
        <div class="tag">${(s.matchScore * 100).toFixed(0)}% skill match</div>
      </div>
      <button class="btn small propose-btn" data-user="${s.otherUserId}">+</button>
    </div>`).join("");
  bindProposeButtons();
}

async function fetchBrowseWithFilters() {
  const params = new URLSearchParams();
  const bucket = document.getElementById("reviewsBucketFilter").value;
  const minEnroll = document.getElementById("minEnrollmentsFilter").value;
  const hasFeedback = document.getElementById("hasFeedbackFilter").checked;
  if (bucket) params.set("reviewsBucket", bucket);
  if (minEnroll) params.set("minEnrollments", minEnroll);
  if (hasFeedback) params.set("hasFeedback", "true");

  lastBrowseResults = await api("/users/browse?" + params.toString());
  filterBrowseGrid();
}

function filterBrowseGrid() {
  const q = document.getElementById("browseSearch").value.trim().toLowerCase();
  const filtered = q
    ? lastBrowseResults.filter(u =>
        (u.offeredSkills || []).some(s => s.toLowerCase().includes(q)) ||
        (u.wantedSkills || []).some(s => s.toLowerCase().includes(q)) ||
        u.fullName.toLowerCase().includes(q))
    : lastBrowseResults;

  document.getElementById("browseGrid").innerHTML = filtered.length
    ? filtered.map(u => feedCard(u)).join("")
    : `<p class="muted">No members match these filters yet.</p>`;
  bindFeedCardButtons();
}
["reviewsBucketFilter", "minEnrollmentsFilter", "hasFeedbackFilter"].forEach(id => {
  document.getElementById(id).addEventListener("change", fetchBrowseWithFilters);
});
document.getElementById("browseSearch").addEventListener("input", filterBrowseGrid);

function bindFeedCardButtons() {
  document.querySelectorAll(".view-profile-btn").forEach(btn => {
    btn.onclick = () => window.location.href = "user.html?id=" + encodeURIComponent(btn.dataset.user);
  });
  document.querySelectorAll(".message-btn").forEach(btn => {
    btn.onclick = () => {
      if (!state.user) { promptGuestSignup(); return; }
      messageUser(btn.dataset.user);
    };
  });
}

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

function bindProposeButtons() {
  document.querySelectorAll(".propose-btn").forEach(btn => {
    btn.onclick = async () => {
      if (!state.user) { promptGuestSignup(); return; }
      btn.disabled = true;
      try {
        await api(`/matches/propose/${btn.dataset.user}`, { method: "POST" });
        btn.textContent = "Match proposed!";
      } catch (err) {
        alert(err.message);
        btn.disabled = false;
      }
    };
  });
}

const initialQuery = qs("q");
if (initialQuery) document.getElementById("browseSearch").value = initialQuery;

loadBrowse();