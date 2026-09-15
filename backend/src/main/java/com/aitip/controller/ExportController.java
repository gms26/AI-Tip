package com.aitip.controller;

import com.aitip.dto.AnalyticsRequest;
import com.aitip.dto.ExportFilterRequest;
import com.aitip.dto.TipExportRecord;
import com.aitip.dto.analytics.TipAnalyticsResponse;
import com.aitip.entity.Tip;
import com.aitip.entity.User;
import com.aitip.repository.TipRepository;
import com.aitip.repository.TipSpecification;
import com.aitip.service.ExportService;
import com.aitip.service.TipAnalyticsService;
import com.aitip.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/export")
@RequiredArgsConstructor
public class ExportController {

    private final ExportService exportService;
    private final TipRepository tipRepository;
    private final UserService userService;
    private final TipAnalyticsService tipAnalyticsService;

    @PostMapping("/tips")
    public ResponseEntity<byte[]> exportTips(
            Authentication authentication,
            @RequestBody(required = false) ExportFilterRequest filter,
            @RequestParam(defaultValue = "csv") String format) {

        User user = userService.getUserByEmail(authentication.getName());
        Specification<Tip> spec = TipSpecification.withFilters(user.getId(), filter);
        List<Tip> tips = tipRepository.findAll(spec);
        List<TipExportRecord> records = exportService.mapToExportRecords(tips);

        if ("json".equalsIgnoreCase(format)) {
            String json = exportService.exportToJson(records);
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setContentDispositionFormData("attachment", "tips.json");
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(bytes);
        } else {
            String csv = exportService.exportTipsToCsv(records);
            byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
            headers.setContentDispositionFormData("attachment", "tips.csv");
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(bytes);
        }
    }

    @PostMapping("/analytics")
    public ResponseEntity<byte[]> exportAnalytics(
            Authentication authentication,
            @RequestBody AnalyticsRequest request) {

        TipAnalyticsResponse response = tipAnalyticsService.getAnalytics(authentication.getName(), request);
        String json = exportService.exportToJson(response);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setContentDispositionFormData("attachment", "analytics.json");

        return ResponseEntity.ok()
                .headers(headers)
                .body(bytes);
    }
}
