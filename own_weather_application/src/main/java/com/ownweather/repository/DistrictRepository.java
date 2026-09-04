package com.ownweather.repository;

import com.ownweather.model.DistrictDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class DistrictRepository {

    private final JdbcTemplate jdbcTemplate;

    public DistrictRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<DistrictDto> findAll() {
        String sql = """
            SELECT district_name, centroid_lat, centroid_lon, dtcode11, dist_lgd
            FROM weather_district_boundaries
            ORDER BY district_name ASC
            """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new DistrictDto(
                rs.getString("district_name"),
                rs.getDouble("centroid_lat"),
                rs.getDouble("centroid_lon"),
                rs.getLong("dtcode11"),
                rs.getLong("dist_lgd")
        ));
    }

    public String getGeoJsonFeatureCollection() {
        String sql = """
            SELECT json_build_object(
                'type', 'FeatureCollection',
                'features', json_agg(
                    json_build_object(
                        'type', 'Feature',
                        'id', district_name,
                        'geometry', ST_AsGeoJSON(geometry)::json,
                        'properties', json_build_object(
                            'district_name', district_name,
                            'centroid_lat', centroid_lat,
                            'centroid_lon', centroid_lon,
                            'dtcode11', dtcode11,
                            'dist_lgd', dist_lgd
                        )
                    )
                )
            )::text AS geojson
            FROM weather_district_boundaries
            """;
        return jdbcTemplate.queryForObject(sql, String.class);
    }
}
