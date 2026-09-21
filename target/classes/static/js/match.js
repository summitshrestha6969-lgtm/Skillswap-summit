const matchId = qs("id");
const activeSessionState = { session: null, deliveryMode: "ONLINE" };

document.getElementById("backToMatchesBtn").onclick = () => window.location.href = "matches.html";

function bubbleHtml(m) {
  const mine = m.sender.id === state.user.id;
  return `<div class="bubble ${mine ? "mine" : "theirs"}">${escapeHtml(m.content)}</div>`;
}

async function loadThread() {
  const messages = await api(`/matches/${matchId}/messages`);
  const box = document.getElementById("thread");
  box.innerHTML = messages.map(m => bubbleHtml(m)).join("") || `<p class="muted small-text">No messages yet - say hello!</p>`;
  box.scrollTop = box.scrollHeight;
}

document.getElementById("sendMessageBtn").onclick = async () => {
  const input = document.getElementById("messageInput");
  if (!input.value.trim()) return;
  try {
    await api(`/matches/${matchId}/messages`, { method: "POST", body: JSON.stringify({ content: input.value }) });
    input.value = "";
    loadThread();
  } catch (err) { alert(err.message); }
};
document.getElementById("messageInput").addEventListener("keydown", e => { if (e.key === "Enter") document.getElementById("sendMessageBtn").click(); });

document.getElementById("modeOnlineBtn").onclick = () => setDeliveryMode("ONLINE");
document.getElementById("modeOfflineBtn").onclick = () => setDeliveryMode("OFFLINE");
function setDeliveryMode(mode) {
  activeSessionState.deliveryMode = mode;
  document.getElementById("modeOnlineBtn").classList.toggle("selected", mode === "ONLINE");
  document.getElementById("modeOfflineBtn").classList.toggle("selected", mode === "OFFLINE");
  document.getElementById("sessionDownloadUrl").classList.toggle("hidden", mode !== "OFFLINE");
}

async function loadSessionInfo() {
  const infoBox = document.getElementById("sessionInfo");
  const actionButtons = document.getElementById("accessButtons");
  try {
    const session = await api(`/matches/${matchId}/session`);
    activeSessionState.session = session;
    infoBox.innerHTML = `Scheduled: ${new Date(session.scheduledAt).toLocaleString()} at ${escapeHtml(session.location || "TBD")}
      (${session.status}) · ${session.deliveryMode === "ONLINE" ? "Online video call" : "Offline download"}
      ${session.capacity > 1 ? ` · capacity ${session.capacity}` : ""}`;
    actionButtons.innerHTML = session.deliveryMode === "ONLINE"
      ? `<button class="icon-btn video" id="joinVideoBtn">🎥 Join video call</button>`
      : `<button class="icon-btn download" id="downloadBtn">⬇️ Download materials</button>`;
    const joinBtn = document.getElementById("joinVideoBtn");
    const dlBtn = document.getElementById("downloadBtn");
    if (joinBtn) joinBtn.onclick = () => hitAccess("JOIN_VIDEO");
    if (dlBtn) dlBtn.onclick = () => hitAccess("DOWNLOAD_MATERIAL");
    document.getElementById("ratingBox").classList.remove("hidden");
  } catch {
    activeSessionState.session = null;
    infoBox.textContent = "No session scheduled yet.";
    actionButtons.innerHTML = "";
    document.getElementById("ratingBox").classList.add("hidden");
  }
}

document.getElementById("scheduleBtn").onclick = async () => {
  const dt = document.getElementById("sessionDateTime").value;
  const loc = document.getElementById("sessionLocation").value;
  const capacity = Number(document.getElementById("sessionCapacity").value) || 1;
  const downloadUrl = document.getElementById("sessionDownloadUrl").value;
  if (!dt) return;
  if (activeSessionState.deliveryMode === "OFFLINE" && !downloadUrl.trim()) {
    alert("Please provide a downloadable resource link for an offline session.");
    return;
  }
  try {
    await api(`/matches/${matchId}/session`, {
      method: "POST",
      body: JSON.stringify({ scheduledAt: dt, location: loc, capacity, deliveryMode: activeSessionState.deliveryMode, downloadUrl }),
    });
    loadSessionInfo();
  } catch (err) { alert(err.message); }
};

async function hitAccess(action) {
  const msgBox = document.getElementById("accessMsg");
  msgBox.textContent = "";
  try {
    const res = await api(`/matches/${matchId}/session/${activeSessionState.session.id}/access`, { method: "POST", body: JSON.stringify({ action }) });
    if (res.url) window.open(res.url, "_blank");
    msgBox.textContent = (action === "JOIN_VIDEO" ? "Opening video call..." : "Download started...") + " - you just earned points for attending!";
  } catch (err) {
    msgBox.textContent = err.message;
  }
}

document.getElementById("submitRatingBtn").onclick = async () => {
  const msgBox = document.getElementById("ratingMsg");
  if (!activeSessionState.session) { msgBox.textContent = "Schedule a session first."; return; }
  try {
    await api(`/sessions/${activeSessionState.session.id}/ratings`, {
      method: "POST",
      body: JSON.stringify({ score: Number(document.getElementById("ratingScore").value), comment: document.getElementById("ratingComment").value }),
    });
    msgBox.textContent = "Rating submitted - thanks! Points awarded.";
    msgBox.className = "small-text success-text";
  } catch (err) { msgBox.textContent = err.message; }
};

async function init() {
  if (!requireAuth()) return;
  renderNav("matches");
  if (!matchId) { document.getElementById("matchDetailTitle").textContent = "No match specified."; return; }
  try {
    const matches = await api("/matches");
    const match = matches.find(m => m.id === matchId);
    document.getElementById("matchDetailTitle").textContent = "Conversation with " + (match ? match.otherUserName : "...");
  } catch { /* ignore, keep generic title */ }
  setDeliveryMode("ONLINE");
  await loadThread();
  await loadSessionInfo();
}
init();
