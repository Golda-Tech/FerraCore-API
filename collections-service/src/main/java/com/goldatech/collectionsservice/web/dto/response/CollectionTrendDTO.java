package com.goldatech.collectionsservice.web.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public record CollectionTrendDTO(
        LocalDate date,
        long totalCount,
        BigDecimal totalAmount,
        Map<String, Long> channelCounts,
        Map<String, Long> statusCounts
) {}
