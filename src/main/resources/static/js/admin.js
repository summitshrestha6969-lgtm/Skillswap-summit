let adminPollTimer = null;

if (requireAdmin()) {
  renderNav("admin");
  loadAdmin();
  adminPollTimer = setInterval(() => loadAdmin().catch(() => {}), 10000);
  window.addEventListener("beforeunload", () => { if (adminPollTimer) clearInterval(adminPollTimer); });
}

async function loadAdmin() {
  const [stats, users, notifications] = await Promise.all([
    api("/admin/dashboard"), api("/admin/users"), api("/admin/notifications"),
  ]);
  document.getElementById("dashboardStats").innerHTML = Object.entries(stats).map(([k, v]) =>
    `<div class="card"><h3>${v}</h3><p class="muted">${k}</p></div>`).join("");

  document.getElementById("adminUserList").innerHTML = users.map(u => `
    <div class="card">
      <strong>${escapeHtml(u.fullName)}</strong> ${u.role === "ROLE_ADMIN" ? '<span class="status-badge status-ACCEPTED">ADMIN</span>' : ""} - ${escapeHtml(u.email)} ${u.emailVerified ? "✅" : "⚠️ unverified"}
      · ${u.ratingCount} reviews · ${u.totalEnrollments} taught · ⭐ ${u.points} pts (Lv.${u.level})
      ${u.active ? (u.role !== "ROLE_ADMIN" ? `<button class="btn small danger deactivate-btn" data-id="${u.id}">Deactivate</button>` : "") : "<em>deactivated</em>"}
    </div>`).join("");

  document.querySelectorAll(".deactivate-btn").forEach(b => {
    b.onclick = async () => { await api(`/admin/users/${b.dataset.id}/deactivate`, { method: "PATCH" }); loadAdmin(); };
  });

  const feed = document.getElementById("adminFeed");
  feed.innerHTML = notifications.map(n =>
    `<div class="admin-feed-item"><div>${escapeHtml(n.message)}</div><div class="time">${new Date(n.createdAt).toLocaleString()}</div></div>`
  ).join("") || `<p class="muted small-text">No activity yet.</p>`;
}

document.getElementById("sendAnnouncementBtn").onclick = async () => {
  const msgBox = document.getElementById("announceMsg");
  const title = document.getElementById("announceTitle").value.trim();
  const body = document.getElementById("announceBody").value.trim();
  if (!title || !body) { msgBox.textContent = "Title and body are required."; return; }
  try {
    await api("/admin/announcements", {
      method: "POST",
      body: JSON.stringify({ type: document.getElementById("announceType").value, title, body }),
    });
    msgBox.textContent = "Announcement sent — emailed to every active user.";
    msgBox.className = "small-text success-text";
    document.getElementById("announceTitle").value = "";
    document.getElementById("announceBody").value = "";
    loadAdmin();
  } catch (err) {
    msgBox.textContent = err.message;
    msgBox.className = "small-text error-text";
  }
};
