package com.ownweather.model;

import java.time.LocalDateTime;

public class PipelineStatusDto {
    private int id;
    private String type;
    private String vendor;
    private String source;
    private LocalDateTime updatedTime;

    public PipelineStatusDto() {}

    public PipelineStatusDto(int id, String type, String vendor, String source, LocalDateTime updatedTime) {
        this.id = id;
        this.type = type;
        this.vendor = vendor;
        this.source = source;
        this.updatedTime = updatedTime;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getVendor() { return vendor; }
    public void setVendor(String vendor) { this.vendor = vendor; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public LocalDateTime getUpdatedTime() { return updatedTime; }
    public void setUpdatedTime(LocalDateTime updatedTime) { this.updatedTime = updatedTime; }
}
