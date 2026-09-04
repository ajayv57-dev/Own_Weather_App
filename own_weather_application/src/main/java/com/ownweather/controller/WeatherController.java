package com.ownweather.controller;

import com.ownweather.model.*;
import com.ownweather.service.WeatherService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class WeatherController {

    private final WeatherService weatherService;
    private final com.ownweather.service.VaultConfigService vaultConfigService;

    public WeatherController(WeatherService weatherService, com.ownweather.service.VaultConfigService vaultConfigService) {
        this.weatherService = weatherService;
        this.vaultConfigService = vaultConfigService;
    }

    @GetMapping("/config/public")
    public ResponseEntity<Map<String, Object>> getPublicConfig() {
        return ResponseEntity.ok(vaultConfigService.getPublicConfig());
    }

    @GetMapping("/districts")
    public ResponseEntity<List<DistrictDto>> getDistricts() {
        return ResponseEntity.ok(weatherService.getAllDistricts());
    }

    @GetMapping(value = "/districts/geojson", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getDistrictGeoJson() {
        return ResponseEntity.ok(weatherService.getDistrictGeoJson());
    }

    @GetMapping("/actual/summary")
    public ResponseEntity<List<WeatherActualDto>> getActualSummary() {
        return ResponseEntity.ok(weatherService.getStatewideActualWeather());
    }

    @GetMapping("/actual/districts/{name}")
    public ResponseEntity<WeatherActualDto> getDistrictActual(@PathVariable String name) {
        return weatherService.getDistrictActualWeather(name)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/forecast/available-dates")
    public ResponseEntity<List<Map<String, Object>>> getAvailableForecastDates() {
        return ResponseEntity.ok(weatherService.getAvailableForecastDates());
    }

    @GetMapping("/forecast/day-wise")
    public ResponseEntity<DayWiseForecastResponse> getDayWiseForecast(
            @RequestParam(defaultValue = "Chennai") String districtName,
            @RequestParam(defaultValue = "0") int dayIndex,
            @RequestParam(defaultValue = "open_meteo") String vendor) {
        return ResponseEntity.ok(weatherService.getDayWiseForecast(districtName, dayIndex, vendor));
    }

    @GetMapping("/forecast/statewide-daily")
    public ResponseEntity<List<Map<String, Object>>> getStatewideDailyForecast(
            @RequestParam(defaultValue = "0") int dayIndex,
            @RequestParam(defaultValue = "open_meteo") String vendor) {
        return ResponseEntity.ok(weatherService.getStatewideDailyForecast(dayIndex, vendor));
    }

    @GetMapping("/forecast/combined")
    public ResponseEntity<CombinedForecastResponse> getCombinedForecast(
            @RequestParam(defaultValue = "Chennai") String districtName,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "open_meteo") String vendor) {
        return ResponseEntity.ok(weatherService.getCombinedForecast(districtName, startDate, endDate, vendor));
    }

    @GetMapping("/pipeline/status")
    public ResponseEntity<List<PipelineStatusDto>> getPipelineStatus() {
        return ResponseEntity.ok(weatherService.getPipelineStatus());
    }
}
