package com.aitip.controller;

import com.aitip.dto.AchievementResponse;
import com.aitip.dto.AchievementSummaryResponse;
import com.aitip.service.AchievementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/achievements")
@RequiredArgsConstructor
public class AchievementController {

    private final AchievementService achievementService;

    @GetMapping
    public ResponseEntity<List<AchievementResponse>> getAchievements(Authentication authentication) {
        return ResponseEntity.ok(achievementService.getAchievements(authentication.getName()));
    }

    @GetMapping("/summary")
    public ResponseEntity<AchievementSummaryResponse> getAchievementSummary(Authentication authentication) {
        return ResponseEntity.ok(achievementService.getSummary(authentication.getName()));
    }

    @PostMapping("/evaluate")
    public ResponseEntity<List<AchievementResponse>> evaluateAchievements(Authentication authentication) {
        return ResponseEntity.ok(achievementService.evaluateAchievements(authentication.getName()));
    }
}
