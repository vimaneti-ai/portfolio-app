package com.vinod.portfolio.service;

import com.vinod.portfolio.dto.VisitorEventRequest;
import com.vinod.portfolio.model.VisitorEvent;
import com.vinod.portfolio.repository.VisitorEventRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VisitorAnalyticsServiceTest {

    @Mock
    private VisitorEventRepository repository;

    @InjectMocks
    private VisitorAnalyticsService service;

    @Mock
    private HttpServletRequest request;

    private VisitorEventRequest eventRequest;

    @BeforeEach
    void setUp() {
        eventRequest = new VisitorEventRequest();
        eventRequest.setSessionId("session-123");
        eventRequest.setEventType("page_view");
        eventRequest.setEventName("site_loaded");
        eventRequest.setPageUrl("/");
        eventRequest.setReferrer("");
    }

    @Test
    void track_savesEventToRepository() {
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0 (Macintosh) Chrome/120");

        service.track(eventRequest, request);

        verify(repository, times(1)).save(any(VisitorEvent.class));
    }

    @Test
    void track_persistsSessionIdAndEventType() {
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0");

        service.track(eventRequest, request);

        ArgumentCaptor<VisitorEvent> captor = ArgumentCaptor.forClass(VisitorEvent.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getSessionId()).isEqualTo("session-123");
        assertThat(captor.getValue().getEventType()).isEqualTo("page_view");
    }

    @Test
    void track_extractsFirstIpFromXForwardedForHeader() {
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.5, 10.0.0.1");
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0");

        service.track(eventRequest, request);

        ArgumentCaptor<VisitorEvent> captor = ArgumentCaptor.forClass(VisitorEvent.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getIpTruncated()).isEqualTo("203.0.113.0");
    }

    @Test
    void track_truncatesLastOctetOfIPv4() {
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn("198.51.100.42");
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0");

        service.track(eventRequest, request);

        ArgumentCaptor<VisitorEvent> captor = ArgumentCaptor.forClass(VisitorEvent.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getIpTruncated()).isEqualTo("198.51.100.0");
    }

    @Test
    void track_hashesIp() {
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0");

        service.track(eventRequest, request);

        ArgumentCaptor<VisitorEvent> captor = ArgumentCaptor.forClass(VisitorEvent.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getIpHash()).isNotNull().hasSize(64);
    }

    @Test
    void track_detectsChromeFromUserAgent() {
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("User-Agent"))
                .thenReturn("Mozilla/5.0 (Windows NT 10.0) AppleWebKit/537.36 Chrome/120.0");

        service.track(eventRequest, request);

        ArgumentCaptor<VisitorEvent> captor = ArgumentCaptor.forClass(VisitorEvent.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getBrowser()).isEqualTo("Chrome");
    }

    @Test
    void track_detectsWindowsFromUserAgent() {
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("User-Agent"))
                .thenReturn("Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0");

        service.track(eventRequest, request);

        ArgumentCaptor<VisitorEvent> captor = ArgumentCaptor.forClass(VisitorEvent.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getOperatingSystem()).isEqualTo("Windows");
    }

    @Test
    void track_detectsMobileDevice() {
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("User-Agent"))
                .thenReturn("Mozilla/5.0 (iPhone; CPU iPhone OS 17_0) Mobile/15E148 Safari/604.1");

        service.track(eventRequest, request);

        ArgumentCaptor<VisitorEvent> captor = ArgumentCaptor.forClass(VisitorEvent.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getDeviceType()).isEqualTo("mobile");
    }

    @Test
    void track_skipsGeoIpForLocalhostIp() {
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0");

        service.track(eventRequest, request);

        ArgumentCaptor<VisitorEvent> captor = ArgumentCaptor.forClass(VisitorEvent.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getCountry()).isNull();
        assertThat(captor.getValue().getCity()).isNull();
    }

    @Test
    void track_skipsGeoIpForPrivateNetworkIp() {
        when(request.getHeader("X-Forwarded-For")).thenReturn("10.0.0.5");
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0");

        service.track(eventRequest, request);

        ArgumentCaptor<VisitorEvent> captor = ArgumentCaptor.forClass(VisitorEvent.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getCountry()).isNull();
    }

    @Test
    void track_handlesNullUserAgentGracefully() {
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("User-Agent")).thenReturn(null);

        service.track(eventRequest, request);

        ArgumentCaptor<VisitorEvent> captor = ArgumentCaptor.forClass(VisitorEvent.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getBrowser()).isEqualTo("Unknown");
        assertThat(captor.getValue().getOperatingSystem()).isEqualTo("Unknown");
        assertThat(captor.getValue().getDeviceType()).isEqualTo("Unknown");
    }
}
