package com.ownweather.repository;

import com.ownweather.model.WeatherActualDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
public class WeatherActualRepository {

    private final JdbcTemplate jdbcTemplate;

    public WeatherActualRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<WeatherActualDto> rowMapper = new RowMapper<>() {
        @Override
        public WeatherActualDto mapRow(ResultSet rs, int rowNum) throws SQLException {
            WeatherActualDto dto = new WeatherActualDto();
            dto.setDistrictName(rs.getString("district_name"));
            dto.setDistrictId(rs.getString("district_id"));
            dto.setLatitude(rs.getDouble("centroid_lat"));
            dto.setLongitude(rs.getDouble("centroid_lon"));

            Timestamp ts = rs.getTimestamp("updated_time");
            if (ts != null) {
                dto.setUpdatedTime(ts.toLocalDateTime());
            }

            // Open-Meteo
            dto.setOpenMeteoTemp(getNullableDouble(rs, "meteo_temp"));
            dto.setOpenMeteoHumidity(getNullableInt(rs, "meteo_humidity"));
            dto.setOpenMeteoPrecipitation(getNullableDouble(rs, "meteo_precip"));
            dto.setOpenMeteoWindSpeed(getNullableDouble(rs, "meteo_wind_speed"));
            dto.setOpenMeteoWindDirection(getNullableInt(rs, "meteo_wind_dir"));

            // OpenWeather
            dto.setOpenWeatherTemp(getNullableDouble(rs, "ow_temp"));
            dto.setOpenWeatherTempMin(getNullableDouble(rs, "ow_temp_min"));
            dto.setOpenWeatherTempMax(getNullableDouble(rs, "ow_temp_max"));
            dto.setOpenWeatherHumidity(getNullableInt(rs, "ow_humidity"));
            dto.setOpenWeatherWindSpeed(getNullableDouble(rs, "ow_wind_speed"));

            // WeatherAPI
            dto.setWeatherApiTemp(getNullableDouble(rs, "api_temp"));
            dto.setWeatherApiHumidity(getNullableDouble(rs, "api_humidity"));
            dto.setWeatherApiWindSpeed(getNullableDouble(rs, "api_wind_speed"));
            dto.setWeatherApiUv(getNullableDouble(rs, "api_uv"));
            dto.setWeatherApiChanceOfRain(getNullableDouble(rs, "api_rain_chance"));

            // Air Quality
            dto.setAqi(getNullableDouble(rs, "european_aqi"));
            dto.setPm25(getNullableDouble(rs, "pm2_5"));
            dto.setCo(getNullableDouble(rs, "carbon_monoxide"));
            dto.setNo2(getNullableDouble(rs, "nitrogen_dioxide"));
            dto.setSo2(getNullableDouble(rs, "sulphur_dioxide"));
            dto.setOzone(getNullableDouble(rs, "ozone"));
            dto.setDust(getNullableDouble(rs, "dust"));
            dto.setUvIndex(getNullableDouble(rs, "uv_index"));

            // Compute Consensus Temperature & Weather Condition
            double sum = 0.0;
            int count = 0;
            if (dto.getOpenMeteoTemp() != null) { sum += dto.getOpenMeteoTemp(); count++; }
            if (dto.getOpenWeatherTemp() != null) { sum += dto.getOpenWeatherTemp(); count++; }
            if (dto.getWeatherApiTemp() != null) { sum += dto.getWeatherApiTemp(); count++; }
            dto.setConsensusTemperature(count > 0 ? Math.round((sum / count) * 10.0) / 10.0 : null);

            // Determine condition
            double rain = dto.getOpenMeteoPrecipitation() != null ? dto.getOpenMeteoPrecipitation() : 0.0;
            double wind = dto.getOpenMeteoWindSpeed() != null ? dto.getOpenMeteoWindSpeed() : 0.0;
            if (rain > 1.0 || (dto.getWeatherApiChanceOfRain() != null && dto.getWeatherApiChanceOfRain() > 60)) {
                dto.setWeatherCondition("Rain");
            } else if (wind > 20.0) {
                dto.setWeatherCondition("Windy");
            } else if (dto.getOpenMeteoHumidity() != null && dto.getOpenMeteoHumidity() > 75) {
                dto.setWeatherCondition("Humid / Partly Cloudy");
            } else {
                dto.setWeatherCondition("Clear / Sunny");
            }

            return dto;
        }

        private Double getNullableDouble(ResultSet rs, String col) throws SQLException {
            double val = rs.getDouble(col);
            return rs.wasNull() ? null : val;
        }

        private Integer getNullableInt(ResultSet rs, String col) throws SQLException {
            int val = rs.getInt(col);
            return rs.wasNull() ? null : val;
        }
    };

    private final String baseSql = """
        SELECT 
            b.district_name,
            b.centroid_lat,
            b.centroid_lon,
            COALESCE(m.district_id, w.district_id, a.district_id) AS district_id,
            COALESCE(m.updated_time, w.updated_time, a.updated_time) AS updated_time,
            m.temperature AS meteo_temp,
            m.relative_humidity AS meteo_humidity,
            m.precipitation AS meteo_precip,
            m.wind_speed AS meteo_wind_speed,
            m.wind_direction AS meteo_wind_dir,
            w.temperature AS ow_temp,
            w.temp_min AS ow_temp_min,
            w.temp_max AS ow_temp_max,
            w.humidity AS ow_humidity,
            w.wind_speed AS ow_wind_speed,
            a.temperature AS api_temp,
            a.humidity AS api_humidity,
            a.wind_speed AS api_wind_speed,
            a.uv AS api_uv,
            a.chance_of_rain AS api_rain_chance,
            aq.european_aqi,
            aq.pm2_5,
            aq.carbon_monoxide,
            aq.nitrogen_dioxide,
            aq.sulphur_dioxide,
            aq.ozone,
            aq.dust,
            aq.uv_index
        FROM weather_district_boundaries b
        LEFT JOIN open_meteo_actual m ON ST_Contains(b.geometry, m.geom)
        LEFT JOIN open_weather_actual w ON ST_Contains(b.geometry, w.geom)
        LEFT JOIN weatherapi_actual a ON ST_Contains(b.geometry, a.geom)
        LEFT JOIN open_meteo_air_quality_actual aq ON ST_Contains(b.geometry, aq.geom)
        """;

    public List<WeatherActualDto> findAllActual() {
        String sql = baseSql + " ORDER BY b.district_name ASC";
        return jdbcTemplate.query(sql, rowMapper);
    }

    public Optional<WeatherActualDto> findByDistrictName(String districtName) {
        String sql = baseSql + " WHERE LOWER(b.district_name) = LOWER(?)";
        List<WeatherActualDto> results = jdbcTemplate.query(sql, rowMapper, districtName);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }
}
