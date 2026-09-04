package com.ownweather.model;

import java.util.ArrayList;
import java.util.List;

public class CombinedForecastResponse {
    private String districtName;
    private String startDate;
    private String endDate;
    private String vendor;
    private int totalDays;
    private Double cumulativeRainfall;
    private Double avgTemperature;
    private Double minTemperature;
    private Double maxTemperature;
    private Double avgMaxWindSpeed;
    private List<DailyForecastSummaryDto> dailyBreakdown = new ArrayList<>();

    public CombinedForecastResponse() {}

    public String getDistrictName() { return districtName; }
    public void setDistrictName(String districtName) { this.districtName = districtName; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }

    public String getVendor() { return vendor; }
    public void setVendor(String vendor) { this.vendor = vendor; }

    public int getTotalDays() { return totalDays; }
    public void setTotalDays(int totalDays) { this.totalDays = totalDays; }

    public Double getCumulativeRainfall() { return cumulativeRainfall; }
    public void setCumulativeRainfall(Double cumulativeRainfall) { this.cumulativeRainfall = cumulativeRainfall; }

    public Double getAvgTemperature() { return avgTemperature; }
    public void setAvgTemperature(Double avgTemperature) { this.avgTemperature = avgTemperature; }

    public Double getMinTemperature() { return minTemperature; }
    public void setMinTemperature(Double minTemperature) { this.minTemperature = minTemperature; }

    public Double getMaxTemperature() { return maxTemperature; }
    public void setMaxTemperature(Double maxTemperature) { this.maxTemperature = maxTemperature; }

    public Double getAvgMaxWindSpeed() { return avgMaxWindSpeed; }
    public void setAvgMaxWindSpeed(Double avgMaxWindSpeed) { this.avgMaxWindSpeed = avgMaxWindSpeed; }

    public List<DailyForecastSummaryDto> getDailyBreakdown() { return dailyBreakdown; }
    public void setDailyBreakdown(List<DailyForecastSummaryDto> dailyBreakdown) { this.dailyBreakdown = dailyBreakdown; }
}
