package io.skillswap.springweb.RestAPIController;

import io.skillswap.springweb.Model.Match;
import io.skillswap.springweb.Model.Message;
import io.skillswap.springweb.Model.User;
import io.skillswap.springweb.Repository.MatchRepository;
import io.skillswap.springweb.Repository.MessageRepository;
import io.skillswap.springweb.Repository.UserRepository;
import io.skillswap.springweb.Util.GamificationUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/matches/{matchId}/messages")
public class MessageRestController {

    private final MessageRepository messageRepo;
    private final MatchRepository matchRepo;
    private final UserRepository uRepo;
    private final GamificationUtil gamificationUtil;

    @GetMapping
    public ResponseEntity<?> list(HttpServletRequest request, @PathVariable Long matchId) {
        User me = currentUser(request);
        if (me == null) return unauthorized();
        Match match = matchRepo.findById(matchId).orElse(null);
        if (match == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Match not found."));
        if (!match.involves(me)) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "This isn't your conversation."));

        List<Map<String, Object>> result = messageRepo.findByMatchIdOrderByCreatedAtAsc(matchId).stream()
                .map(msg -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", msg.getId());
                    m.put("content", msg.getContent());
                    m.put("sender", Map.of("id", msg.getSender().getId()));
                    m.put("createdAt", msg.getCreatedAt());
                    return m;
                })
                .toList();
        return ResponseEntity.ok(result);
    }

    @PostMapping
    public ResponseEntity<?> send(HttpServletRequest request, @PathVariable Long matchId, @RequestBody Map<String, Object> body) {
        User me = currentUser(request);
        if (me == null) return unauthorized();
        Match match = matchRepo.findById(matchId).orElse(null);
        if (match == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Match not found."));
        if (!match.involves(me)) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "This isn't your conversation."));

        String content = (String) body.get("content");
        if (content == null || content.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Message can't be empty."));
        }

        Message message = new Message();
        message.setMatch(match);
        message.setSender(me);
        message.setContent(content.trim());
        messageRepo.save(message);

        me.setMessageCount(me.getMessageCount() + 1);
        gamificationUtil.checkAndAwardBadges(me);
        uRepo.save(me);

        return ResponseEntity.ok(Map.of("id", message.getId()));
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
