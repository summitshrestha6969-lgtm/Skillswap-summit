package io.skillswap.springweb.RestAPIController;

import io.skillswap.springweb.Model.Match;
import io.skillswap.springweb.Model.Rating;
import io.skillswap.springweb.Model.Session;
import io.skillswap.springweb.Model.User;
import io.skillswap.springweb.Repository.RatingRepository;
import io.skillswap.springweb.Repository.SessionRepository;
import io.skillswap.springweb.Repository.UserRepository;
import io.skillswap.springweb.Util.GamificationUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sessions/{sessionId}/ratings")
public class RatingRestController {

    private final RatingRepository ratingRepo;
    private final SessionRepository sessionRepo;
    private final UserRepository uRepo;
    private final GamificationUtil gamificationUtil;

    @PostMapping
    public ResponseEntity<?> submit(HttpServletRequest request, @PathVariable Long sessionId, @RequestBody Map<String, Object> body) {
        User me = currentUser(request);
        if (me == null) return unauthorized();

        Session session = sessionRepo.findById(sessionId).orElse(null);
        if (session == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Session not found."));

        Match match = session.getMatch();
        if (!match.involves(me)) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "This isn't your session."));
        User other = match.otherUser(me);

        Object scoreRaw = body.get("score");
        if (scoreRaw == null) return ResponseEntity.badRequest().body(Map.of("message", "Score is required."));
        int score = ((Number) scoreRaw).intValue();
        if (score < 1 || score > 5) return ResponseEntity.badRequest().body(Map.of("message", "Score must be between 1 and 5."));
        String comment = (String) body.get("comment");

        other.setRatingSum(other.getRatingSum() + score);
        other.setRatingCount(other.getRatingCount() + 1);

        Rating rating = new Rating();
        rating.setSession(session);
        rating.setRater(me);
        rating.setRatedUser(other);
        rating.setScore(score);
        rating.setComment(comment);
        ratingRepo.save(rating);

        gamificationUtil.awardPoints(me, GamificationUtil.RATING_GIVEN);
        gamificationUtil.checkAndAwardBadges(me);
        if (score == 5) gamificationUtil.awardPoints(other, GamificationUtil.RATING_RECEIVED_5STAR);
        gamificationUtil.checkAndAwardBadges(other);

        uRepo.save(me);
        uRepo.save(other);

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
