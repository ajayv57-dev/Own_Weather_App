package com.ownweather.service;

import com.ownweather.model.*;
import com.ownweather.repository.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class WeatherService {

    private final DistrictRepository districtRepository;
    private final WeatherActualRepository weatherActualRepository;
    private final ForecastRepository forecastRepository;
    private final PipelineStatusRepository pipelineStatusRepository;

    public WeatherService(DistrictRepository districtRepository,
                          WeatherActualRepository weatherActualRepository,
                          ForecastRepository forecastRepository,
                          PipelineStatusRepository pipelineStatusRepository) {
        this.districtRepository = districtRepository;
        this.weatherActualRepository = weatherActualRepository;
        this.forecastRepository = forecastRepository;
        this.pipelineStatusRepository = pipelineStatusRepository;
    }

    public List<DistrictDto> getAllDistricts() {
        return districtRepository.findAll();
    }

    public String getDistrictGeoJson() {
        return districtRepository.getGeoJsonFeatureCollection();
    }

    public List<WeatherActualDto> getStatewideActualWeather() {
        return weatherActualRepository.findAllActual();
    }

    public Optional<WeatherActualDto> getDistrictActualWeather(String districtName) {
        return weatherActualRepository.findByDistrictName(districtName);
    }

    public List<Map<String, Object>> getAvailableForecastDates() {
        return forecastRepository.getAvailableDates();
    }

    public DayWiseForecastResponse getDayWiseForecast(String districtName, int dayIndex, String vendor) {
        return forecastRepository.getDayWiseForecast(districtName, dayIndex, vendor);
    }

    public List<Map<String, Object>> getStatewideDailyForecast(int dayIndex, String vendor) {
        return forecastRepository.getStatewideDailyForecast(dayIndex, vendor);
    }

    public CombinedForecastResponse getCombinedForecast(String districtName, String startDate, String endDate, String vendor) {
        return forecastRepository.getCombinedForecast(districtName, startDate, endDate, vendor);
    }

    public List<PipelineStatusDto> getPipelineStatus() {
        return pipelineStatusRepository.getRecentPipelineRuns();
    }
}
