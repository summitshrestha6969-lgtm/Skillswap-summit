renderNav("home");

document.getElementById("heroGetStarted").onclick = () => window.location.href = state.user ? "browse.html" : "register.html";
document.getElementById("ctaBandBtn").onclick = () => window.location.href = state.user ? "browse.html" : "register.html";
document.getElementById("heroLeaderboardBtn").onclick = () => window.location.href = "leaderboard.html";
document.getElementById("heroSearchBtn").onclick = () => {
  const q = document.getElementById("heroSearch").value;
  window.location.href = "browse.html?q=" + encodeURIComponent(q);
};
document.getElementById("heroSearch").addEventListener("keydown", e => { if (e.key === "Enter") document.getElementById("heroSearchBtn").click(); });

const TESTIMONIALS = [
  { quote: "I taught Excel to a stranger and walked away knowing how to play the guitar. Fair trade doesn't get more literal than that.", who: "Priya G.", role: "learned Guitar, taught Excel" },
  { quote: "No subscription, no paywall — just a photographer and a chef swapping what they know. This is what learning should feel like.", who: "Ben C.", role: "learned Nepali, taught Photography" },
  { quote: "I've taken paid courses that taught me less than one video call with someone who actually cared whether I understood.", who: "Mira T.", role: "learned Public Speaking, taught Baking" },
  { quote: "The matching actually works, and the points/badges kept me coming back to finish what I started.", who: "Aarav S.", role: "learned Python, taught Guitar" },
];
let testimonialIndex = 0;
let testimonialTimer = null;

function renderTestimonials() {
  const track = document.getElementById("testimonialTrack");
  const dots = document.getElementById("testimonialDots");
  track.innerHTML = TESTIMONIALS.map((t, i) => `
    <div class="testimonial-slide ${i === 0 ? "active" : ""}" data-i="${i}">
      <p class="quote">"${escapeHtml(t.quote)}"</p>
      <p class="who">${escapeHtml(t.who)} <span>· ${escapeHtml(t.role)}</span></p>
    </div>`).join("");
  dots.innerHTML = TESTIMONIALS.map((_, i) => `<button data-i="${i}" class="${i === 0 ? "active" : ""}"></button>`).join("");
  dots.querySelectorAll("button").forEach(btn => btn.onclick = () => goToTestimonial(Number(btn.dataset.i)));
  if (testimonialTimer) clearInterval(testimonialTimer);
  testimonialTimer = setInterval(() => goToTestimonial((testimonialIndex + 1) % TESTIMONIALS.length), 5000);
}
function goToTestimonial(i) {
  testimonialIndex = i;
  document.querySelectorAll(".testimonial-slide").forEach(s => s.classList.toggle("active", Number(s.dataset.i) === i));
  document.querySelectorAll(".testimonial-dots button").forEach(b => b.classList.toggle("active", Number(b.dataset.i) === i));
}

async function loadHome() {
  renderTestimonials();
  try {
    const data = await api("/public/featured");
    document.getElementById("statsRow").innerHTML = `
      <div class="stat-item"><div class="num">${data.stats.totalUsers ?? "-"}</div><div class="label">community members</div></div>
      <div class="stat-item"><div class="num">${data.stats.totalSwaps ?? "-"}</div><div class="label">skill swaps made</div></div>
      <div class="stat-item"><div class="num">${data.stats.totalRatings ?? "-"}</div><div class="label">session reviews</div></div>`;

    document.getElementById("categoryCarousel").innerHTML = data.categories.map(c =>
      `<div class="carousel-item"><button class="category-chip" data-cat="${escapeHtml(c.name)}">${escapeHtml(c.name)}</button></div>`
    ).join("") || `<p class="muted">No categories yet.</p>`;
    document.querySelectorAll(".category-chip").forEach(btn => {
      btn.onclick = () => window.location.href = "browse.html?q=" + encodeURIComponent(btn.dataset.cat);
    });

    document.getElementById("teacherCarousel").innerHTML = data.teachers.map(t => `
      <div class="carousel-item teacher-card">
        <img class="avatar" src="${avatarUrl(t)}" alt="">
        <h4>${escapeHtml(t.fullName)}</h4>
        <p style="font-size:0.78rem;color:var(--muted);">${escapeHtml(t.city || "")} · <span class="star-rating">★ ${t.avgRating.toFixed(1)}</span> (${t.ratingCount}) · Lv.${t.level}</p>
        <div>${(t.offeredSkills || []).slice(0, 3).map(s => `<span class="pill">${escapeHtml(s)}</span>`).join("")}</div>
        ${t.totalEnrollments > 0 ? `<p style="font-size:0.75rem;color:var(--muted);margin-top:6px;">${t.totalEnrollments} students taught</p>` : ""}
      </div>`).join("") || `<p class="muted">No rated members yet — be the first to complete a session!</p>`;
  } catch (err) {
    console.error(err);
  }
}

loadHome();
