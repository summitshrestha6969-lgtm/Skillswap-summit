if (requireAuth()) {
  renderNav("rewards");
  loadRewards();
  loadRedemptions();
}

async function loadRewards() {
  try {
    const [catalog, gam] = await Promise.all([api("/rewards/catalog"), api("/gamification/me")]);
    document.getElementById("pointsSummary").textContent = `You have ${gam.points} points to spend.`;
    document.getElementById("rewardGrid").innerHTML = catalog.map(r => `
      <div class="reward-card">
        <div class="reward-icon">${r.icon}</div>
        <h4>${escapeHtml(r.name)}</h4>
        <div class="reward-cost">${r.cost} pts</div>
        <p class="reward-desc">${escapeHtml(r.description)}</p>
        <button class="btn small redeem-btn" data-id="${r.id}" data-cost="${r.cost}" ${r.canAfford ? "" : "disabled"}>
          ${r.canAfford ? "Redeem" : "Not enough points"}
        </button>
      </div>`).join("");

    document.querySelectorAll(".redeem-btn").forEach(btn => {
      btn.onclick = async () => {
        if (!confirm(`Redeem this reward for ${btn.dataset.cost} points?`)) return;
        btn.disabled = true;
        try {
          await api(`/rewards/redeem/${btn.dataset.id}`, { method: "POST" });
          await loadRewards();
          await loadRedemptions();
        } catch (err) {
          alert(err.message);
          btn.disabled = false;
        }
      };
    });
  } catch (err) {
    console.error(err);
  }
}

async function loadRedemptions() {
  try {
    const redemptions = await api("/rewards/me");
    const el = document.getElementById("redemptionsList");
    el.innerHTML = redemptions.length
      ? redemptions.map(r => `
        <div class="redemption-item">
          <div>${escapeHtml(r.name)} <span class="muted">(${r.cost} pts)</span></div>
          <div class="time">${timeAgo(r.date)}</div>
        </div>`).join("")
      : `<p class="muted small-text">No rewards redeemed yet.</p>`;
  } catch (err) {
    console.error(err);
  }
}
