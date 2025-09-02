package com.goldatech.collectionsservice.domain.service;

import com.goldatech.collectionsservice.domain.model.Collection;
import com.goldatech.collectionsservice.domain.repository.CollectionRepository;
import com.goldatech.collectionsservice.web.dto.response.CollectionTrendDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CollectionTrendService {

    private final CollectionRepository collectionRepository;

    public enum Interval {
        DAILY, WEEKLY, MONTHLY
    }

    public List<CollectionTrendDTO> getTrends(LocalDateTime start, LocalDateTime end, Interval interval) {
        List<Collection> collections = collectionRepository.findByInitiatedAtBetween(start, end);

        // Choose grouping function
        Function<Collection, LocalDate> groupingFn = switch (interval) {
            case DAILY -> c -> c.getInitiatedAt().toLocalDate();
            case WEEKLY -> c -> c.getInitiatedAt().toLocalDate()
                    .with(DayOfWeek.MONDAY); // start of week
            case MONTHLY -> c -> c.getInitiatedAt().toLocalDate()
                    .withDayOfMonth(1); // start of month
        };

        // Group by interval
        Map<LocalDate, List<Collection>> grouped = collections.stream()
                .collect(Collectors.groupingBy(groupingFn));

        return grouped.entrySet().stream()
                .map(entry -> {
                    LocalDate period = entry.getKey();
                    List<Collection> groupCollections = entry.getValue();

                    long totalCount = groupCollections.size();
                    BigDecimal totalAmount = groupCollections.stream()
                            .map(Collection::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    Map<String, Long> channelCounts = groupCollections.stream()
                            .collect(Collectors.groupingBy(
                                    c -> c.getPaymentChannel() != null ? c.getPaymentChannel() : "UNKNOWN",
                                    Collectors.counting()
                            ));

                    Map<String, Long> statusCounts = groupCollections.stream()
                            .collect(Collectors.groupingBy(
                                    c -> c.getStatus().name(),
                                    Collectors.counting()
                            ));

                    return new CollectionTrendDTO(period, totalCount, totalAmount, channelCounts, statusCounts);
                })
                .sorted(Comparator.comparing(CollectionTrendDTO::date))
                .toList();
    }
}

