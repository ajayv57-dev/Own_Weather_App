package com.ownweather.model;

import java.time.LocalDateTime;

public class WeatherActualDto {
    private String districtName;
    private String districtId;
    private Double latitude;
    private Double longitude;
    private LocalDateTime updatedTime;

    // Consensus / Normalized
    private Double consensusTemperature;
    private String weatherCondition; // e.g., "Clear", "Rain", "Cloudy", "Windy"

    // Open-Meteo
    private Double openMeteoTemp;
    private Integer openMeteoHumidity;
    private Double openMeteoPrecipitation;
    private Double openMeteoWindSpeed;
    private Integer openMeteoWindDirection;

    // OpenWeather
    private Double openWeatherTemp;
    private Double openWeatherTempMin;
    private Double openWeatherTempMax;
    private Integer openWeatherHumidity;
    private Double openWeatherWindSpeed;

    // WeatherAPI
    private Double weatherApiTemp;
    private Double weatherApiHumidity;
    private Double weatherApiWindSpeed;
    private Double weatherApiUv;
    private Double weatherApiChanceOfRain;

    // Air Quality Telemetry
    private Double aqi;
    private Double pm25;
    private Double co;
    private Double no2;
    private Double so2;
    private Double ozone;
    private Double dust;
    private Double uvIndex;

    public WeatherActualDto() {}

    // Getters and Setters
    public String getDistrictName() { return districtName; }
    public void setDistrictName(String districtName) { this.districtName = districtName; }

    public String getDistrictId() { return districtId; }
    public void setDistrictId(String districtId) { this.districtId = districtId; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public LocalDateTime getUpdatedTime() { return updatedTime; }
    public void setUpdatedTime(LocalDateTime updatedTime) { this.updatedTime = updatedTime; }

    public Double getConsensusTemperature() { return consensusTemperature; }
    public void setConsensusTemperature(Double consensusTemperature) { this.consensusTemperature = consensusTemperature; }

    public String getWeatherCondition() { return weatherCondition; }
    public void setWeatherCondition(String weatherCondition) { this.weatherCondition = weatherCondition; }

    public Double getOpenMeteoTemp() { return openMeteoTemp; }
    public void setOpenMeteoTemp(Double openMeteoTemp) { this.openMeteoTemp = openMeteoTemp; }

    public Integer getOpenMeteoHumidity() { return openMeteoHumidity; }
    public void setOpenMeteoHumidity(Integer openMeteoHumidity) { this.openMeteoHumidity = openMeteoHumidity; }

    public Double getOpenMeteoPrecipitation() { return openMeteoPrecipitation; }
    public void setOpenMeteoPrecipitation(Double openMeteoPrecipitation) { this.openMeteoPrecipitation = openMeteoPrecipitation; }

    public Double getOpenMeteoWindSpeed() { return openMeteoWindSpeed; }
    public void setOpenMeteoWindSpeed(Double openMeteoWindSpeed) { this.openMeteoWindSpeed = openMeteoWindSpeed; }

    public Integer getOpenMeteoWindDirection() { return openMeteoWindDirection; }
    public void setOpenMeteoWindDirection(Integer openMeteoWindDirection) { this.openMeteoWindDirection = openMeteoWindDirection; }

    public Double getOpenWeatherTemp() { return openWeatherTemp; }
    public void setOpenWeatherTemp(Double openWeatherTemp) { this.openWeatherTemp = openWeatherTemp; }

    public Double getOpenWeatherTempMin() { return openWeatherTempMin; }
    public void setOpenWeatherTempMin(Double openWeatherTempMin) { this.openWeatherTempMin = openWeatherTempMin; }

    public Double getOpenWeatherTempMax() { return openWeatherTempMax; }
    public void setOpenWeatherTempMax(Double openWeatherTempMax) { this.openWeatherTempMax = openWeatherTempMax; }

    public Integer getOpenWeatherHumidity() { return openWeatherHumidity; }
    public void setOpenWeatherHumidity(Integer openWeatherHumidity) { this.openWeatherHumidity = openWeatherHumidity; }

    public Double getOpenWeatherWindSpeed() { return openWeatherWindSpeed; }
    public void setOpenWeatherWindSpeed(Double openWeatherWindSpeed) { this.openWeatherWindSpeed = openWeatherWindSpeed; }

    public Double getWeatherApiTemp() { return weatherApiTemp; }
    public void setWeatherApiTemp(Double weatherApiTemp) { this.weatherApiTemp = weatherApiTemp; }

    public Double getWeatherApiHumidity() { return weatherApiHumidity; }
    public void setWeatherApiHumidity(Double weatherApiHumidity) { this.weatherApiHumidity = weatherApiHumidity; }

    public Double getWeatherApiWindSpeed() { return weatherApiWindSpeed; }
    public void setWeatherApiWindSpeed(Double weatherApiWindSpeed) { this.weatherApiWindSpeed = weatherApiWindSpeed; }

    public Double getWeatherApiUv() { return weatherApiUv; }
    public void setWeatherApiUv(Double weatherApiUv) { this.weatherApiUv = weatherApiUv; }

    public Double getWeatherApiChanceOfRain() { return weatherApiChanceOfRain; }
    public void setWeatherApiChanceOfRain(Double weatherApiChanceOfRain) { this.weatherApiChanceOfRain = weatherApiChanceOfRain; }

    public Double getAqi() { return aqi; }
    public void setAqi(Double aqi) { this.aqi = aqi; }

    public Double getPm25() { return pm25; }
    public void setPm25(Double pm25) { this.pm25 = pm25; }

    public Double getCo() { return co; }
    public void setCo(Double co) { this.co = co; }

    public Double getNo2() { return no2; }
    public void setNo2(Double no2) { this.no2 = no2; }

    public Double getSo2() { return so2; }
    public void setSo2(Double so2) { this.so2 = so2; }

    public Double getOzone() { return ozone; }
    public void setOzone(Double ozone) { this.ozone = ozone; }

    public Double getDust() { return dust; }
    public void setDust(Double dust) { this.dust = dust; }

    public Double getUvIndex() { return uvIndex; }
    public void setUvIndex(Double uvIndex) { this.uvIndex = uvIndex; }
}
