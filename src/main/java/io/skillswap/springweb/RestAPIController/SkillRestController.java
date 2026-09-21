package io.skillswap.springweb.RestAPIController;

import io.skillswap.springweb.Model.Skill;
import io.skillswap.springweb.Repository.SkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/skills")
public class SkillRestController {

    private final SkillRepository skillRepo;

    @GetMapping("/catalog")
    public List<Map<String, String>> catalog() {
        return skillRepo.findAll().stream()
                .map(Skill::getName)
                .sorted()
                .map(name -> Map.of("name", name))
                .toList();
    }
}
