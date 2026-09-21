renderNav("home");

if (state.user) window.location.href = "browse.html";

document.getElementById("registerForm").onsubmit = async (e) => {
  e.preventDefault();
  const errBox = document.getElementById("regError");
  errBox.classList.add("hidden");
  try {
    const data = await api("/auth/register", {
      method: "POST",
      body: JSON.stringify({
        fullName: document.getElementById("regName").value,
        email: document.getElementById("regEmail").value,
        password: document.getElementById("regPassword").value,
        city: document.getElementById("regCity").value,
      }),
    });
    persistSession(data);
    window.location.href = "profile.html?welcome=1";
  } catch (err) {
    errBox.textContent = err.message;
    errBox.classList.remove("hidden");
  }
};
