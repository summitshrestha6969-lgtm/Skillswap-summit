package io.skillswap.springweb.RestAPIController;

import io.skillswap.springweb.Model.MatchStatus;
import io.skillswap.springweb.Model.Notification;
import io.skillswap.springweb.Model.User;
import io.skillswap.springweb.Repository.*;
import io.skillswap.springweb.Util.GamificationUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminRestController {

    private final UserRepository uRepo;
    private final MatchRepository matchRepo;
    private final SessionRepository sessionRepo;
    private final RedemptionRepository redemptionRepo;
    private final NotificationRepository notificationRepo;
    private final GamificationUtil gamificationUtil;
    private final JavaMailSender jms;

    @GetMapping("/dashboard")
    public ResponseEntity<?> dashboard(HttpServletRequest request) {
        ResponseEntity<?> guard = requireAdmin(request);
        if (guard != null) return guard;

        List<User> users = uRepo.findAll();
        long totalPointsHeld = users.stream().mapToLong(User::getPoints).sum();
        long totalPointsRedeemed = redemptionRepo.findAll().stream().mapToLong(r -> r.getCost()).sum();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("Total users", users.size());
        stats.put("Verified users", users.stream().filter(User::isEmailVerified).count());
        stats.put("Accepted matches", matchRepo.countByStatus(MatchStatus.ACCEPTED));
        stats.put("Pending matches", matchRepo.countByStatus(MatchStatus.PENDING));
        stats.put("Sessions scheduled", sessionRepo.count());
        stats.put("Ratings submitted", users.stream().mapToInt(User::getRatingCount).sum());
        stats.put("Total points awarded", totalPointsHeld + totalPointsRedeemed);
        stats.put("Rewards redeemed", redemptionRepo.count());
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/users")
    public ResponseEntity<?> users(HttpServletRequest request) {
        ResponseEntity<?> guard = requireAdmin(request);
        if (guard != null) return guard;

        List<Map<String, Object>> result = uRepo.findAll().stream()
                .map(u -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", u.getId());
                    m.put("fullName", u.getFullName());
                    m.put("email", u.getEmail());
                    m.put("emailVerified", u.isEmailVerified());
                    m.put("ratingCount", u.getRatingCount());
                    m.put("totalEnrollments", u.getTotalEnrollments());
                    m.put("active", u.isActive());
                    m.put("points", u.getPoints());
                    m.put("level", gamificationUtil.computeLevel(u.getPoints()).level());
                    m.put("role", u.getRole());
                    return m;
                })
                .toList();
        return ResponseEntity.ok(result);
    }

    @PatchMapping("/users/{userId}/deactivate")
    public ResponseEntity<?> deactivate(HttpServletRequest request, @PathVariable Long userId) {
        ResponseEntity<?> guard = requireAdmin(request);
        if (guard != null) return guard;

        User user = uRepo.findById(userId).orElse(null);
        if (user == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "User not found."));
        if ("ROLE_ADMIN".equals(user.getRole())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Can't deactivate an admin account."));
        }
        user.setActive(false);
        uRepo.save(user);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @GetMapping("/notifications")
    public ResponseEntity<?> notifications(HttpServletRequest request) {
        ResponseEntity<?> guard = requireAdmin(request);
        if (guard != null) return guard;
        List<Notification> notifications = notificationRepo.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 50));
        return ResponseEntity.ok(notifications);
    }

    @PostMapping("/announcements")
    public ResponseEntity<?> announce(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        ResponseEntity<?> guard = requireAdmin(request);
        if (guard != null) return guard;

        String type = (String) body.get("type");
        String title = (String) body.get("title");
        String text = (String) body.get("body");
        if (title == null || title.isBlank() || text == null || text.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Title and body are required."));
        }

        for (User user : uRepo.findAll()) {
            if (!user.isActive()) continue;
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(user.getEmail());
                message.setSubject("[SkillSwap] " + title);
                message.setText(text);
                jms.send(message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        notificationRepo.save(new Notification("\uD83D\uDCE2 [" + type + "] " + title + ": " + text));
        return ResponseEntity.ok(Map.of("ok", true));
    }

    private ResponseEntity<?> requireAdmin(HttpServletRequest request) {
        User me = currentUser(request);
        if (me == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Not authenticated."));
        if (!"ROLE_ADMIN".equals(me.getRole())) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Admin access only."));
        return null;
    }

    private User currentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) return null;
        Object id = session.getAttribute("userId");
        if (id == null) return null;
        return uRepo.findById((Long) id).orElse(null);
    }
}
