# SkillSwap

A community-based, gamified peer-to-peer skill exchange platform — trade what you know for what you want to learn, earn points and badges, climb the leaderboard, and redeem rewards.

## Project structure

```
SkillSwap/
├── index.html              ← redirects to html/index.html
├── README.md
├── html/                    ← all 11 pages
│   ├── index.html            Home / landing (public)
│   ├── login.html            Log in
│   ├── register.html         Sign up
│   ├── browse.html           Browse the community feed (public, personalized when logged in)
│   ├── user.html              Public profile of another member (?id=)
│   ├── matches.html           My matches (list)
│   ├── match.html              Match detail: chat + schedule session + rate (?id=)
│   ├── profile.html            Edit my profile, skills, photo
│   ├── leaderboard.html        🏆 Points, level, badges, top swappers
│   ├── rewards.html            🎁 Points store — redeem perks
│   └── admin.html              Admin dashboard (admin accounts only)
├── css/
│   └── style.css              All styling, including nav, cards, gamification UI
├── js/
│   ├── backend.js              Mock REST backend (auth, matching, messaging, sessions,
│   │                            ratings, gamification, rewards, admin) — persists to localStorage
│   ├── common.js                Shared nav rendering, auth guards, formatting helpers
│   └── <page>.js                One script per page (home.js, login.js, browse.js, ...)
└── assets/                  ← static assets (images/icons), empty for now
```

## How to run

No build step, no server, no dependencies required.

**Option A - VS Code Live Server (recommended)**
1. Open this folder in VS Code.
2. Install the "Live Server" extension if you don't have it.
3. Right-click `html/index.html` → "Open with Live Server".

**Option B - just open the file**
Double-click `html/index.html` (or the root `index.html`) to open it directly in your browser.

## Demo accounts

- **priya@skillswap.com** / **password123** — regular user (existing matches, ratings, points, badges)
- **admin@skillswap.com** / **admin123** — admin dashboard access
- Or register a brand new account (you'll start earning points immediately).

## Gamification

- **Points** are earned for: signing up, daily logins (with streak bonuses), completing your
  profile, getting a match accepted, scheduling a session, attending a session, and giving/
  receiving ratings (a 5★ rating gives the recipient bonus points).
- **Levels**: Newcomer → Skill Swapper → Active Trader → Community Mentor → SkillSwap Legend,
  based on total points. Shown as a progress bar on the Leaderboard page and as a pill in the nav bar.
- **Badges**: 7 badges (Profile Pro, First Swap, Chatterbox, Community Mentor, Five-Star Swapper,
  3-Day Streak, Generous Teacher) unlock automatically as you use the app.
- **Rewards store**: redeem points for perks (verified badge, profile boost, featured profile,
  priority matching, custom theme, extra skill slot).
- **Leaderboard**: ranks all members by total points.

## Notes

- `js/backend.js` simulates a real backend (the API contract mirrors what a Spring/Hibernate REST
  API would return) entirely in the browser using `localStorage`, so the whole app works with zero
  setup. To connect this to a real backend later, only the `api()` function in `js/backend.js`
  needs to be swapped for a real `fetch()` call — no page or page-script needs to change.
- Data (accounts, matches, messages, ratings, points, badges) persists in your browser's
  localStorage. Clear site data / use a different browser profile to reset to the seeded demo state.
