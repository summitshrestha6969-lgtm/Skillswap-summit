package io.skillswap.springweb.Util;

import io.skillswap.springweb.Model.*;
import io.skillswap.springweb.Repository.BadgeRepository;
import io.skillswap.springweb.Repository.MatchRepository;
import io.skillswap.springweb.Repository.NotificationRepository;
import io.skillswap.springweb.Repository.UserImageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Small shared helper for points/levels/badges so this logic isn't copy-pasted
// across every Controller/RestAPIController that needs it (matching a match,
// scheduling a session, rating someone, etc). Everything else in this
// project talks to Repositories directly - this is the one exception,
// kept as a plain helper component rather than a full service layer.
@Component
public class GamificationUtil {

    @Autowired
    private BadgeRepository badgeRepository;
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private MatchRepository matchRepository;
    @Autowired
    private UserImageRepository userImageRepository;

    // ---- points earned for each action ----
    public static final int REGISTER = 20;
    public static final int DAILY_LOGIN = 5;
    public static final int COMPLETE_PROFILE = 15;
    public static final int MATCH_ACCEPTED = 15;
    public static final int SESSION_SCHEDULED = 10;
    public static final int SESSION_ATTENDED = 25;
    public static final int RATING_GIVEN = 10;
    public static final int RATING_RECEIVED_5STAR = 15;

    private record LevelDef(int min, String title, String icon) {
    }

    private static final List<LevelDef> LEVELS = List.of(
            new LevelDef(0, "Newcomer", "\uD83C\uDF31"),
            new LevelDef(50, "Skill Swapper", "\uD83D\uDD01"),
            new LevelDef(150, "Active Trader", "\u26A1"),
            new LevelDef(300, "Community Mentor", "\uD83C\uDF1F"),
            new LevelDef(600, "SkillSwap Legend", "\uD83C\uDFC6")
    );

    public record LevelInfo(int level, String title, String icon, String nextTitle, Integer nextAt, int progressPct) {
    }

    public LevelInfo computeLevel(int points) {
        LevelDef current = LEVELS.get(0);
        int idx = 0;
        for (int i = 0; i < LEVELS.size(); i++) {
            if (points >= LEVELS.get(i).min()) {
                current = LEVELS.get(i);
                idx = i;
            }
        }
        LevelDef next = idx + 1 < LEVELS.size() ? LEVELS.get(idx + 1) : null;
        int progressPct = next == null
                ? 100
                : (int) Math.round(((points - current.min()) * 100.0) / (next.min() - current.min()));
        return new LevelInfo(idx + 1, current.title(), current.icon(),
                next == null ? null : next.title(), next == null ? null : next.min(), progressPct);
    }

    public void awardPoints(User user, int amount) {
        user.setPoints(user.getPoints() + amount);
    }

    private interface BadgeCheck {
        boolean test(User user, boolean hasImage, boolean hasAcceptedMatch);
    }

    private record BadgeDef(String code, String name, String icon, String description, BadgeCheck check) {
    }

    private static final List<BadgeDef> BADGE_DEFS = List.of(
            new BadgeDef("PROFILE_PRO", "Profile Pro", "\uD83E\uDEAA",
                    "Complete your profile with a photo, bio and skills.",
                    (u, hasImage, hasAccepted) -> hasImage && !u.getOfferedSkills().isEmpty() && !u.getWantedSkills().isEmpty()),
            new BadgeDef("FIRST_SWAP", "First Swap", "\uD83C\uDF89",
                    "Get your first match accepted.",
                    (u, hasImage, hasAccepted) -> hasAccepted),
            new BadgeDef("CHATTERBOX", "Chatterbox", "\uD83D\uDCAC",
                    "Send 10 or more messages.",
                    (u, hasImage, hasAccepted) -> u.getMessageCount() >= 10),
            new BadgeDef("MENTOR", "Community Mentor", "\uD83C\uDF93",
                    "Teach 10 or more students.",
                    (u, hasImage, hasAccepted) -> u.getTotalEnrollments() >= 10),
            new BadgeDef("FIVE_STAR", "Five-Star Swapper", "\u2B50",
                    "Earn a 4.5+ average rating from 5+ reviews.",
                    (u, hasImage, hasAccepted) -> u.getRatingCount() >= 5 && u.getAvgRating() >= 4.5),
            new BadgeDef("STREAK_3", "3-Day Streak", "\uD83D\uDD25",
                    "Log in 3 days in a row.",
                    (u, hasImage, hasAccepted) -> u.getStreakDays() >= 3),
            new BadgeDef("GENEROUS_TEACHER", "Generous Teacher", "\uD83D\uDCDA",
                    "List 3 or more skills you can teach.",
                    (u, hasImage, hasAccepted) -> u.getOfferedSkills().size() >= 3)
    );

    /** Re-checks every badge definition and awards any newly-earned ones. */
    public void checkAndAwardBadges(User user) {
        boolean hasImage = userImageRepository.findByUserId(user.getId()).isPresent();
        boolean hasAcceptedMatch = matchRepository.findAllInvolvingUser(user.getId()).stream()
                .anyMatch(m -> m.getStatus() == MatchStatus.ACCEPTED);

        for (BadgeDef def : BADGE_DEFS) {
            boolean alreadyHasIt = user.getBadges().stream().anyMatch(b -> b.getCode().equals(def.code()));
            if (!alreadyHasIt && def.check().test(user, hasImage, hasAcceptedMatch)) {
                Badge badge = badgeRepository.findByCode(def.code())
                        .orElseGet(() -> badgeRepository.save(new Badge(def.code(), def.name(), def.icon(), def.description())));
                user.getBadges().add(badge);
                notificationRepository.save(new Notification(
                        user.getFullName() + " earned the \"" + def.name() + "\" badge " + def.icon()));
            }
        }
    }

    public record BadgeInfo(String id, String name, String icon, String description, boolean earned) {
    }

    /** All badge definitions with an "earned" flag for the given user. */
    public List<BadgeInfo> allBadgesFor(User user) {
        return BADGE_DEFS.stream()
                .map(def -> new BadgeInfo(def.code(), def.name(), def.icon(), def.description(),
                        user.getBadges().stream().anyMatch(b -> b.getCode().equals(def.code()))))
                .toList();
    }

    // Shapes a User entity into exactly the flat JSON object the frontend
    // expects (hasImage/offeredSkills-as-strings/level/levelTitle/badgeCount),
    // instead of the frontend needing to know about our JPA entity graph.
    // Used by every endpoint that returns one or more users.
    public Map<String, Object> userView(User u) {
        boolean hasImage = userImageRepository.findByUserId(u.getId()).isPresent();
        LevelInfo level = computeLevel(u.getPoints());

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", u.getId());
        m.put("fullName", u.getFullName());
        m.put("city", u.getCity());
        m.put("bio", u.getBio());
        m.put("hasImage", hasImage);
        m.put("offeredSkills", u.getOfferedSkills().stream().map(Skill::getName).sorted().toList());
        m.put("wantedSkills", u.getWantedSkills().stream().map(Skill::getName).sorted().toList());
        m.put("avgRating", u.getAvgRating());
        m.put("ratingCount", u.getRatingCount());
        m.put("totalEnrollments", u.getTotalEnrollments());
        m.put("emailVerified", u.isEmailVerified());
        m.put("role", u.getRole());
        m.put("points", u.getPoints());
        m.put("level", level.level());
        m.put("levelTitle", level.title());
        m.put("badgeCount", u.getBadges().size());
        return m;
    }
}
