package com.vinod.portfolio.service;

import com.vinod.portfolio.dto.AnalyticsSummaryDTO;
import com.vinod.portfolio.model.VisitorEvent;
import com.vinod.portfolio.repository.VisitorEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AnalyticsDashboardServiceTest {

    @Mock
    private VisitorEventRepository repository;

    @InjectMocks
    private AnalyticsDashboardService service;

    private VisitorEvent event(String browser, String os, String device,
                               String country, String eventType, LocalDateTime time) {
        VisitorEvent e = new VisitorEvent();
        e.setBrowser(browser);
        e.setOperatingSystem(os);
        e.setDeviceType(device);
        e.setCountry(country);
        e.setEventType(eventType);
        e.setEventName("site_loaded");
        e.setPageUrl("/");
        e.setSessionId("sess-1");
        e.setCreatedAt(time);
        return e;
    }

    private List<VisitorEvent> sampleEvents() {
        LocalDateTime now = LocalDateTime.now();
        return List.of(
            event("Chrome",  "Windows", "desktop", "United States", "page_view",   now.minusDays(1)),
            event("Chrome",  "Windows", "desktop", "India",         "page_view",   now.minusDays(2)),
            event("Safari",  "macOS",   "desktop", "India",         "page_view",   now.minusDays(3)),
            event("Firefox", "Linux",   "desktop", null,            "link_click",  now.minusDays(5)),
            event("Chrome",  "Android", "mobile",  "India",         "page_view",   now.minusDays(10))
        );
    }

    @BeforeEach
    void setUp() {
        when(repository.findAll()).thenReturn(sampleEvents());
        when(repository.countDistinctSessions()).thenReturn(3L);
        when(repository.countByEventType("page_view")).thenReturn(4L);
        when(repository.findTop20ByOrderByCreatedAtDesc()).thenReturn(sampleEvents());
    }

    @Test
    void getSummary_returnsTotalEventCount() {
        AnalyticsSummaryDTO result = service.getSummary();
        assertThat(result.getTotalEvents()).isEqualTo(5);
    }

    @Test
    void getSummary_returnsUniqueSessionCount() {
        AnalyticsSummaryDTO result = service.getSummary();
        assertThat(result.getUniqueSessions()).isEqualTo(3);
    }

    @Test
    void getSummary_returnsPageViewCount() {
        AnalyticsSummaryDTO result = service.getSummary();
        assertThat(result.getPageViews()).isEqualTo(4);
    }

    @Test
    void getSummary_browsersSortedByCountDescending() {
        AnalyticsSummaryDTO result = service.getSummary();
        assertThat(result.getBrowsers()).isNotEmpty();
        assertThat(result.getBrowsers().get(0).name()).isEqualTo("Chrome");
        assertThat(result.getBrowsers().get(0).count()).isEqualTo(3);
    }

    @Test
    void getSummary_browsersExcludeNullValues() {
        VisitorEvent nullBrowser = event(null, "Windows", "desktop", "US", "page_view", LocalDateTime.now());
        when(repository.findAll()).thenReturn(List.of(nullBrowser));

        AnalyticsSummaryDTO result = service.getSummary();
        assertThat(result.getBrowsers()).isEmpty();
    }

    @Test
    void getSummary_operatingSystemsSortedByCountDescending() {
        AnalyticsSummaryDTO result = service.getSummary();
        assertThat(result.getOperatingSystems().get(0).name()).isEqualTo("Windows");
        assertThat(result.getOperatingSystems().get(0).count()).isEqualTo(2);
    }

    @Test
    void getSummary_deviceTypesGroupedCorrectly() {
        AnalyticsSummaryDTO result = service.getSummary();
        assertThat(result.getDeviceTypes()).hasSize(2);
        assertThat(result.getDeviceTypes().get(0).name()).isEqualTo("desktop");
        assertThat(result.getDeviceTypes().get(0).count()).isEqualTo(4);
    }

    @Test
    void getSummary_countriesExcludeNullValues() {
        AnalyticsSummaryDTO result = service.getSummary();
        boolean hasNull = result.getCountries().stream().anyMatch(c -> c.name() == null);
        assertThat(hasNull).isFalse();
    }

    @Test
    void getSummary_countriesSortedByCountDescending() {
        AnalyticsSummaryDTO result = service.getSummary();
        assertThat(result.getCountries().get(0).name()).isEqualTo("India");
        assertThat(result.getCountries().get(0).count()).isEqualTo(3);
    }

    @Test
    void getSummary_last30DaysIncludesOnlyRecentEvents() {
        AnalyticsSummaryDTO result = service.getSummary();
        assertThat(result.getLast30Days()).isNotEmpty();
        result.getLast30Days().forEach(d ->
            assertThat(d.date()).matches("\\d{4}-\\d{2}-\\d{2}")
        );
    }

    @Test
    void getSummary_last30DaysSortedByDateAscending() {
        AnalyticsSummaryDTO result = service.getSummary();
        List<String> dates = result.getLast30Days().stream().map(d -> d.date()).toList();
        assertThat(dates).isSortedAccordingTo(String::compareTo);
    }

    @Test
    void getSummary_recentEventsReturnedFromRepository() {
        AnalyticsSummaryDTO result = service.getSummary();
        assertThat(result.getRecentEvents()).hasSize(5);
        verify(repository, times(1)).findTop20ByOrderByCreatedAtDesc();
    }

    @Test
    void getSummary_recentEventsMappedCorrectly() {
        AnalyticsSummaryDTO result = service.getSummary();
        assertThat(result.getRecentEvents().get(0).eventType()).isEqualTo("page_view");
        assertThat(result.getRecentEvents().get(0).browser()).isEqualTo("Chrome");
        assertThat(result.getRecentEvents().get(0).createdAt()).isNotBlank();
    }

    @Test
    void getSummary_emptyRepositoryReturnsZeroCounts() {
        when(repository.findAll()).thenReturn(List.of());
        when(repository.countDistinctSessions()).thenReturn(0L);
        when(repository.countByEventType(anyString())).thenReturn(0L);
        when(repository.findTop20ByOrderByCreatedAtDesc()).thenReturn(List.of());

        AnalyticsSummaryDTO result = service.getSummary();

        assertThat(result.getTotalEvents()).isZero();
        assertThat(result.getBrowsers()).isEmpty();
        assertThat(result.getLast30Days()).isEmpty();
        assertThat(result.getRecentEvents()).isEmpty();
    }
}
