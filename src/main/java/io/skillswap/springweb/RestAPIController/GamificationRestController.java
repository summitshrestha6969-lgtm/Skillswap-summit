package io.skillswap.springweb.RestAPIController;

import io.skillswap.springweb.Model.User;
import io.skillswap.springweb.Repository.UserRepository;
import io.skillswap.springweb.Util.GamificationUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class GamificationRestController {

    private final UserRepository uRepo;
    private final GamificationUtil gamificationUtil;

    @GetMapping("/api/gamification/me")
    public ResponseEntity<?> me(HttpServletRequest request) {
        User me = currentUser(request);
        if (me == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Not authenticated."));

        GamificationUtil.LevelInfo level = gamificationUtil.computeLevel(me.getPoints());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("points", me.getPoints());
        result.put("level", level.level());
        result.put("title", level.title());
        result.put("icon", level.icon());
        result.put("nextTitle", level.nextTitle());
        result.put("nextAt", level.nextAt());
        result.put("progressPct", level.progressPct());
        result.put("streakDays", me.getStreakDays());
        result.put("badges", gamificationUtil.allBadgesFor(me));
        return ResponseEntity.ok(result);
    }

    @GetMapping("/api/leaderboard")
    public List<Map<String, Object>> leaderboard() {
        List<User> ranked = uRepo.findAll().stream()
                .filter(u -> u.isActive() && !"ROLE_ADMIN".equals(u.getRole()))
                .sorted(Comparator.comparingInt(User::getPoints).reversed())
                .limit(20)
                .toList();

        List<Map<String, Object>> result = new java.util.ArrayList<>();
        int rank = 1;
        for (User u : ranked) {
            GamificationUtil.LevelInfo level = gamificationUtil.computeLevel(u.getPoints());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("rank", rank++);
            m.put("id", u.getId());
            m.put("fullName", u.getFullName());
            m.put("points", u.getPoints());
            m.put("level", level.level());
            m.put("title", level.title());
            m.put("badgeCount", u.getBadges().size());
            result.add(m);
        }
        return result;
    }

    private User currentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) return null;
        Object id = session.getAttribute("userId");
        if (id == null) return null;
        return uRepo.findById((Long) id).orElse(null);
    }
}
