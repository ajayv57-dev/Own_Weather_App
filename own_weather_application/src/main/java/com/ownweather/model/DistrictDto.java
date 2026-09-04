package com.ownweather.model;

public class DistrictDto {
    private String districtName;
    private Double centroidLat;
    private Double centroidLon;
    private Long dtCode11;
    private Long distLgd;

    public DistrictDto() {}

    public DistrictDto(String districtName, Double centroidLat, Double centroidLon, Long dtCode11, Long distLgd) {
        this.districtName = districtName;
        this.centroidLat = centroidLat;
        this.centroidLon = centroidLon;
        this.dtCode11 = dtCode11;
        this.distLgd = distLgd;
    }

    public String getDistrictName() { return districtName; }
    public void setDistrictName(String districtName) { this.districtName = districtName; }

    public Double getCentroidLat() { return centroidLat; }
    public void setCentroidLat(Double centroidLat) { this.centroidLat = centroidLat; }

    public Double getCentroidLon() { return centroidLon; }
    public void setCentroidLon(Double centroidLon) { this.centroidLon = centroidLon; }

    public Long getDtCode11() { return dtCode11; }
    public void setDtCode11(Long dtCode11) { this.dtCode11 = dtCode11; }

    public Long getDistLgd() { return distLgd; }
    public void setDistLgd(Long distLgd) { this.distLgd = distLgd; }
}
