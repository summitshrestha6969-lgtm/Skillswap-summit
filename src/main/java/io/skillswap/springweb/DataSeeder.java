package io.skillswap.springweb;

import io.skillswap.springweb.Model.*;
import io.skillswap.springweb.Repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

// Runs once on startup (only if the database is empty) so the project is
// immediately demoable, same idea as the mock frontend's seeded accounts.
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository uRepo;
    private final SkillRepository skillRepo;
    private final MatchRepository matchRepo;
    private final MessageRepository messageRepo;

    @Override
    @Transactional
    public void run(String... args) {
        if (uRepo.count() > 0) return; // already seeded

        User admin = newUser("Admin User", "admin@skillswap.com", "admin123", "Kathmandu",
                "SkillSwap platform administrator.", Set.of(), Set.of(), 0, 0, 0, true);
        admin.setRole("ROLE_ADMIN");
        uRepo.save(admin);

        User aarav = uRepo.save(newUser("Aarav Sharma", "aarav@skillswap.com", "password123", "Kathmandu",
                "Guitarist for 10 years, always up for a jam session. Trying to finally learn Python.",
                Set.of("Guitar", "Photography"), Set.of("Python", "Cooking"), 23, 5, 12, true));

        uRepo.save(newUser("Priya Gurung", "priya@skillswap.com", "password123", "Pokhara",
                "Excel wizard and public speaking coach. Want to finally pick up guitar and yoga.",
                Set.of("Excel", "Public Speaking"), Set.of("Guitar", "Yoga"), 48, 10, 20, true));

        uRepo.save(newUser("Ben Carter", "ben@skillswap.com", "password123", "Kathmandu",
                "Freelance photographer and video editor. Learning Nepali for my move here.",
                Set.of("Photography", "Video Editing"), Set.of("Nepali", "Cooking"), 36, 8, 15, true));

        uRepo.save(newUser("Mira Tamang", "mira@skillswap.com", "password123", "Lalitpur",
                "I run a small bakery. Love teaching cooking and baking, want to get better with numbers and words.",
                Set.of("Baking", "Cooking"), Set.of("Public Speaking", "Excel"), 68, 14, 25, true));

        User sita = uRepo.save(newUser("Sita Rai", "sita@skillswap.com", "password123", "Pokhara",
                "Software engineer. Happy to teach Python or JS in exchange for guitar or photography lessons.",
                Set.of("Python", "JavaScript"), Set.of("Guitar", "Photography"), 42, 9, 18, true));

        uRepo.save(newUser("Ramesh K.C.", "ramesh@skillswap.com", "password123", "Biratnagar",
                "Chess club organizer and yoga instructor. Trying to learn Java and get better with Excel.",
                Set.of("Chess", "Yoga"), Set.of("Java", "Excel"), 26, 6, 9, true));

        uRepo.save(newUser("Anjali Thapa", "anjali@skillswap.com", "password123", "Kathmandu",
                "Bilingual translator (Spanish/French). Would love to finally learn piano and singing.",
                Set.of("Spanish", "French"), Set.of("Piano", "Singing"), 32, 7, 11, true));

        uRepo.save(newUser("David Lee", "david@skillswap.com", "password123", "Pokhara",
                "Backend developer (Java/JS). Moving to Nepal, want to pick up Nepali and Spanish.",
                Set.of("Java", "JavaScript"), Set.of("Nepali", "Spanish"), 20, 5, 7, true));

        uRepo.save(newUser("Suman Basnet", "suman@skillswap.com", "password123", "Chitwan",
                "Just joined - still setting things up.",
                Set.of("Cooking"), Set.of("Excel"), 0, 0, 0, false));

        // Extra skills so the catalog isn't tiny even before any user adds more
        for (String name : new String[]{"Web Development", "Meditation", "Music Theory", "UI/UX Design", "Graphic Design"}) {
            skillRepo.findByNameIgnoreCase(name).orElseGet(() -> skillRepo.save(new Skill(name)));
        }

        // One pre-existing accepted match with a couple of messages so
        // Matches/Chat has something to show immediately.
        Match match = new Match();
        match.setUserA(aarav);
        match.setUserB(sita);
        match.setStatus(MatchStatus.ACCEPTED);
        match.setMatchScore(0.8);
        match.setExpiresAt(LocalDateTime.now().plusDays(7));
        matchRepo.save(match);

        saveMessage(match, aarav, "Hey! Would love to trade guitar lessons for some Python basics.");
        saveMessage(match, sita, "Sounds great, I'm free most evenings this week!");
    }

    private User newUser(String fullName, String email, String rawPassword, String city, String bio,
                          Set<String> offered, Set<String> wanted,
                          double ratingSum, int ratingCount, int totalEnrollments, boolean verified) {
        User u = new User();
        u.setFullName(fullName);
        u.setEmail(email);
        u.setPassword(DigestUtils.md5DigestAsHex(rawPassword.getBytes()));
        u.setCity(city);
        u.setBio(bio);
        u.setOfferedSkills(resolveSkills(offered));
        u.setWantedSkills(resolveSkills(wanted));
        u.setRatingSum(ratingSum);
        u.setRatingCount(ratingCount);
        u.setTotalEnrollments(totalEnrollments);
        u.setEmailVerified(verified);
        u.setAwardedProfileCompleteBonus(true);
        u.setPoints((int) Math.round(ratingCount * 8 + totalEnrollments * 4 + offered.size() * 5));
        u.setStreakDays(Math.min(6, Math.max(1, ratingCount / 2)));
        return u;
    }

    private Set<Skill> resolveSkills(Set<String> names) {
        Set<Skill> result = new HashSet<>();
        for (String name : names) {
            result.add(skillRepo.findByNameIgnoreCase(name).orElseGet(() -> skillRepo.save(new Skill(name))));
        }
        return result;
    }

    private void saveMessage(Match match, User sender, String content) {
        Message m = new Message();
        m.setMatch(match);
        m.setSender(sender);
        m.setContent(content);
        messageRepo.save(m);
    }
}
