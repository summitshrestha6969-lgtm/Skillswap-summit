# SkillSwap - Spring Boot backend + real frontend (defense-ready)

This is your project, fixed and wired together end-to-end: **one running app**
(same pattern as your friend's SilaiBook) that serves both the REST API and
the HTML/CSS/JS frontend, backed by a real database.

## What was actually broken (and is now fixed)

Your frontend (`SkillSwap_projectttt.zip`) was still running on its **old
fake in-browser database** - `js/backend.js` was a mock that faked every API
call using `localStorage`, completely disconnected from your real Spring Boot
backend. That's why:
- **Registration "worked" but nothing was real** - it just wrote a fake user into your browser's local storage, never touched MySQL.
- **Skills wouldn't save** - same reason; whatever you saved lived only in that browser tab's local storage, not the database, and the shape of the fake data didn't match your real backend's API at all.
- **No email ever sent** - the mock never called an email service; it doesn't exist in a browser.

### What I changed

1. **`js/backend.js`** - completely rewritten. It's no longer a fake
   database; it's now a thin real API client that calls your actual backend
   with `fetch()`.
2. **`js/common.js`** - switched from a fake "token" concept to real
   session-cookie auth (what your backend actually uses), and fixed profile
   photo URLs to point at the real image endpoint instead of expecting
   embedded fake image data.
3. **Backend (`GamificationUtil`, `UserRestController`, `PublicRestController`,
   `RewardRestController`)** - these were returning raw database entities
   whose JSON shape didn't match what the frontend expected (e.g. skills as
   `{id, name}` objects instead of plain strings, no `level`/`hasImage`
   fields). Added a `userView()` helper that shapes every user response
   exactly the way the frontend needs it.
4. **Frontend is now served BY the backend** - moved into
   `src/main/resources/static/`, exactly like SilaiBook does it. One app, one
   port, no CORS/cookie cross-origin headaches.
5. Removed the old, now-redundant Thymeleaf pages/controllers (the real
   frontend replaces them).
6. Added an **H2 fallback profile** as a safety net for tomorrow (see below).

## 1. Open in IntelliJ

`File → Open` this folder (the one with `pom.xml`). Java 17+ as the Project SDK.

## 2. Choose your database

**Option A - MySQL (what your assignment spec requires):**
```sql
CREATE DATABASE skillswap;
```
`application.properties` is already set for `localhost:3306/skillswap`,
user/pass `root`/`root` - edit those two lines if yours differ. Then just run
normally.

**Option B - H2, zero setup (safety net for tomorrow only):**
Run with the `h2` Spring profile active - in IntelliJ's Run Configuration,
set **Active profiles: `h2`** (or run `./mvnw spring-boot:run
-Dspring-boot.run.profiles=h2` from a terminal). No install needed at all.
Use this ONLY if MySQL isn't cooperating right before your defense - switch
back to MySQL for the actual submission since that's what the spec requires.

Either way, `spring.jpa.hibernate.ddl-auto=update` means Hibernate creates
every table automatically the first time it runs.

## 3. ⚠️ SMTP email - you MUST do this for email to actually send

Right now `application.properties` has **placeholder** Gmail credentials:
```properties
spring.mail.username=your_email@gmail.com
spring.mail.password=your_16_char_app_password
```
Emails will silently fail to send until you replace both with a real Gmail
address and an **App Password** (NOT your normal Gmail password):

1. Go to your Google Account → Security → **2-Step Verification** (turn it on if it isn't already).
2. Go to Security → **App passwords**.
3. Create one (name it "SkillSwap"), copy the 16-character code it gives you.
4. Paste your Gmail address into `spring.mail.username` and that 16-character
   code (no spaces) into `spring.mail.password`.

Without this, registration still works fine - the email attempt just fails
silently in the console log instead of crashing anything. But if your
defense requires showing the email arriving, **do this step**.

## 4. Run it

Run `SkillSwapApplication.main()` in IntelliJ. Opens at **http://localhost:9090**.
`DataSeeder` fills in demo users/skills/one sample match automatically the
first time it starts (only if the database is empty).

### Demo accounts (password `password123` unless noted)

| Email | Role |
|---|---|
| `priya@skillswap.com` | user (has existing rating history, matches) |
| `aarav`, `ben`, `mira`, `sita`, `ramesh`, `anjali`, `david`, `suman` `@skillswap.com` | users |
| `admin@skillswap.com` / `admin123` | admin |

## 5. Try it

Go to **http://localhost:9090** - that's the real, working site:
- Register a brand-new account (or use a demo login)
- Go to **Profile**, add skills, upload a photo → click Save → refresh the
  page → your skills are still there (they're in the actual database now)
- **Browse** other people, propose a match, chat, schedule a session, rate
  each other, earn points/badges
- **Leaderboard**, **Rewards**, and (as admin) the **Admin** dashboard all work

## If something still doesn't work

Open your browser's DevTools (F12) → **Console** tab and **Network** tab
while reproducing the problem - the error message and the failed request
will tell you exactly what's wrong. The most likely remaining issues:

- **MySQL not running / wrong credentials** → app won't start at all; check
  the IntelliJ console for a `Communications link failure` error, or just use
  the H2 profile instead.
- **Port 9090 already in use** → change `server.port` in
  `application.properties`.
