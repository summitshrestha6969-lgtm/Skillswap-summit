package io.skillswap.springweb.RestAPIController;

import io.skillswap.springweb.Model.User;
import io.skillswap.springweb.Repository.UserRepository;
import io.skillswap.springweb.Util.GamificationUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

// Same idea as SignupLoginController, but returns JSON instead of HTML pages
// for the SkillSwap frontend to consume via fetch(). Auth is session-based
// (JSESSIONID cookie), same mechanism the Thymeleaf pages use - Postman and
// browsers both handle this automatically as long as cookies are enabled.
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthRestController {

    private final UserRepository uRepo;
    private final JavaMailSender jms;
    private final GamificationUtil gamificationUtil;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        String fullName = (String) body.get("fullName");
        String email = (String) body.get("email");
        String password = (String) body.get("password");
        String city = (String) body.get("city");

        if (fullName == null || email == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "All fields are required."));
        }
        if (password.length() < 8) {
            return ResponseEntity.badRequest().body(Map.of("message", "Password must be at least 8 characters."));
        }
        if (uRepo.existsByEmailIgnoreCase(email)) {
            return ResponseEntity.badRequest().body(Map.of("message", "An account with this email already exists."));
        }

        User user = new User();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPassword(DigestUtils.md5DigestAsHex(password.getBytes()));
        user.setCity(city);
        user.setEmailVerified(true);
        user.setStreakDays(1);
        user.setLastLoginDate(LocalDate.now());
        gamificationUtil.awardPoints(user, GamificationUtil.REGISTER);
        uRepo.save(user);

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("Welcome to SkillSwap!");
            message.setText("Hi " + fullName + ", your SkillSwap account has been created.");
            jms.send(message);
        } catch (Exception e) {
            e.printStackTrace();
        }

        request.getSession().setAttribute("userId", user.getId());
        return ResponseEntity.ok(Map.of("userId", user.getId(), "fullName", user.getFullName(), "role", user.getRole()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        String email = (String) body.get("email");
        String password = (String) body.get("password");
        String hashPassword = password == null ? "" : DigestUtils.md5DigestAsHex(password.getBytes());

        User user = uRepo.findByEmailIgnoreCase(email == null ? "" : email).orElse(null);
        if (user == null || !user.getPassword().equals(hashPassword)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Invalid email or password."));
        }
        if (!user.isActive()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "This account has been deactivated."));
        }

        LocalDate today = LocalDate.now();
        if (!today.equals(user.getLastLoginDate())) {
            LocalDate yesterday = today.minusDays(1);
            user.setStreakDays(yesterday.equals(user.getLastLoginDate()) ? user.getStreakDays() + 1 : 1);
            user.setLastLoginDate(today);
            gamificationUtil.awardPoints(user, GamificationUtil.DAILY_LOGIN);
            gamificationUtil.checkAndAwardBadges(user);
            uRepo.save(user);
        }

        request.getSession().setAttribute("userId", user.getId());
        return ResponseEntity.ok(Map.of("userId", user.getId(), "fullName", user.getFullName(), "role", user.getRole()));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok(Map.of("ok", true));
    }
}
