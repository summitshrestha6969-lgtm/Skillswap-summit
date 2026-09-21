package io.skillswap.springweb.RestAPIController;

import io.skillswap.springweb.Model.MatchStatus;
import io.skillswap.springweb.Model.User;
import io.skillswap.springweb.Repository.MatchRepository;
import io.skillswap.springweb.Repository.UserRepository;
import io.skillswap.springweb.Util.GamificationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public")
public class PublicRestController {

    private final UserRepository uRepo;
    private final MatchRepository matchRepo;
    private final GamificationUtil gamificationUtil;

    private static final List<String> CATEGORIES =
            List.of("Guitar", "Python", "Cooking", "Photography", "Spanish", "Yoga", "Chess", "Excel");

    @GetMapping("/featured")
    public Map<String, Object> featured() {
        List<User> allUsers = uRepo.findAll();

        List<Map<String, Object>> teachers = allUsers.stream()
                .filter(u -> u.isActive() && u.getRatingCount() > 0)
                .sorted(Comparator.comparingDouble(User::getAvgRating).reversed()
                        .thenComparing(Comparator.comparingInt(User::getRatingCount).reversed()))
                .limit(6)
                .map(gamificationUtil::userView)
                .toList();

        long totalUsers = allUsers.stream().filter(User::isActive).count();
        long totalSwaps = matchRepo.countByStatus(MatchStatus.ACCEPTED);
        long totalRatings = allUsers.stream().mapToInt(User::getRatingCount).sum();

        return Map.of(
                "stats", Map.of("totalUsers", totalUsers, "totalSwaps", totalSwaps, "totalRatings", totalRatings),
                "categories", CATEGORIES,
                "teachers", teachers
        );
    }
}

