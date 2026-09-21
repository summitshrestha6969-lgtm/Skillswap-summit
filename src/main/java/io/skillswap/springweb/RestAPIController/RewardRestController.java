package io.skillswap.springweb.RestAPIController;

import io.skillswap.springweb.Model.Redemption;
import io.skillswap.springweb.Model.User;
import io.skillswap.springweb.Repository.RedemptionRepository;
import io.skillswap.springweb.Repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rewards")
public class RewardRestController {

    private final RedemptionRepository redemptionRepo;
    private final UserRepository uRepo;

    private record RewardDef(String code, String name, String icon, int cost, String description) {
    }

    private static final List<RewardDef> CATALOG = List.of(
            new RewardDef("EXTRA_SKILL_SLOT", "Extra Skill Slot", "\u2795", 50,
                    "A small perk to mark you as a power user for going deep on skills."),
            new RewardDef("VERIFIED_BADGE", "Verified Badge", "\u2705", 100,
                    "A verified checkmark next to your name to build extra trust with new matches."),
            new RewardDef("PROFILE_BOOST", "Profile Boost", "\uD83D\uDE80", 80,
                    "Get bumped into more people's Suggested-for-you strip for a week."),
            new RewardDef("FEATURED_7D", "Featured Profile (7 days)", "\uD83D\uDCCC", 150,
                    "Pinned to the top-rated members carousel on the homepage for a week."),
            new RewardDef("PRIORITY_MATCHING", "Priority Matching (30 days)", "\uD83C\uDFAF", 200,
                    "Your proposed matches get surfaced first in the other person's Matches tab."),
            new RewardDef("CUSTOM_THEME", "Custom Profile Theme", "\uD83C\uDFA8", 250,
                    "Unlock a custom accent color for your public profile page.")
    );

    @GetMapping("/catalog")
    public List<Map<String, Object>> catalog(HttpServletRequest request) {
        User me = currentUser(request);
        return CATALOG.stream()
                .map(r -> Map.<String, Object>of(
                        "id", r.code(), "name", r.name(), "icon", r.icon(), "cost", r.cost(),
                        "description", r.description(), "canAfford", me != null && me.getPoints() >= r.cost()))
                .toList();
    }

    @GetMapping("/me")
    public ResponseEntity<?> myRedemptions(HttpServletRequest request) {
        User me = currentUser(request);
        if (me == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Not authenticated."));
        List<Map<String, Object>> redemptions = redemptionRepo.findByUserIdOrderByRedeemedAtDesc(me.getId()).stream()
                .map(r -> Map.<String, Object>of(
                        "rewardId", r.getRewardCode(),
                        "name", r.getRewardName(),
                        "cost", r.getCost(),
                        "date", r.getRedeemedAt().toString()))
                .toList();
        return ResponseEntity.ok(redemptions);
    }

    @PostMapping("/redeem/{rewardCode}")
    public ResponseEntity<?> redeem(HttpServletRequest request, @PathVariable String rewardCode) {
        User me = currentUser(request);
        if (me == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Not authenticated."));

        RewardDef reward = CATALOG.stream().filter(r -> r.code().equals(rewardCode)).findFirst().orElse(null);
        if (reward == null) return ResponseEntity.badRequest().body(Map.of("message", "Reward not found."));
        if (me.getPoints() < reward.cost()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Not enough points for this reward yet."));
        }

        me.setPoints(me.getPoints() - reward.cost());
        uRepo.save(me);

        Redemption redemption = new Redemption();
        redemption.setUser(me);
        redemption.setRewardCode(reward.code());
        redemption.setRewardName(reward.name());
        redemption.setCost(reward.cost());
        redemptionRepo.save(redemption);

        return ResponseEntity.ok(Map.of("ok", true, "remainingPoints", me.getPoints()));
    }

    private User currentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) return null;
        Object id = session.getAttribute("userId");
        if (id == null) return null;
        return uRepo.findById((Long) id).orElse(null);
    }
}
