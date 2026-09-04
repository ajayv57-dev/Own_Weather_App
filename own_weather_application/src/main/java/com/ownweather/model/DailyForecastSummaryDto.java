package com.ownweather.model;

public class DailyForecastSummaryDto {
    private int dayIndex; // 0 to 7
    private String date; // "2026-09-03"
    private Double tempMax;
    private Double tempMean;
    private Double tempMin;
    private Double precipSum;
    private Double windSpeedMax;
    private Double windDirDominant;
    private Double uvIndexMax;

    public DailyForecastSummaryDto() {}

    public int getDayIndex() { return dayIndex; }
    public void setDayIndex(int dayIndex) { this.dayIndex = dayIndex; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public Double getTempMax() { return tempMax; }
    public void setTempMax(Double tempMax) { this.tempMax = tempMax; }

    public Double getTempMean() { return tempMean; }
    public void setTempMean(Double tempMean) { this.tempMean = tempMean; }

    public Double getTempMin() { return tempMin; }
    public void setTempMin(Double tempMin) { this.tempMin = tempMin; }

    public Double getPrecipSum() { return precipSum; }
    public void setPrecipSum(Double precipSum) { this.precipSum = precipSum; }

    public Double getWindSpeedMax() { return windSpeedMax; }
    public void setWindSpeedMax(Double windSpeedMax) { this.windSpeedMax = windSpeedMax; }

    public Double getWindDirDominant() { return windDirDominant; }
    public void setWindDirDominant(Double windDirDominant) { this.windDirDominant = windDirDominant; }

    public Double getUvIndexMax() { return uvIndexMax; }
    public void setUvIndexMax(Double uvIndexMax) { this.uvIndexMax = uvIndexMax; }
}
