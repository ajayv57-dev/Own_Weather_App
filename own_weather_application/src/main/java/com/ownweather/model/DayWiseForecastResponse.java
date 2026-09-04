package com.ownweather.model;

import java.util.ArrayList;
import java.util.List;

public class DayWiseForecastResponse {
    private String districtName;
    private String date;
    private int dayIndex;
    private String vendor;
    private Double tempMin;
    private Double tempMax;
    private Double tempMean;
    private Double totalPrecipitation;
    private Double maxWindSpeed;
    private List<HourlyForecastDto> hourly = new ArrayList<>();

    public DayWiseForecastResponse() {}

    public String getDistrictName() { return districtName; }
    public void setDistrictName(String districtName) { this.districtName = districtName; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public int getDayIndex() { return dayIndex; }
    public void setDayIndex(int dayIndex) { this.dayIndex = dayIndex; }

    public String getVendor() { return vendor; }
    public void setVendor(String vendor) { this.vendor = vendor; }

    public Double getTempMin() { return tempMin; }
    public void setTempMin(Double tempMin) { this.tempMin = tempMin; }

    public Double getTempMax() { return tempMax; }
    public void setTempMax(Double tempMax) { this.tempMax = tempMax; }

    public Double getTempMean() { return tempMean; }
    public void setTempMean(Double tempMean) { this.tempMean = tempMean; }

    public Double getTotalPrecipitation() { return totalPrecipitation; }
    public void setTotalPrecipitation(Double totalPrecipitation) { this.totalPrecipitation = totalPrecipitation; }

    public Double getMaxWindSpeed() { return maxWindSpeed; }
    public void setMaxWindSpeed(Double maxWindSpeed) { this.maxWindSpeed = maxWindSpeed; }

    public List<HourlyForecastDto> getHourly() { return hourly; }
    public void setHourly(List<HourlyForecastDto> hourly) { this.hourly = hourly; }
}
