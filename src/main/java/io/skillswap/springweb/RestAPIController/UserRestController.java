package io.skillswap.springweb.RestAPIController;

import io.skillswap.springweb.Model.Skill;
import io.skillswap.springweb.Model.User;
import io.skillswap.springweb.Model.UserImage;
import io.skillswap.springweb.Repository.SkillRepository;
import io.skillswap.springweb.Repository.UserImageRepository;
import io.skillswap.springweb.Repository.UserRepository;
import io.skillswap.springweb.Util.GamificationUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserRestController {

    private final UserRepository uRepo;
    private final UserImageRepository imageRepo;
    private final SkillRepository skillRepo;
    private final GamificationUtil gamificationUtil;

    @GetMapping("/me")
    public ResponseEntity<?> me(HttpServletRequest request) {
        User me = currentUser(request);
        if (me == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Not authenticated."));
        return ResponseEntity.ok(gamificationUtil.userView(me));
    }

    @GetMapping("/browse")
    public List<Map<String, Object>> browse(HttpServletRequest request,
                                            @RequestParam(required = false) String reviewsBucket,
                                            @RequestParam(required = false) Integer minEnrollments,
                                            @RequestParam(required = false, defaultValue = "false") boolean hasFeedback) {
        User me = currentUser(request);
        return uRepo.findAll().stream()
                .filter(u -> u.isActive() && u.isEmailVerified())
                .filter(u -> me == null || !u.getId().equals(me.getId()))
                .filter(u -> matchesBucket(u, reviewsBucket))
                .filter(u -> minEnrollments == null || u.getTotalEnrollments() >= minEnrollments)
                .filter(u -> !hasFeedback || u.getRatingCount() > 0)
                .map(gamificationUtil::userView)
                .toList();
    }

    private boolean matchesBucket(User u, String bucket) {
        if (bucket == null || bucket.isBlank()) return true;
        return switch (bucket) {
            case "under10" -> u.getRatingCount() < 10;
            case "under100" -> u.getRatingCount() < 100;
            case "under1000" -> u.getRatingCount() < 1000;
            default -> true;
        };
    }

    @PutMapping("/me/skills")
    public ResponseEntity<?> updateSkills(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        User me = currentUser(request);
        if (me == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Not authenticated."));

        boolean hadSkillsBefore = !me.getOfferedSkills().isEmpty() && !me.getWantedSkills().isEmpty();

        me.setOfferedSkills(resolveSkills(asStringSet(body.get("offeredSkills"))));
        me.setWantedSkills(resolveSkills(asStringSet(body.get("wantedSkills"))));

        if (body.containsKey("bio")) {
            Object bioRaw = body.get("bio");
            String bio = bioRaw == null ? null : bioRaw.toString().trim();
            me.setBio((bio == null || bio.isEmpty()) ? null : bio);
        }

        boolean hasSkillsNow = !me.getOfferedSkills().isEmpty() && !me.getWantedSkills().isEmpty();
        if (!hadSkillsBefore && hasSkillsNow && !me.isAwardedProfileCompleteBonus()) {
            gamificationUtil.awardPoints(me, GamificationUtil.COMPLETE_PROFILE);
            me.setAwardedProfileCompleteBonus(true);
        }
        gamificationUtil.checkAndAwardBadges(me);
        uRepo.save(me);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @SuppressWarnings("unchecked")
    private Set<String> asStringSet(Object raw) {
        if (!(raw instanceof List<?> list)) return Set.of();
        Set<String> result = new HashSet<>();
        for (Object o : list) if (o != null) result.add(o.toString());
        return result;
    }

    private Set<Skill> resolveSkills(Set<String> names) {
        Set<Skill> result = new HashSet<>();
        for (String name : names) {
            String trimmed = name.trim();
            if (trimmed.isEmpty()) continue;
            Skill skill = skillRepo.findByNameIgnoreCase(trimmed).orElseGet(() -> skillRepo.save(new Skill(trimmed)));
            result.add(skill);
        }
        return result;
    }

    @PostMapping("/me/image")
    public ResponseEntity<?> uploadImage(HttpServletRequest request, @RequestParam("file") MultipartFile file) {
        User me = currentUser(request);
        if (me == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Not authenticated."));

        try {
            String base64 = Base64.getEncoder().encodeToString(file.getBytes());
            UserImage image = imageRepo.findByUserId(me.getId()).orElse(new UserImage());
            image.setUser(me);
            image.setImage(base64);
            image.setContentType(file.getContentType());
            imageRepo.save(image);

            gamificationUtil.checkAndAwardBadges(me);
            uRepo.save(me);
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOne(@PathVariable Long id) {
        return uRepo.findById(id)
                .<ResponseEntity<?>>map(u -> ResponseEntity.ok(gamificationUtil.userView(u)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "User not found.")));
    }

    @GetMapping("/{id}/image")
    public ResponseEntity<byte[]> getImage(@PathVariable Long id) {
        Optional<UserImage> image = imageRepo.findByUserId(id);
        if (image.isEmpty()) return ResponseEntity.notFound().build();
        byte[] bytes = Base64.getDecoder().decode(image.get().getImage());
        MediaType type = image.get().getContentType() != null
                ? MediaType.parseMediaType(image.get().getContentType())
                : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok().contentType(type).body(bytes);
    }

    private User currentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) return null;
        Object id = session.getAttribute("userId");
        if (id == null) return null;
        return uRepo.findById((Long) id).orElse(null);
    }
}