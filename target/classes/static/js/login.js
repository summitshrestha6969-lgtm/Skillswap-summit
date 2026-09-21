renderNav("home");

if (state.user) window.location.href = "browse.html";

document.getElementById("loginForm").onsubmit = async (e) => {
  e.preventDefault();
  const errBox = document.getElementById("loginError");
  errBox.classList.add("hidden");
  try {
    const data = await api("/auth/login", {
      method: "POST",
      body: JSON.stringify({ email: document.getElementById("loginEmail").value, password: document.getElementById("loginPassword").value }),
    });
    persistSession(data);
    window.location.href = "browse.html";
  } catch (err) {
    errBox.textContent = err.message;
    errBox.classList.remove("hidden");
  }
};
