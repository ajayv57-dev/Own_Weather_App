package com.ownweather.model;

public class HourlyForecastDto {
    private int hour; // 0 to 23
    private String timeLabel; // "00:00", "01:00", etc.
    private Double temperature;
    private Double rainQty;
    private Double windSpeed;
    private Double windDirection;
    private Double humidity;

    public HourlyForecastDto() {}

    public HourlyForecastDto(int hour, Double temperature, Double rainQty, Double windSpeed, Double windDirection, Double humidity) {
        this.hour = hour;
        this.timeLabel = String.format("%02d:00", hour);
        this.temperature = temperature;
        this.rainQty = rainQty;
        this.windSpeed = windSpeed;
        this.windDirection = windDirection;
        this.humidity = humidity;
    }

    public int getHour() { return hour; }
    public void setHour(int hour) { 
        this.hour = hour; 
        this.timeLabel = String.format("%02d:00", hour);
    }

    public String getTimeLabel() { return timeLabel; }
    public void setTimeLabel(String timeLabel) { this.timeLabel = timeLabel; }

    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }

    public Double getRainQty() { return rainQty; }
    public void setRainQty(Double rainQty) { this.rainQty = rainQty; }

    public Double getWindSpeed() { return windSpeed; }
    public void setWindSpeed(Double windSpeed) { this.windSpeed = windSpeed; }

    public Double getWindDirection() { return windDirection; }
    public void setWindDirection(Double windDirection) { this.windDirection = windDirection; }

    public Double getHumidity() { return humidity; }
    public void setHumidity(Double humidity) { this.humidity = humidity; }
}
