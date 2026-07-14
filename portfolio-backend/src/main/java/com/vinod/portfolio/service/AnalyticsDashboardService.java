package com.vinod.portfolio.service;

import com.vinod.portfolio.dto.AnalyticsSummaryDTO;
import com.vinod.portfolio.dto.AnalyticsSummaryDTO.CountItem;
import com.vinod.portfolio.dto.AnalyticsSummaryDTO.DailyCount;
import com.vinod.portfolio.dto.AnalyticsSummaryDTO.RecentEvent;
import com.vinod.portfolio.model.VisitorEvent;
import com.vinod.portfolio.repository.VisitorEventRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AnalyticsDashboardService {

    private final VisitorEventRepository repository;
    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public AnalyticsDashboardService(VisitorEventRepository repository) {
        this.repository = repository;
    }

    public AnalyticsSummaryDTO getSummary() {
        List<VisitorEvent> all = repository.findAll();
        List<VisitorEvent> last30 = all.stream()
                .filter(e -> e.getCreatedAt().isAfter(LocalDateTime.now().minusDays(30)))
                .collect(Collectors.toList());

        AnalyticsSummaryDTO dto = new AnalyticsSummaryDTO();
        dto.setTotalEvents(all.size());
        dto.setUniqueSessions(repository.countDistinctSessions());
        dto.setPageViews(repository.countByEventType("page_view"));
        dto.setBrowsers(groupBy(all, VisitorEvent::getBrowser));
        dto.setOperatingSystems(groupBy(all, VisitorEvent::getOperatingSystem));
        dto.setDeviceTypes(groupBy(all, VisitorEvent::getDeviceType));
        dto.setCountries(groupBy(all, VisitorEvent::getCountry));
        dto.setLast30Days(dailyCounts(last30));
        dto.setRecentEvents(recentEvents());
        return dto;
    }

    private List<CountItem> groupBy(List<VisitorEvent> events,
                                    java.util.function.Function<VisitorEvent, String> field) {
        return events.stream()
                .map(field)
                .filter(v -> v != null && !v.isBlank())
                .collect(Collectors.groupingBy(v -> v, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(e -> new CountItem(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    private List<DailyCount> dailyCounts(List<VisitorEvent> events) {
        return events.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getCreatedAt().format(DAY_FMT),
                        Collectors.counting()
                ))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> new DailyCount(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    private List<RecentEvent> recentEvents() {
        return repository.findTop20ByOrderByCreatedAtDesc().stream()
                .map(e -> new RecentEvent(
                        e.getEventType(),
                        e.getEventName(),
                        e.getPageUrl(),
                        e.getCountry(),
                        e.getCity(),
                        e.getBrowser(),
                        e.getDeviceType(),
                        e.getCreatedAt() != null ? e.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM d, HH:mm")) : ""
                ))
                .collect(Collectors.toList());
    }
}
