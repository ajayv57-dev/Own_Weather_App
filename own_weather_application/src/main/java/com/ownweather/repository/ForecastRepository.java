package com.ownweather.repository;

import com.ownweather.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Repository
public class ForecastRepository {

    private static final Logger log = LoggerFactory.getLogger(ForecastRepository.class);
    private final JdbcTemplate jdbcTemplate;

    public ForecastRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Sanitizes vendor name to prevent SQL injection.
     */
    private String sanitizeVendor(String vendor) {
        if (vendor == null) return "open_meteo";
        String v = vendor.toLowerCase().trim();
        return switch (v) {
            case "openweather", "open_weather" -> "open_weather";
            case "weatherapi" -> "weatherapi";
            default -> "open_meteo";
        };
    }

    /**
     * Retrieves the available forecast dates and day indices from open_meteo_forecast.
     */
    public List<Map<String, Object>> getAvailableDates() {
        String sql = "SELECT time_day_0, time_day_1, time_day_2, time_day_3, time_day_4, time_day_5, time_day_6, time_day_7 FROM open_meteo_forecast LIMIT 1";
        return jdbcTemplate.query(sql, rs -> {
            List<Map<String, Object>> list = new ArrayList<>();
            if (rs.next()) {
                for (int i = 0; i <= 7; i++) {
                    String date = rs.getString("time_day_" + i);
                    if (date != null && !date.isBlank()) {
                        Map<String, Object> map = new LinkedHashMap<>();
                        map.put("dayIndex", i);
                        map.put("date", date);
                        list.add(map);
                    }
                }
            }
            return list;
        });
    }

    /**
     * Returns 24-hour hourly forecast for a given district and dayIndex.
     */
    public DayWiseForecastResponse getDayWiseForecast(String districtName, int dayIndex, String vendorInput) {
        String vendor = sanitizeVendor(vendorInput);
        if (dayIndex < 0 || dayIndex > 7) dayIndex = 0;

        String tableName = vendor + "_forecast_hourly_" + dayIndex;
        String sql = "SELECT * FROM " + tableName + " WHERE LOWER(district_name) = LOWER(?) LIMIT 1";

        final int targetDayIndex = dayIndex;
        List<DayWiseForecastResponse> results = jdbcTemplate.query(sql, (rs, rowNum) -> {
            DayWiseForecastResponse response = new DayWiseForecastResponse();
            response.setDistrictName(rs.getString("district_name"));
            response.setDate(rs.getString("date"));
            response.setDayIndex(targetDayIndex);
            response.setVendor(vendor);

            List<HourlyForecastDto> hourlyList = new ArrayList<>();
            double tempMin = Double.MAX_VALUE;
            double tempMax = Double.MIN_VALUE;
            double tempSum = 0.0;
            int tempCount = 0;
            double totalRain = 0.0;
            double maxWind = 0.0;

            for (int h = 0; h < 24; h++) {
                HourlyForecastDto hDto = new HourlyForecastDto();
                hDto.setHour(h);

                Double temp = getNullableDouble(rs, "temperature_hour" + h);
                Double rain = getNullableDouble(rs, "rain_qty_hour" + h);
                Double windSpeed = getNullableDouble(rs, "windspeed_hour" + h);
                Double windDir = getNullableDouble(rs, "winddirection_hour" + h);
                Double humidity = getNullableDouble(rs, "humidity_hour" + h);

                hDto.setTemperature(temp);
                hDto.setRainQty(rain);
                hDto.setWindSpeed(windSpeed);
                hDto.setWindDirection(windDir);
                hDto.setHumidity(humidity);

                if (temp != null) {
                    tempMin = Math.min(tempMin, temp);
                    tempMax = Math.max(tempMax, temp);
                    tempSum += temp;
                    tempCount++;
                }
                if (rain != null) {
                    totalRain += rain;
                }
                if (windSpeed != null) {
                    maxWind = Math.max(maxWind, windSpeed);
                }

                hourlyList.add(hDto);
            }

            response.setHourly(hourlyList);
            response.setTempMin(tempCount > 0 ? Math.round(tempMin * 10.0) / 10.0 : null);
            response.setTempMax(tempCount > 0 ? Math.round(tempMax * 10.0) / 10.0 : null);
            response.setTempMean(tempCount > 0 ? Math.round((tempSum / tempCount) * 10.0) / 10.0 : null);
            response.setTotalPrecipitation(Math.round(totalRain * 10.0) / 10.0);
            response.setMaxWindSpeed(Math.round(maxWind * 10.0) / 10.0);

            return response;
        }, districtName);

        if (!results.isEmpty()) {
            return results.get(0);
        }

        // Return empty container if no record
        DayWiseForecastResponse empty = new DayWiseForecastResponse();
        empty.setDistrictName(districtName);
        empty.setDayIndex(dayIndex);
        empty.setVendor(vendor);
        return empty;
    }

