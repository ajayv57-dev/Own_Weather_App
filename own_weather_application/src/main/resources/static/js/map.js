/**
 * Tamil Nadu Weather Intelligence - Interactive Map (Leaflet.js)
 * PostGIS Boundary Choropleth with Zoom-Responsive District Name Labels
 */

let mapInstance = null;
let geoJsonLayer = null;
let labelsLayer = null;
let currentMetric = 'temp'; // 'temp' | 'rain' | 'wind' | 'aqi'
let actualDataMap = new Map(); // districtName -> WeatherActualDto
let districtCentroids = new Map(); // districtName.toLowerCase() -> {lat, lon}
let onDistrictSelectCallback = null;
let selectedDistrict = 'Chennai';

const WeatherMap = {
    init(containerId, onDistrictSelect) {
        onDistrictSelectCallback = onDistrictSelect;

        // Centered over Tamil Nadu
        mapInstance = L.map(containerId, {
            center: [11.05, 78.65],
            zoom: 7,
            minZoom: 6,
            maxZoom: 12,
            zoomControl: false
        });

        L.control.zoom({ position: 'bottomright' }).addTo(mapInstance);

        // High-contrast Dark Basemap (100% Free, No API Key Required, Zero Watermark)
        L.tileLayer('https://server.arcgisonline.com/ArcGIS/rest/services/Canvas/World_Dark_Gray_Base/MapServer/tile/{z}/{y}/{x}', {
            attribution: 'Tiles &copy; Esri &mdash; Esri, DeLorme, NAVTEQ',
            maxZoom: 16
        }).addTo(mapInstance);

        labelsLayer = L.layerGroup().addTo(mapInstance);

        // Zoom responsive class handling
        mapInstance.on('zoomend', () => this.updateZoomClasses());
        this.updateZoomClasses();

        return mapInstance;
    },

    updateZoomClasses() {
        if (!mapInstance) return;
        const container = mapInstance.getContainer();
        const zoom = mapInstance.getZoom();

        container.classList.remove('zoom-low', 'zoom-mid', 'zoom-high', 'zoom-max');
        if (zoom <= 7) {
            container.classList.add('zoom-low');
        } else if (zoom === 8) {
            container.classList.add('zoom-mid');
        } else if (zoom === 9) {
            container.classList.add('zoom-high');
        } else {
            container.classList.add('zoom-max');
        }
    },

    setActualData(actualList) {
        actualDataMap.clear();
        actualList.forEach(item => {
            if (item.districtName) {
                actualDataMap.set(item.districtName.toLowerCase(), item);
            }
        });
        this.updateLayerStyles();
        this.updateLabelsText();
    },

    loadGeoJson(geoJsonData) {
        if (geoJsonLayer && mapInstance) {
            mapInstance.removeLayer(geoJsonLayer);
        }
        if (labelsLayer) {
            labelsLayer.clearLayers();
        }

        districtCentroids.clear();

        // 1. Polygon Layer
        geoJsonLayer = L.geoJSON(geoJsonData, {
            style: feature => this.getFeatureStyle(feature),
            onEachFeature: (feature, layer) => {
                const districtName = feature.properties.district_name;
                const lat = feature.properties.centroid_lat;
                const lon = feature.properties.centroid_lon;

                if (lat && lon) {
                    districtCentroids.set(districtName.toLowerCase(), { lat, lon });

                    // 2. Permanent District Name Label at Centroid
                    const isSelected = selectedDistrict && selectedDistrict.toLowerCase() === districtName.toLowerCase();
                    const marker = L.marker([lat, lon], {
                        icon: L.divIcon({
                            className: 'district-map-label-wrapper',
                            html: `<span class="district-map-label ${isSelected ? 'active-label' : ''}" id="label-${districtName.replace(/\s+/g, '')}">${districtName}</span>`,
                            iconSize: null
                        }),
                        interactive: true,
                        zIndexOffset: 1000
                    });

                    marker.on('click', () => {
                        this.setSelectedDistrict(districtName);
                        if (onDistrictSelectCallback) {
                            onDistrictSelectCallback(districtName);
                        }
                    });

                    labelsLayer.addLayer(marker);
                }

                // Hover Tooltip with Live Metrics
                layer.bindTooltip(() => {
                    const data = actualDataMap.get(districtName.toLowerCase());
                    let valStr = '--';
                    if (data) {
                        if (currentMetric === 'temp') valStr = `${data.consensusTemperature ?? '--'}°C`;
                        else if (currentMetric === 'rain') valStr = `${data.openMeteoPrecipitation ?? 0} mm`;
                        else if (currentMetric === 'wind') valStr = `${data.openMeteoWindSpeed ?? '--'} km/h`;
                        else if (currentMetric === 'aqi') valStr = `AQI: ${data.aqi ?? '--'}`;
                        else if (currentMetric === 'uv') valStr = `UV Index: ${data.uvIndex ?? data.weatherApiUv ?? '--'}`;
                    }
                    return `<div class="custom-tooltip"><strong>${districtName}</strong><br/>${valStr}</div>`;
                }, { sticky: true, className: 'custom-tooltip-wrap' });

                // Interactive polygon events
                layer.on({
                    mouseover: e => {
                        const l = e.target;
                        l.setStyle({
                            weight: 2.5,
                            color: '#ffffff',
                            fillOpacity: 0.88
                        });
                    },
                    mouseout: e => {
                        if (geoJsonLayer) {
                            geoJsonLayer.resetStyle(e.target);
                        }
                    },
                    click: () => {
                        this.setSelectedDistrict(districtName);
                        if (onDistrictSelectCallback) {
                            onDistrictSelectCallback(districtName);
                        }
                    }
                });
            }
        }).addTo(mapInstance);
    },

    setSelectedDistrict(name, fly = false) {
        selectedDistrict = name;
        this.updateLayerStyles();

        // Update active class on labels
        document.querySelectorAll('.district-map-label').forEach(el => {
            el.classList.remove('active-label');
        });
        const activeLabel = document.getElementById(`label-${name.replace(/\s+/g, '')}`);
        if (activeLabel) {
            activeLabel.classList.add('active-label');
        }

        if (fly && mapInstance) {
            const centroid = districtCentroids.get(name.toLowerCase());
            if (centroid) {
                mapInstance.flyTo([centroid.lat, centroid.lon], Math.max(mapInstance.getZoom(), 8.5), {
                    duration: 1.0
                });
            }
        }
    },

    setMetric(metric) {
        currentMetric = metric;
        this.updateLayerStyles();
        this.updateLegendUI(metric);
        this.updateLabelsText();
    },

    updateLabelsText() {
        const zoom = mapInstance ? mapInstance.getZoom() : 7;
        // On high zoom (>=9), show district name + current metric value
        if (zoom >= 9) {
            districtCentroids.forEach((coords, key) => {
                const data = actualDataMap.get(key);
                if (!data) return;
                const safeId = `label-${data.districtName.replace(/\s+/g, '')}`;
                const el = document.getElementById(safeId);
                if (el) {
                    let metricTag = '';
                    if (currentMetric === 'temp') metricTag = ` (${data.consensusTemperature ?? '--'}°)`;
                    else if (currentMetric === 'rain') metricTag = ` (${data.openMeteoPrecipitation ?? 0}mm)`;
                    else if (currentMetric === 'wind') metricTag = ` (${data.openMeteoWindSpeed ?? '--'}k)`;
                    else if (currentMetric === 'aqi') metricTag = ` (AQI ${data.aqi ?? '--'})`;
                    else if (currentMetric === 'uv') metricTag = ` (UV ${data.uvIndex ?? data.weatherApiUv ?? '--'})`;
                    el.textContent = `${data.districtName}${metricTag}`;
                }
            });
        }
    },

    getFeatureStyle(feature) {
        const districtName = feature.properties.district_name;
        const isSelected = selectedDistrict && selectedDistrict.toLowerCase() === districtName.toLowerCase();
        const data = actualDataMap.get(districtName.toLowerCase());
        const color = this.getColorForDistrict(data);

        return {
            fillColor: color,
            weight: isSelected ? 3.5 : 1.2,
            opacity: 1,
            color: isSelected ? '#38bdf8' : 'rgba(255, 255, 255, 0.25)',
            fillOpacity: isSelected ? 0.92 : 0.72
        };
    },

    updateLayerStyles() {
        if (!geoJsonLayer) return;
        geoJsonLayer.eachLayer(layer => {
            if (layer.feature) {
                layer.setStyle(this.getFeatureStyle(layer.feature));
            }
        });
    },

    getColorForDistrict(data) {
        if (!data) return '#1e293b';

        if (currentMetric === 'temp') {
            const t = data.consensusTemperature ?? 30;
            if (t < 26) return '#38bdf8'; // cool
            if (t < 30) return '#34d399'; // mild
            if (t < 33) return '#fbbf24'; // warm
            if (t < 36) return '#f87171'; // hot
            return '#ef4444';             // very hot
        } else if (currentMetric === 'rain') {
            const r = data.openMeteoPrecipitation ?? 0;
            if (r === 0) return '#1e293b';
            if (r < 2) return '#7dd3fc';
            if (r < 10) return '#38bdf8';
            if (r < 25) return '#2563eb';
            return '#1e40af';
        } else if (currentMetric === 'wind') {
            const w = data.openMeteoWindSpeed ?? 10;
            if (w < 8) return '#6ee7b7';
            if (w < 15) return '#34d399';
            if (w < 25) return '#fbbf24';
            if (w < 35) return '#f97316';
            return '#ef4444';
        } else if (currentMetric === 'aqi') {
            const a = data.aqi ?? 25;
            if (a < 20) return '#10b981'; // good
            if (a < 35) return '#34d399'; // fair
            if (a < 50) return '#f59e0b'; // moderate
            if (a < 75) return '#f97316'; // poor
            return '#ef4444';             // very poor
        } else if (currentMetric === 'uv') {
            const u = data.uvIndex ?? data.weatherApiUv ?? 6;
            if (u < 3) return '#10b981';  // Low (Green)
            if (u < 6) return '#facc15';  // Moderate (Yellow)
            if (u < 8) return '#f97316';  // High (Orange)
            if (u < 11) return '#ef4444'; // Very High (Red)
            return '#a855f7';             // Extreme (Purple)
        }
        return '#3b82f6';
    },

    updateLegendUI(metric) {
        const titleEl = document.getElementById('legendTitle');
        const labelsEl = document.getElementById('legendLabels');
        const barEl = document.getElementById('legendGradientBar');
        const captionEl = document.getElementById('mapSubCaption');

        if (!titleEl || !labelsEl || !barEl) return;

        if (metric === 'temp') {
            titleEl.textContent = 'Temperature Scale (°C)';
            if (captionEl) captionEl.textContent = 'Showing Current Temperature (°C)';
            barEl.style.background = 'linear-gradient(90deg, #38bdf8, #34d399, #fbbf24, #f87171, #ef4444)';
            labelsEl.innerHTML = '<span>24°C</span><span>28°C</span><span>32°C</span><span>36°C</span><span>40°C+</span>';
        } else if (metric === 'rain') {
            titleEl.textContent = 'Precipitation Scale (mm)';
            if (captionEl) captionEl.textContent = 'Showing Precipitation (mm)';
            barEl.style.background = 'linear-gradient(90deg, #1e293b, #7dd3fc, #38bdf8, #2563eb, #1e40af)';
            labelsEl.innerHTML = '<span>0 mm</span><span>2 mm</span><span>10 mm</span><span>25 mm</span><span>50 mm+</span>';
        } else if (metric === 'wind') {
            titleEl.textContent = 'Wind Speed Scale (km/h)';
            if (captionEl) captionEl.textContent = 'Showing Wind Speed (km/h)';
            barEl.style.background = 'linear-gradient(90deg, #6ee7b7, #34d399, #fbbf24, #f97316, #ef4444)';
            labelsEl.innerHTML = '<span>5 km/h</span><span>12 km/h</span><span>20 km/h</span><span>30 km/h</span><span>40 km/h+</span>';
        } else if (metric === 'aqi') {
            titleEl.textContent = 'Air Quality Index (European AQI)';
            if (captionEl) captionEl.textContent = 'Showing European AQI Rating';
            barEl.style.background = 'linear-gradient(90deg, #10b981, #34d399, #f59e0b, #f97316, #ef4444)';
            labelsEl.innerHTML = '<span>Good (10)</span><span>Fair (25)</span><span>Moderate (40)</span><span>Poor (60)</span><span>Hazardous (80+)</span>';
        } else if (metric === 'uv') {
            titleEl.textContent = 'Ultraviolet Index (WHO Solar Radiation Standard)';
            if (captionEl) captionEl.textContent = 'Showing Real-Time Solar UV Radiation Index';
            barEl.style.background = 'linear-gradient(90deg, #10b981, #facc15, #f97316, #ef4444, #a855f7)';
            labelsEl.innerHTML = '<span>0-2 Low</span><span>3-5 Mod</span><span>6-7 High</span><span>8-10 V.High</span><span>11+ Extreme</span>';
        }
    }
};

window.WeatherMap = WeatherMap;
