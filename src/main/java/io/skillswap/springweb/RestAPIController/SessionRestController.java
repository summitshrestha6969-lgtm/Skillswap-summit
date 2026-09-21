package io.skillswap.springweb.RestAPIController;

import io.skillswap.springweb.Model.*;
import io.skillswap.springweb.Repository.MatchRepository;
import io.skillswap.springweb.Repository.SessionRepository;
import io.skillswap.springweb.Repository.UserRepository;
import io.skillswap.springweb.Util.GamificationUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/matches/{matchId}/session")
public class SessionRestController {

    private final SessionRepository sessionRepo;
    private final MatchRepository matchRepo;
    private final UserRepository uRepo;
    private final GamificationUtil gamificationUtil;

    @GetMapping
    public ResponseEntity<?> get(HttpServletRequest request, @PathVariable Long matchId) {
        User me = currentUser(request);
        if (me == null) return unauthorized();
        Match match = accessibleMatch(matchId, me);
        if (match == null) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "This isn't your match."));

        Session session = sessionRepo.findByMatchId(matchId).orElse(null);
        if (session == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "No session scheduled yet."));
        return ResponseEntity.ok(session);
    }

    @PostMapping
    public ResponseEntity<?> schedule(HttpServletRequest request, @PathVariable Long matchId, @RequestBody Map<String, Object> body) {
        User me = currentUser(request);
        if (me == null) return unauthorized();
        Match match = matchRepo.findById(matchId).orElse(null);
        if (match == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Match not found."));
        if (!match.involves(me)) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "This isn't your match."));

        boolean isNew = sessionRepo.findByMatchId(matchId).isEmpty();
        Session session = sessionRepo.findByMatchId(matchId).orElse(new Session());
        session.setMatch(match);
        session.setScheduledAt(LocalDateTime.parse((String) body.get("scheduledAt")));
        session.setLocation((String) body.get("location"));
        Object capacityRaw = body.get("capacity");
        session.setCapacity(capacityRaw == null ? 1 : ((Number) capacityRaw).intValue());
        String modeRaw = (String) body.get("deliveryMode");
        session.setDeliveryMode(DeliveryMode.valueOf(modeRaw == null ? "ONLINE" : modeRaw));
        session.setDownloadUrl((String) body.get("downloadUrl"));
        session.setStatus(SessionStatus.SCHEDULED);
        sessionRepo.save(session);

        if (isNew) {
            gamificationUtil.awardPoints(me, GamificationUtil.SESSION_SCHEDULED);
            gamificationUtil.checkAndAwardBadges(me);
            uRepo.save(me);
        }
        return ResponseEntity.ok(session);
    }

    @PostMapping("/{sessionId}/access")
    public ResponseEntity<?> access(HttpServletRequest request, @PathVariable Long matchId,
                                     @PathVariable Long sessionId, @RequestBody Map<String, Object> body) {
        User me = currentUser(request);
        if (me == null) return unauthorized();
        Match match = matchRepo.findById(matchId).orElse(null);
        if (match == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Match not found."));
        if (!match.involves(me)) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "This isn't your match."));

        Session session = sessionRepo.findById(sessionId).orElse(null);
        if (session == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Session not found."));

        User other = match.otherUser(me);
        other.setTotalEnrollments(other.getTotalEnrollments() + 1);

        if (session.getAttendedUsers().stream().noneMatch(u -> u.getId().equals(me.getId()))) {
            session.getAttendedUsers().add(me);
            sessionRepo.save(session);
            gamificationUtil.awardPoints(me, GamificationUtil.SESSION_ATTENDED);
            gamificationUtil.checkAndAwardBadges(me);
        }
        gamificationUtil.checkAndAwardBadges(other);
        uRepo.save(me);
        uRepo.save(other);

        String action = (String) body.get("action");
        String url = "JOIN_VIDEO".equals(action)
                ? "https://meet.jit.si/skillswap-" + match.getId()
                : (session.getDownloadUrl() == null || session.getDownloadUrl().isBlank() ? "#" : session.getDownloadUrl());

        return ResponseEntity.ok(Map.of("url", url));
    }

    private Match accessibleMatch(Long matchId, User me) {
        Match match = matchRepo.findById(matchId).orElse(null);
        if (match == null || !match.involves(me)) return null;
        return match;
    }

    private ResponseEntity<?> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Not authenticated."));
    }

    private User currentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) return null;
        Object id = session.getAttribute("userId");
        if (id == null) return null;
        return uRepo.findById((Long) id).orElse(null);
    }
}