    /**
     * Returns state-wide daily forecast cards for all 38 districts on a given dayIndex.
     */
    public List<Map<String, Object>> getStatewideDailyForecast(int dayIndex, String vendorInput) {
        String vendor = sanitizeVendor(vendorInput);
        if (dayIndex < 0 || dayIndex > 7) dayIndex = 0;

        String tableName = vendor + "_forecast";
        String sql;
        if ("open_weather".equals(vendor)) {
            sql = "SELECT district_name, district_id, latitude, longitude, updated_time, " +
                  "time_day_" + dayIndex + " AS forecast_date, " +
                  "temp_min_day_" + dayIndex + " AS temp_min, " +
                  "temp_max_day_" + dayIndex + " AS temp_max, " +
                  "temp_day_" + dayIndex + " AS temp_mean, " +
                  "humidity_day_" + dayIndex + " AS humidity, " +
                  "wind_speed_day_" + dayIndex + " AS wind_speed " +
                  "FROM " + tableName + " ORDER BY district_name ASC";
        } else {
            sql = "SELECT district_name, district_id, latitude, longitude, updated_time, " +
                  "time_day_" + dayIndex + " AS forecast_date, " +
                  "temp_min_day_" + dayIndex + " AS temp_min, " +
                  "temp_max_day_" + dayIndex + " AS temp_max, " +
                  "temp_mean_day_" + dayIndex + " AS temp_mean, " +
                  "precip_sum_day_" + dayIndex + " AS precip_sum, " +
                  "wind_speed_max_day_" + dayIndex + " AS wind_speed, " +
                  "wind_dir_dom_day_" + dayIndex + " AS wind_dir " +
                  "FROM " + tableName + " ORDER BY district_name ASC";
        }

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("districtName", rs.getString("district_name"));
            map.put("districtId", rs.getString("district_id"));
            map.put("latitude", rs.getDouble("latitude"));
            map.put("longitude", rs.getDouble("longitude"));
            map.put("date", rs.getString("forecast_date"));
            map.put("tempMin", rs.getObject("temp_min"));
            map.put("tempMax", rs.getObject("temp_max"));
            map.put("tempMean", rs.getObject("temp_mean"));
            if ("open_weather".equals(vendor)) {
                map.put("humidity", rs.getObject("humidity"));
                map.put("precipSum", 0.0);
            } else {
                map.put("precipSum", rs.getObject("precip_sum"));
                map.put("windDir", rs.getObject("wind_dir"));
            }
            map.put("windSpeed", rs.getObject("wind_speed"));
            return map;
        });
    }

    /**
     * Combined forecast analytics: aggregates data between startDate and endDate.
     */
    public CombinedForecastResponse getCombinedForecast(String districtName, String startDate, String endDate, String vendorInput) {
        String vendor = sanitizeVendor(vendorInput);
        List<Map<String, Object>> availableDates = getAvailableDates();

        // Identify day indices within range
        List<Integer> matchingIndices = new ArrayList<>();
        Map<Integer, String> indexToDate = new HashMap<>();

        for (Map<String, Object> dateMap : availableDates) {
            int idx = (Integer) dateMap.get("dayIndex");
            String d = (String) dateMap.get("date");
            indexToDate.put(idx, d);

            if ((startDate == null || d.compareTo(startDate) >= 0) &&
                (endDate == null || d.compareTo(endDate) <= 0)) {
                matchingIndices.add(idx);
            }
        }

        if (matchingIndices.isEmpty() && !availableDates.isEmpty()) {
            matchingIndices.add((Integer) availableDates.get(0).get("dayIndex"));
        }

        String tableName = vendor + "_forecast";
        String sql = "SELECT * FROM " + tableName + " WHERE LOWER(district_name) = LOWER(?) LIMIT 1";

        List<CombinedForecastResponse> results = jdbcTemplate.query(sql, (rs, rowNum) -> {
            CombinedForecastResponse response = new CombinedForecastResponse();
            response.setDistrictName(rs.getString("district_name"));
            response.setVendor(vendor);
            response.setStartDate(startDate);
            response.setEndDate(endDate);

            List<DailyForecastSummaryDto> dailyList = new ArrayList<>();
            double cumRain = 0.0;
            double minTemp = Double.MAX_VALUE;
            double maxTemp = Double.MIN_VALUE;
            double tempSum = 0.0;
            int tempCount = 0;
            double windSum = 0.0;
            int windCount = 0;

            for (int idx : matchingIndices) {
                DailyForecastSummaryDto dayDto = new DailyForecastSummaryDto();
                dayDto.setDayIndex(idx);
                dayDto.setDate(indexToDate.get(idx));

                Double tMin = getNullableDouble(rs, "temp_min_day_" + idx);
                Double tMax = getNullableDouble(rs, "temp_max_day_" + idx);
                Double tMean = "open_weather".equals(vendor) ? getNullableDouble(rs, "temp_day_" + idx) : getNullableDouble(rs, "temp_mean_day_" + idx);
                Double precip = "open_weather".equals(vendor) ? 0.0 : getNullableDouble(rs, "precip_sum_day_" + idx);
                Double windMax = getNullableDouble(rs, "wind_speed_max_day_" + idx);
                if (windMax == null && "open_weather".equals(vendor)) {
                    windMax = getNullableDouble(rs, "wind_speed_day_" + idx);
                }

                dayDto.setTempMin(tMin);
                dayDto.setTempMax(tMax);
                dayDto.setTempMean(tMean);
                dayDto.setPrecipSum(precip);
                dayDto.setWindSpeedMax(windMax);

                if (tMin != null) minTemp = Math.min(minTemp, tMin);
                if (tMax != null) maxTemp = Math.max(maxTemp, tMax);
                if (tMean != null) { tempSum += tMean; tempCount++; }
                if (precip != null) cumRain += precip;
                if (windMax != null) { windSum += windMax; windCount++; }

                dailyList.add(dayDto);
            }

            response.setTotalDays(dailyList.size());
            response.setCumulativeRainfall(Math.round(cumRain * 10.0) / 10.0);
            response.setMinTemperature(tempCount > 0 ? Math.round(minTemp * 10.0) / 10.0 : null);
            response.setMaxTemperature(tempCount > 0 ? Math.round(maxTemp * 10.0) / 10.0 : null);
            response.setAvgTemperature(tempCount > 0 ? Math.round((tempSum / tempCount) * 10.0) / 10.0 : null);
            response.setAvgMaxWindSpeed(windCount > 0 ? Math.round((windSum / windCount) * 10.0) / 10.0 : null);
            response.setDailyBreakdown(dailyList);

            return response;
        }, districtName);

        if (!results.isEmpty()) {
            return results.get(0);
        }

        CombinedForecastResponse empty = new CombinedForecastResponse();
        empty.setDistrictName(districtName);
        empty.setVendor(vendor);
        return empty;
    }

    private Double getNullableDouble(ResultSet rs, String col) {
        try {
            double v = rs.getDouble(col);
            return rs.wasNull() ? null : v;
        } catch (SQLException e) {
            return null;
        }
    }
}
