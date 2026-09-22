const profileState = { offeredSkills: new Set(), wantedSkills: new Set() };

if (requireAuth()) {
  renderNav("profile");
  if (qs("welcome")) document.getElementById("welcomeBanner").classList.remove("hidden");
  loadProfile();
}

async function loadProfile() {
  const p = await api("/users/me");
  document.getElementById("profileAvatar").src = avatarUrl(p);
  document.getElementById("verifiedNote").textContent = p.emailVerified
    ? "✅ Email verified - your skills are publicly searchable."
    : "⚠️ Email not verified yet - your skills won't appear in Browse until you verify. Check your email for the code.";

  document.getElementById("bioInput").value = p.bio || "";

  profileState.offeredSkills = new Set(p.offeredSkills || []);
  profileState.wantedSkills = new Set(p.wantedSkills || []);
  renderTags("offeredTags", profileState.offeredSkills, "offered");
  renderTags("wantedTags", profileState.wantedSkills, "wanted");

  try {
    const catalog = await api("/skills/catalog");
    document.getElementById("skillCatalogList").innerHTML = catalog.map(s => `<option value="${escapeHtml(s.name)}">`).join("");
  } catch { /* catalog is a nice-to-have */ }
}

function renderTags(containerId, set, kind) {
  const el = document.getElementById(containerId);
  el.innerHTML = [...set].map(s => `<span class="pill ${kind === "wanted" ? "wanted" : ""}">${escapeHtml(s)} <a href="#" data-kind="${kind}" data-skill="${escapeHtml(s)}" class="remove-tag" style="text-decoration:none;">✕</a></span>`).join("");
  el.querySelectorAll(".remove-tag").forEach(a => {
    a.onclick = (e) => {
      e.preventDefault();
      (a.dataset.kind === "wanted" ? profileState.wantedSkills : profileState.offeredSkills).delete(a.dataset.skill);
      renderTags(containerId, a.dataset.kind === "wanted" ? profileState.wantedSkills : profileState.offeredSkills, kind);
    };
  });
}
function bindSkillInput(inputId, containerId, kind) {
  document.getElementById(inputId).addEventListener("keydown", (e) => {
    if (e.key === "Enter" && e.target.value.trim()) {
      e.preventDefault();
      const set = kind === "wanted" ? profileState.wantedSkills : profileState.offeredSkills;
      set.add(e.target.value.trim());
      renderTags(containerId, set, kind);
      e.target.value = "";
    }
  });
}
bindSkillInput("offeredInput", "offeredTags", "offered");
bindSkillInput("wantedInput", "wantedTags", "wanted");

document.getElementById("saveSkillsBtn").onclick = async () => {
  const msgBox = document.getElementById("skillsMsg");

  commitPendingInput("offeredInput", "offeredTags", "offered");
  commitPendingInput("wantedInput", "wantedTags", "wanted");

  try {
    await api("/users/me/skills", {
      method: "PUT",
      body: JSON.stringify({
        bio: document.getElementById("bioInput").value.trim(),
        offeredSkills: [...profileState.offeredSkills],
        wantedSkills: [...profileState.wantedSkills],
      }),
    });
    msgBox.textContent = "Profile saved! Check the Leaderboard page to see your updated points.";
    msgBox.className = "small-text success-text";
  } catch (err) {
    msgBox.textContent = err.message;
    msgBox.className = "small-text error-text";
  }
};

function commitPendingInput(inputId, containerId, kind) {
  const input = document.getElementById(inputId);
  const value = input.value.trim();
  if (!value) return;
  const set = kind === "wanted" ? profileState.wantedSkills : profileState.offeredSkills;
  set.add(value);
  renderTags(containerId, set, kind);
  input.value = "";
}

document.getElementById("uploadImageBtn").onclick = async () => {
  const fileInput = document.getElementById("imageInput");
  if (!fileInput.files.length) return;
  const fd = new FormData();
  fd.append("file", fileInput.files[0]);
  try {
    await api("/users/me/image", { method: "POST", body: fd });
    loadProfile();
  } catch (err) { alert(err.message); }
};
