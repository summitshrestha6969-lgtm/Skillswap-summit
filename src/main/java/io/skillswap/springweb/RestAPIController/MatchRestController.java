package io.skillswap.springweb.RestAPIController;

import io.skillswap.springweb.Model.Match;
import io.skillswap.springweb.Model.MatchStatus;
import io.skillswap.springweb.Model.Skill;
import io.skillswap.springweb.Model.User;
import io.skillswap.springweb.Repository.MatchRepository;
import io.skillswap.springweb.Repository.UserRepository;
import io.skillswap.springweb.Util.GamificationUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/matches")
public class MatchRestController {

    private final MatchRepository matchRepo;
    private final UserRepository uRepo;
    private final GamificationUtil gamificationUtil;

    /** Same idea used across the SkillSwap frontend: bidirectional skill overlap, normalized 0..1. */
    private double computeMatchScore(User a, User b) {
        Set<String> offeredA = lowerNames(a.getOfferedSkills());
        Set<String> wantedA = lowerNames(a.getWantedSkills());
        Set<String> offeredB = lowerNames(b.getOfferedSkills());
        Set<String> wantedB = lowerNames(b.getWantedSkills());

        long aTeachesB = offeredA.stream().filter(wantedB::contains).count();
        long bTeachesA = offeredB.stream().filter(wantedA::contains).count();
        int denominator = Math.max(1, wantedA.size() + wantedB.size());
        return Math.min(1.0, (aTeachesB + bTeachesA) / (double) denominator);
    }

    private Set<String> lowerNames(Set<Skill> skills) {
        return skills.stream().map(s -> s.getName().toLowerCase()).collect(Collectors.toSet());
    }

    @GetMapping("/suggestions")
    public ResponseEntity<?> suggestions(HttpServletRequest request) {
        User me = currentUser(request);
        if (me == null) return unauthorized();

        Set<Long> linkedIds = matchRepo.findAllInvolvingUser(me.getId()).stream()
                .filter(m -> m.getStatus() != MatchStatus.REJECTED)
                .map(m -> m.otherUser(me).getId())
                .collect(Collectors.toSet());

        List<Map<String, Object>> result = uRepo.findAll().stream()
                .filter(u -> u.isActive() && u.isEmailVerified() && !u.getId().equals(me.getId()) && !linkedIds.contains(u.getId()))
                .map(u -> new AbstractMap.SimpleEntry<>(u, computeMatchScore(me, u)))
                .filter(e -> e.getValue() > 0)
                .sorted((x, y) -> Double.compare(y.getValue(), x.getValue()))
                .limit(8)
                .map(e -> {
                    User u = e.getKey();
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("otherUserId", u.getId());
                    m.put("otherUserName", u.getFullName());
                    m.put("matchScore", e.getValue());
                    return m;
                })
                .toList();

        return ResponseEntity.ok(result);
    }

    @GetMapping
    public ResponseEntity<?> list(HttpServletRequest request) {
        User me = currentUser(request);
        if (me == null) return unauthorized();

        List<Map<String, Object>> result = matchRepo.findAllInvolvingUser(me.getId()).stream()
                .map(match -> {
                    User other = match.otherUser(me);
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", match.getId());
                    m.put("otherUserId", other.getId());
                    m.put("otherUserName", other.getFullName());
                    m.put("status", match.getStatus().name());
                    m.put("matchScore", match.getMatchScore());
                    m.put("expiresAt", match.getExpiresAt());
                    return m;
                })
                .toList();
        return ResponseEntity.ok(result);
    }

    @PostMapping("/propose/{otherUserId}")
    public ResponseEntity<?> propose(HttpServletRequest request, @PathVariable Long otherUserId) {
        User me = currentUser(request);
        if (me == null) return unauthorized();
        if (otherUserId.equals(me.getId())) {
            return ResponseEntity.badRequest().body(Map.of("message", "You can't propose a match with yourself."));
        }
        User other = uRepo.findById(otherUserId).orElse(null);
        if (other == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "User not found."));

        if (matchRepo.findActiveBetween(me.getId(), otherUserId).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("message", "An active match already exists with this user."));
        }

        double score = computeMatchScore(me, other);
        if (score <= 0) {
            return ResponseEntity.badRequest().body(Map.of("message",
                    "No bidirectional skill overlap found with this person yet - try adding more skills to your profile."));
        }

        Match match = new Match();
        match.setUserA(me);
        match.setUserB(other);
        match.setMatchScore(score);
        match.setExpiresAt(LocalDateTime.now().plusDays(7));
        matchRepo.save(match);

        return ResponseEntity.ok(Map.of("id", match.getId()));
    }

    @PatchMapping("/{matchId}/respond")
    public ResponseEntity<?> respond(HttpServletRequest request, @PathVariable Long matchId, @RequestBody Map<String, Object> body) {
        User me = currentUser(request);
        if (me == null) return unauthorized();

        Match match = matchRepo.findById(matchId).orElse(null);
        if (match == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Match not found."));
        if (!match.involves(me)) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "This isn't your match."));

        boolean accept = Boolean.TRUE.equals(body.get("accept"));
        match.setStatus(accept ? MatchStatus.ACCEPTED : MatchStatus.REJECTED);
        matchRepo.save(match);

        if (accept) {
            User other = match.otherUser(me);
            gamificationUtil.awardPoints(me, GamificationUtil.MATCH_ACCEPTED);
            gamificationUtil.checkAndAwardBadges(me);
            gamificationUtil.checkAndAwardBadges(other);
            uRepo.save(me);
            uRepo.save(other);
        }

        return ResponseEntity.ok(Map.of("ok", true));
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
