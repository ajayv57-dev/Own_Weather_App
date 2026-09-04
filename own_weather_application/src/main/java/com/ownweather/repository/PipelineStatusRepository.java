package com.ownweather.repository;

import com.ownweather.model.PipelineStatusDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;

@Repository
public class PipelineStatusRepository {

    private final JdbcTemplate jdbcTemplate;

    public PipelineStatusRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<PipelineStatusDto> getRecentPipelineRuns() {
        String sql = """
            SELECT id, type, "Vendor" AS vendor, source, updated_time
            FROM processed_data
            ORDER BY updated_time DESC, id DESC
            LIMIT 25
            """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Timestamp ts = rs.getTimestamp("updated_time");
            return new PipelineStatusDto(
                    rs.getInt("id"),
                    rs.getString("type"),
                    rs.getString("vendor"),
                    rs.getString("source"),
                    ts != null ? ts.toLocalDateTime() : null
            );
        });
    }
}
