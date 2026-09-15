package com.aitip.controller;

import com.aitip.dto.PosBillRequest;
import com.aitip.dto.PosBillResponse;
import com.aitip.service.PosService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pos")
public class PosController {

    private final PosService posService;

    public PosController(PosService posService) {
        this.posService = posService;
    }

    @PostMapping("/bill")
    public ResponseEntity<PosBillResponse> getBill(@Valid @RequestBody PosBillRequest request) {
        PosBillResponse response = posService.getBill(request);
        return ResponseEntity.ok(response);
    }
}
