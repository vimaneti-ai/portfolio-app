package com.vinod.portfolio.dto;

import java.util.List;

public class AnalyticsSummaryDTO {

    private long totalEvents;
    private long uniqueSessions;
    private long pageViews;
    private List<CountItem> browsers;
    private List<CountItem> operatingSystems;
    private List<CountItem> deviceTypes;
    private List<CountItem> countries;
    private List<DailyCount> last30Days;
    private List<RecentEvent> recentEvents;

    public record CountItem(String name, long count) {}
    public record DailyCount(String date, long count) {}
    public record RecentEvent(
        String eventType, String eventName, String pageUrl,
        String country, String city, String browser,
        String deviceType, String createdAt
    ) {}

    public long getTotalEvents() { return totalEvents; }
    public void setTotalEvents(long totalEvents) { this.totalEvents = totalEvents; }

    public long getUniqueSessions() { return uniqueSessions; }
    public void setUniqueSessions(long uniqueSessions) { this.uniqueSessions = uniqueSessions; }

    public long getPageViews() { return pageViews; }
    public void setPageViews(long pageViews) { this.pageViews = pageViews; }

    public List<CountItem> getBrowsers() { return browsers; }
    public void setBrowsers(List<CountItem> browsers) { this.browsers = browsers; }

    public List<CountItem> getOperatingSystems() { return operatingSystems; }
    public void setOperatingSystems(List<CountItem> operatingSystems) { this.operatingSystems = operatingSystems; }

    public List<CountItem> getDeviceTypes() { return deviceTypes; }
    public void setDeviceTypes(List<CountItem> deviceTypes) { this.deviceTypes = deviceTypes; }

    public List<CountItem> getCountries() { return countries; }
    public void setCountries(List<CountItem> countries) { this.countries = countries; }

    public List<DailyCount> getLast30Days() { return last30Days; }
    public void setLast30Days(List<DailyCount> last30Days) { this.last30Days = last30Days; }

    public List<RecentEvent> getRecentEvents() { return recentEvents; }
    public void setRecentEvents(List<RecentEvent> recentEvents) { this.recentEvents = recentEvents; }
}
