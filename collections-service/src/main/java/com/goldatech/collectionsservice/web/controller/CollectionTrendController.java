package com.goldatech.collectionsservice.web.controller;

import com.goldatech.collectionsservice.domain.service.CollectionTrendService;
import com.goldatech.collectionsservice.web.dto.response.CollectionTrendDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/collections/trends")
@RequiredArgsConstructor
@Slf4j
public class CollectionTrendController {

    private final CollectionTrendService trendService;

    @GetMapping
    public ResponseEntity<List<CollectionTrendDTO>> getTrends(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "DAILY") CollectionTrendService.Interval interval
    ) {
        try {
            List<CollectionTrendDTO> trends = trendService.getTrends(startDate, endDate, interval);
            return ResponseEntity.ok(trends);
        } catch (Exception e) {
            log.error("Error fetching trends: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
