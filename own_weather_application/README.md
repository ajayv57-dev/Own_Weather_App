# 🌦️ Tamil Nadu Weather & Environmental Intelligence Platform
## Comprehensive Technical Architecture, Operational Manual & Engineering Guide

An enterprise-grade, distributed meteorological telemetry, environmental analysis, and spatial forecasting platform engineered with **Java 21**, **Spring Boot 3.4.0**, **PostgreSQL 18 + PostGIS**, **HashiCorp Vault**, and an interactive **Glassmorphic Leaflet.js Frontend**.

The platform monitors and forecasts atmospheric conditions across all **38 Districts of Tamil Nadu**, cross-referencing real-time observations from multiple global meteorological providers (**Open-Meteo**, **OpenWeather**, **WeatherAPI**) and scientific numerical weather prediction models (**NCUM / NCMRWF**).

---

## 📑 Table of Contents

1. [Platform Overview & Key Capabilities](#-platform-overview--key-capabilities)
2. [End-to-End System Architecture](#-end-to-end-system-architecture)
3. [Database & PostGIS Spatial Layer Architecture](#-database--postgis-spatial-layer-architecture)
   - [Spatial Join Resolution vs. Transliteration Mismatches](#spatial-join-resolution-vs-transliteration-mismatches)
   - [Database Table Schema & Categorization](#database-table-schema--categorization)
4. [HashiCorp Vault Security Architecture](#-hashicorp-vault-security-architecture)
   - [Zero-Touch Isolation Policy](#zero-touch-isolation-policy)
   - [Secret Key-Value Schema](#secret-key-value-schema)
   - [Dynamic Datasource Bootstrapping & Fallback](#dynamic-datasource-bootstrapping--fallback)
5. [Interactive User Interface & Visualization Engine](#-interactive-user-interface--visualization-engine)
   - [Top-Level Mode 1: Actual Weather View](#top-level-mode-1-actual-weather-view)
   - [Top-Level Mode 2: Forecast Weather View](#top-level-mode-2-forecast-weather-view)
     - [Day-Wise 24-Hour Forecast Breakdown](#day-wise-24-hour-forecast-breakdown)
     - [Combined Date Range Analytics ("From Date — To Date")](#combined-date-range-analytics-from-date--to-date)
   - [Interactive PostGIS Map & Basemap Solution](#interactive-postgis-map--basemap-solution)
     - [The "API Key Required" Watermark Root Cause & Esri Canvas Fix](#the-api-key-required-watermark-root-cause--esri-canvas-fix)
     - [Centroid District Labels & Zoom-Responsive Scaling](#centroid-district-labels--zoom-responsive-scaling)
     - [Dynamic Metric Choropleth & WHO UV Radiation Ramp](#dynamic-metric-choropleth--who-uv-radiation-ramp)
     - [Bi-Directional District Synchronization](#bi-directional-district-synchronization)
6. [Comprehensive REST API Reference](#-comprehensive-rest-api-reference)
7. [Directory Structure & Module Breakdown](#-directory-structure--module-breakdown)
8. [Build, Deployment & Verification Guide](#-build-deployment--verification-guide)
9. [Operational Troubleshooting & FAQ](#-operational-troubleshooting--faq)

---

## 🌟 Platform Overview & Key Capabilities

The Tamil Nadu Weather & Environmental Intelligence Platform serves critical situational awareness needs by aggregating, validating, and presenting geospatial weather data. Key capabilities include:

* **Dual Primary Operating Modes**: Switch between instantaneous real-time conditions (**Actual**) and multi-tier predictive horizons (**Forecast**).
* **Multi-Tier Predictive Horizons**:
  * **Day-Wise 24-Hour Horizon**: Granular hourly telemetry curves (temperature, precipitation, wind speed, humidity) for any day between Day 0 (today) and Day 7.
  * **Combined Date Range Analytics**: User-defined temporal windows (e.g., *Sep 3 to Sep 7*) calculating cumulative rainfall, temperature envelopes (min/mean/max), peak wind gusts, and daily trajectory breakdowns.
* **100% PostGIS Geospatial Integrity**: Real-time conversion of 38 Tamil Nadu district administrative polygon boundaries into GeoJSON features via PostGIS `ST_AsGeoJSON(geometry)`.
* **Zero Transliteration Mismatch**: Uses PostGIS spatial containment queries (`ST_Contains`) instead of brittle text matching to guarantee 100% multi-vendor observation coverage across all 38 districts.
* **Keyless, Watermark-Free Cartography**: Powered by Esri World Dark Gray Canvas raster tiles, eliminating third-party API key dependencies and watermark degradation.
* **Automated Centroid Labeling & Zoom Responsiveness**: Permanent district labels positioned at geometric centroids (`ST_Centroid`) that dynamically scale font size, capsules, and metadata according to map zoom level.
* **Comprehensive Environmental & UV Matrix**: Tracks atmospheric chemistry ($\text{PM}_{2.5}$, $\text{NO}_2$, $\text{SO}_2$, $\text{CO}$, $\text{O}_3$, European AQI) and maps UV radiation on the World Health Organization (WHO) safety scale.
* **Enterprise Secret Isolation**: Integrates natively with HashiCorp Vault to dynamically inject database credentials and configurations at startup while preserving strict isolation boundaries.

---

## 🏛️ End-to-End System Architecture

```
                               DATA INGESTION PIPELINE
                  ┌──────────────────────────────────────────────┐
                  │           Weather Data Sources               │
                  │  • Open-Meteo API (ECMWF / DWD ICON)         │
                  │  • OpenWeather API (Global Multi-Model)      │
                  │  • WeatherAPI (NWS / MetOffice Feeds)        │
                  └──────────────────────┬───────────────────────┘
                                         │
                                         ▼
                  ┌──────────────────────────────────────────────┐
                  │            Apache NiFi Pipeline              │
                  │  • Scheduled InvokeHTTP Data Retrieval       │
                  │  • JoltTransformJSON & Schema Validation     │
                  │  • PutDatabaseRecord & Spatial Point Insert  │
                  └──────────────────────┬───────────────────────┘
                                         │
                                         ▼
                  ┌──────────────────────────────────────────────┐
                  │      PostgreSQL 18 + PostGIS (Own_Weather)   │
                  │  • weather_district_boundaries (38 Polygons) │
                  │  • Multi-Vendor Observation & AQ Tables      │
                  │  • 8-Day Rolling Forecast & 24h Hourly Slices│
                  │  • processed_data (Sync & Audit Log)         │
                  └──────────────────────┬───────────────────────┘
                                         │
                   ┌─────────────────────┴──────────────────────┐
                   │                                            │
                   ▼                                            ▼
    ┌──────────────────────────────┐             ┌──────────────────────────────┐
    │   HashiCorp Vault (Port 8200)│             │     Spring Boot 3.4.0 API    │
    │ • Path: secret/local/Weather/│             │ • VaultConfigService         │
    │   weather.json               │────────────▶│ • PostGIS Spatial Repositories│
    │ • Dynamic DB Credentials     │   Startup   │ • REST Controller (/api/v1/..)│
    │ • Zero-Touch Isolation Policy│  Bootstrap  │ • Embedded Tomcat & HikariCP │
    └──────────────────────────────┘             └──────────────┬───────────────┘
                                                                │
                                                                ▼
                                                 ┌──────────────────────────────┐
                                                 │  Modern Interactive Frontend │
                                                 │ • Obsidian Glassmorphism UI  │
                                                 │ • Leaflet.js + Esri Canvas   │
                                                 │ • 38 District Centroid Labels│
                                                 │ • Day-Wise 24h Hourly Curves │
                                                 │ • Combined Range Analytics   │
                                                 │ • WHO UV & Multi-Metric Ramps│
                                                 └──────────────────────────────┘
```

### Architectural Flow:
1. **Ingestion Layer**: External weather providers deliver observations and forecasts via REST. Ingestion processors (e.g. Apache NiFi) transform payloads and write them to PostgreSQL tables, populating geographic coordinate points (`geom = ST_SetSRID(ST_Point(lon, lat), 4326)`).
2. **Persistence & Spatial Layer**: PostgreSQL 18 with PostGIS manages 72 tables, containing exact MultiPolygon boundaries for all 38 districts alongside hourly and multi-day forecasts.
3. **Security & Configuration Layer**: During the Spring Boot application initialization, `VaultDataSourceConfig` queries HashiCorp Vault at `http://127.0.0.1:8200` to retrieve database credentials and application settings.
4. **Backend Application Layer**: Spring Boot 3 with Java 21 executes spatial SQL queries (using `ST_Contains` and `ST_AsGeoJSON`) via Spring JDBC, serving clean JSON representations to the frontend.
5. **Presentation Layer**: A dark obsidian glassmorphic single-page application (SPA) renders interactive maps, metric choropleths, Chart.js dual-axis graphs, and multi-vendor comparison tables.

---

## 🗄️ Database & PostGIS Spatial Layer Architecture

The database runs on **PostgreSQL 18** with the **PostGIS** spatial extension enabled on database `Own_Weather`.

### Spatial Join Resolution vs. Transliteration Mismatches

A common challenge in meteorological systems is discrepancies between administrative boundary names and commercial API station names. In Tamil Nadu, several districts have divergent spellings:

| Official District Boundary Name | Commercial API Provider Name |
| :--- | :--- |
| **Thoothukudi** | *Tuticorin* |
| **Kancheepuram** | *Kanchipuram* |
| **Viluppuram** | *Villupuram* |
| **Kanniyakumari** | *Kanyakumari* |
| **Tiruchirappalli** | *Tiruchchirappalli* / *Trichy* |
| **Mayiladuthurai** | *Mayiladuturai* |

#### The Flawed Approach (Text Equality):
```sql
-- Fails for 6 out of 38 districts due to transliteration differences (Only 84% accuracy)
SELECT * FROM weather_district_boundaries b 
JOIN open_meteo_actual m ON LOWER(b.district_name) = LOWER(m.district_name);
```

#### The PostGIS Solution (Point-in-Polygon Containment):
```sql
-- Succeeds for 38 out of 38 districts (100% accuracy)
SELECT 
    b.district_name,
    b.centroid_lat,
    b.centroid_lon,
    m.temperature_c,
    m.humidity_pct,
    m.wind_speed_kmh
FROM weather_district_boundaries b
LEFT JOIN open_meteo_actual m 
       ON ST_Contains(b.geometry, m.geom);
```
By indexing `b.geometry` with a **GIST** index (`weather_district_boundaries_geometry_gix`) and verifying that the observation coordinate point falls physically inside the district polygon boundary, text transliteration mismatches are bypassed entirely.

---

### Database Table Schema & Categorization

The database organizes 72 tables into five logical functional tiers:

```
                              DATABASE TABLE FAMILIES
                                  (Own_Weather)
                                         │
        ┌────────────────────────────────┼────────────────────────────────┐
        │                                │                                │
        ▼                                ▼                                ▼
┌──────────────────────┐      ┌──────────────────────┐      ┌──────────────────────┐
│  Spatial Boundaries  │      │ Current Observations │      │ Atmospheric Quality  │
│ • weather_district_  │      │ • open_meteo_actual  │      │ • open_meteo_air_    │
│   boundaries         │      │ • open_weather_actual│      │   quality_actual     │
│   (38 Polygons, GIST)│      │ • weatherapi_actual  │      │   (AQI, PM2.5, NO2,  │
│                      │      │ • ncum_actual        │      │    SO2, CO, O3, UV)  │
└──────────────────────┘      └──────────────────────┘      └──────────────────────┘
                                         │
        ┌────────────────────────────────┴────────────────────────────────┐
        │                                                                 │
        ▼                                                                 ▼
┌──────────────────────────────────────┐        ┌──────────────────────────────────────┐
│       8-Day Rolling Forecasts        │        │       Ingestion & Audit Status       │
│ • open_meteo_forecast (Day 0 - 7)    │        │ • processed_data                     │
│ • open_weather_forecast              │        │   (vendor, sync timestamp, record    │
│ • open_meteo_forecast_hourly_0 .. 7  │        │    counts, status flag)              │
│   (24-hour denormalized columns)     │        │                                      │
└──────────────────────────────────────┘        └──────────────────────────────────────┘
```

#### 1. Spatial Boundaries (`weather_district_boundaries`)
* **Row Count**: 38 rows (Tamil Nadu's 38 revenue districts).
* **Columns**: `district_code` (VARCHAR PK), `district_name` (VARCHAR), `geometry` (GEOMETRY MultiPolygon, SRID 4326), `centroid_lat` (NUMERIC), `centroid_lon` (NUMERIC), `area_sq_km` (NUMERIC).
* **Spatial Method**: Polygons are serialized on demand to GeoJSON via `ST_AsGeoJSON(geometry)` in `DistrictRepository.java`.

#### 2. Multi-Vendor Real-Time Observations (`*_actual`)
* Tables: `open_meteo_actual`, `open_weather_actual`, `weatherapi_actual`, `ncum_actual`.
* Schema: `district_name`, `temperature_c`, `humidity_pct`, `wind_speed_kmh`, `wind_direction_deg`, `precipitation_mm`, `geom` (Point geometry), `created_at`.

#### 3. Air Quality & Chemical Telemetry (`*_air_quality_actual`)
* Table: `open_meteo_air_quality_actual`.
* Schema: `district_name`, `european_aqi`, `pm2_5`, `carbon_monoxide`, `nitrogen_dioxide`, `sulphur_dioxide`, `ozone`, `uv_index`, `dust`, `geom`.

#### 4. Forecast Summary & 24-Hour Hourly Slices
* **Daily Aggregates (`*_forecast`)**: Contains rows for `day_index` 0 (Today) through 7 (7 days ahead), tracking `min_temperature_c`, `max_temperature_c`, `mean_temperature_c`, `precipitation_sum_mm`, and `max_wind_speed_kmh`.
* **Hourly Tables (`*_forecast_hourly_0` to `*_forecast_hourly_7`)**: Denormalized wide tables with columns `hour0` through `hour23` for each meteorological metric (`temp_hour0`...`temp_hour23`, `rain_hour0`...`rain_hour23`), enabling instantaneous single-query retrievals of 24-hour timelines without heavy table scans.

#### 5. Audit & Ingestion Synchronization (`processed_data`)
* Schema: `Vendor`, `type` (`actual` vs `forecast`), `source` table, `updated_time`. Used by health checks and UI status indicators.

---

## 🔐 HashiCorp Vault Security Architecture

To meet enterprise security standards, database credentials, API keys, and spatial defaults are externalized from configuration files into a local **HashiCorp Vault** instance running at `http://127.0.0.1:8200`.

### Zero-Touch Isolation Policy

```
Vault Engine (http://127.0.0.1:8200)
 └── secret/local/
      ├── Predective/
      │    └── Predective.json   ◀── [STRICT ZERO-TOUCH BOUNDARY] (Unmodified & Preserved)
      │
      └── Weather/
           └── weather.json      ◀── [WEATHER PLATFORM SECRETS] (Created & Managed)
```

The application strictly isolates its operational parameters under the dedicated secret path:  
📍 `secret/local/Weather/weather.json`

Existing credentials in other namespaces (such as `secret/local/Predective/Predective.json`) remain untouched.

---

### Secret Key-Value Schema

The JSON document stored at `secret/local/Weather/weather.json` contains:

```json
{
  "spring.datasource.url": "jdbc:postgresql://localhost:5432/Own_Weather",
  "spring.datasource.username": "readonly_user",
  "spring.datasource.password": "viewer1",
  "spring.datasource.driver-class-name": "org.postgresql.Driver",
  "spring.datasource.hikari.maximum-pool-size": "10",
  "spring.datasource.hikari.minimum-idle": "2",

  "weather.api.openweather.token": "ow_live_api_token_secure",
  "weather.api.weatherapi.token": "wa_live_api_token_secure",
  "weather.api.openmeteo.base-url": "https://api.open-meteo.com/v1",

  "app.weather.default-district": "Chennai",
  "app.weather.default-vendor": "open_meteo",
  "app.weather.state-name": "Tamil Nadu",
  "app.weather.map.center-lat": "11.05",
  "app.weather.map.center-lon": "78.65",
  "app.weather.map.zoom": "7",
  "app.weather.map.tile-layer-url": "https://server.arcgisonline.com/ArcGIS/rest/services/Canvas/World_Dark_Gray_Base/MapServer/tile/{z}/{y}/{x}",

  "pipeline.nifi.endpoint": "http://localhost:8443/nifi-api",
  "pipeline.kafka.bootstrap-servers": "localhost:9092",
  "pipeline.kafka.topic.weather": "weather.telemetry.raw"
}
```

---

### Dynamic Datasource Bootstrapping & Fallback

The application initializes its database connection using a custom `VaultDataSourceConfig` class:

```
[Application Startup]
        │
        ▼
[Query Vault at http://127.0.0.1:8200]
        │
        ├──▶ (Success) ──▶ Parse JSON from secret/local/Weather/weather.json
        │                   └── Inject DB URL, Username, Password into HikariDataSource
        │
        └──▶ (Fallback) ─▶ Vault Offline / Unreachable?
                            └── Read static credentials from application.yml
```

This dual-layer mechanism ensures that the application boots in air-gapped or test environments while taking advantage of centralized secret management in production.

---

## 🖥️ Interactive User Interface & Visualization Engine

The user interface is built as a single-page application utilizing **Obsidian Glassmorphism** styling, featuring high-contrast typography, translucent containers (`backdrop-filter: blur(16px)`), and responsive CSS grid systems.

```
┌──────────────────────────────────────────────────────────────────────────────────────────────────┐
│  🌦️ TAMIL NADU WEATHER INTELLIGENCE              [Actual Weather]  [Forecast Weather]            │
│  District: [ Chennai         ▾ ]   Vendor: [ Open-Meteo ▾ ]   ● LIVE PIPELINE                    │
├──────────────────────────────────────────────────┬───────────────────────────────────────────────┤
│                                                  │                                               │
│                 LEAFLET POSTGIS MAP              │             DYNAMIC TELEMETRY PANEL           │
│                                                  │                                               │
│   Layers: [Temp] [Rain] [Wind] [AQI] [UV]        │   [ACTUAL MODE]                               │
│                                                  │   • Statewide KPIs (Avg, High, Low, Rain)     │
│   ┌──────────────────────────────────────────┐   │   • Selected District Hero Card               │
│   │                                          │   │   • Multi-Vendor Consensus Table              │
│   │               [Chennai (35.2°)]          │   │   • Atmospheric Pollutants & UV Gauge        │
│   │                                          │   │                                               │
│   │   [Salem]               [Cuddalore]      │   │   [FORECAST MODE]                             │
│   │                                          │   │   Sub-mode: (•) Day-Wise   ( ) Combined Range │
│   │                                          │   │   • Day Selector: [Today] [Tomorrow] [Day 2]..│
│   │               [Madurai]                  │   │   • 24h Dual-Axis Chart (Temp Line + Rain Bar)│
│   │                                          │   │   • 24-Card Horizontal Hourly Progression     │
│   │                                          │   │   • Combined Range: [From Date] ➔ [To Date]   │
│   │   [Tirunelveli]                          │   │   • Envelope Aggregates (Cumulative Rain,     │
│   │                                          │   │     Min/Mean/Max Temp, Peak Wind Gust)        │
│   └──────────────────────────────────────────┘   │   • Daily Trend Breakdown Table               │
│   Tile Provider: Esri Dark Gray (Keyless, Clean) │                                               │
└──────────────────────────────────────────────────┴───────────────────────────────────────────────┘
```

---

### Top-Level Mode 1: Actual Weather View

Activating the **`[Actual Weather]`** button renders instantaneous ground conditions:

1. **Statewide Weather Intelligence Cards**:
   * **State Average Temperature**: Aggregated mean temperature across all 38 districts.
   * **Hottest District**: Real-time maximum temperature district identifier and value.
   * **Coolest District**: Real-time minimum temperature district identifier and value.
   * **Districts with Rain**: Real-time count of districts with precipitation $> 0.0\text{ mm}$.
2. **Selected District Hero Display**:
   * Primary temperature reading with high-contrast font.
   * Weather condition badge (e.g., *Clear Sky*, *Light Rain*, *Breezy*).
   * Secondary indicators: Relative Humidity (%), Wind Velocity (km/h), Wind Direction (°), and Precipitation (mm).
3. **Multi-Vendor Cross-Verification Table**:
   * Side-by-side comparison comparing **Open-Meteo**, **OpenWeather**, and **WeatherAPI**.
   * Identifies sensor variance and consensus readings across providers.
4. **Atmospheric Pollutant Matrix**:
   * Circular and linear progress indicators for European AQI, $\text{PM}_{2.5}$, $\text{NO}_2$, $\text{SO}_2$, $\text{CO}$, $\text{O}_3$, and UV Index.

---

### Top-Level Mode 2: Forecast Weather View

Activating the **`[Forecast Weather]`** button reveals a dedicated forecasting suite with two sub-operational modes:

#### Day-Wise 24-Hour Forecast Breakdown
* **Day Selector Pills**: Quick-selection buttons for **Today (Day 0)**, **Tomorrow (Day 1)**, through **Day 7**.
* **Dual-Axis 24-Hour Chart** (rendered with Chart.js):
  * Left Y-Axis: Temperature (°C) depicted as a continuous bezier curve.
  * Right Y-Axis: Precipitation (mm) depicted as vertical indigo bar columns.
  * Interactive tooltips displaying exact hourly values upon hover.
* **24-Card Horizontal Progression Rail**:
  * Horizontally scrollable timeline containing 24 individual cards (00:00 to 23:00).
  * Each card displays the hour, condition icon, temperature, rainfall accumulation, wind velocity, and humidity.

#### Combined Date Range Analytics ("From Date — To Date")
* **Range Controls**: Interactive dropdown selectors for **From Date** and **To Date**, dynamically populated from the database's available forecast dates.
* **Multi-Day Aggregate KPI Cards**:
  * **Cumulative Rainfall (mm)**: Total volume of rain expected over the selected window.
  * **Average Temperature (°C)**: Mean expected temperature over the duration.
  * **Temperature Envelope (Min / Max °C)**: The coldest low and hottest peak forecast across the range.
  * **Peak Wind Speed (km/h)**: Maximum expected wind velocity.
* **Multi-Day Envelope Trend Chart**:
  * Visualizes the trajectory of maximum, mean, and minimum temperatures across dates, paired with daily precipitation bars.
* **Daily Progression Trajectory Table**:
  * Comprehensive table listing Date, Day Name, Min Temp, Mean Temp, Max Temp, Total Rain, and Peak Wind.

---

### Interactive PostGIS Map & Basemap Solution

#### The "API Key Required" Watermark Root Cause & Esri Canvas Fix

| Attribute | Old Provider (CARTO Dark Matter) | New Solution (Esri World Dark Gray Canvas) |
| :--- | :--- | :--- |
| **URL Pattern** | `basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png` | `server.arcgisonline.com/.../World_Dark_Gray_Base/...` |
| **Policy Change** | CARTO initiated mandatory API key enforcement; unauthenticated requests render repeated **"API Key Required"** watermarks. | Esri provides open, public access for canvas basemaps with zero authentication tokens needed. |
| **Visual Quality** | Obscured by diagonal watermark text. | Crystal-clear, high-contrast dark gray palette with zero watermarks. |
| **Operational Cost** | Paid subscription required to remove watermarks. | **100% Free & Open**. |

The basemap URL is configured dynamically via Vault and `application.yml`:
```yaml
app:
  weather:
    map:
      tile-layer-url: "https://server.arcgisonline.com/ArcGIS/rest/services/Canvas/World_Dark_Gray_Base/MapServer/tile/{z}/{y}/{x}"
```

---

#### Centroid District Labels & Zoom-Responsive Scaling

To provide spatial orientation without map clutter, permanent labels are placed at the geographic centroid (`centroid_lat`, `centroid_lon`) of each district. A dynamic CSS class switcher listens to Leaflet's `zoomend` events and adjusts typography:

```
Zoom Level 6–7  ──▶ .zoom-low   (8.5px font, subtle text shadow, zero clutter)
Zoom Level 8    ──▶ .zoom-mid   (11px font, semi-transparent obsidian pill)
Zoom Level 9    ──▶ .zoom-high  (13px font, live temperature tag, e.g. "Chennai (35.2°)")
Zoom Level 10+  ──▶ .zoom-max   (15px bold font, glowing border, backdrop blur)
```

```javascript
// Dynamic CSS class adjustment based on zoom
function updateDistrictLabelClasses() {
    const zoom = map.getZoom();
    const container = document.getElementById('map');
    container.classList.remove('zoom-low', 'zoom-mid', 'zoom-high', 'zoom-max');
    
    if (zoom <= 7) container.classList.add('zoom-low');
    else if (zoom === 8) container.classList.add('zoom-mid');
    else if (zoom === 9) container.classList.add('zoom-high');
    else container.classList.add('zoom-max');
}
```

---

#### Dynamic Metric Choropleth & WHO UV Radiation Ramp

The map features five switchable layer modes, dynamically shading district polygons:

1. 🌡️ **Temperature (`temp`)**: Color scale ranging from Cool blue (`#38bdf8`) to Extreme red (`#ef4444`).
2. 🌧️ **Precipitation (`rain`)**: Shading from Dry (`#1e293b`) through Torrential blue (`#1e40af`).
3. 💨 **Wind Speed (`wind`)**: Shading from Gentle breeze (`#10b981`) to Gale warning (`#dc2626`).
4. 🛡️ **Air Quality Index (`aqi`)**: Shading from Good (`#10b981`) to Hazardous purple (`#7c3aed`).
5. ☀️ **UV Radiation Index (`uv`)**: Shaded according to the **World Health Organization (WHO)** ultraviolet radiation safety scale:

| UV Range | Risk Category | Color Hex | Visual Representation | Protective Action |
| :--- | :--- | :--- | :--- | :--- |
| **0.0 – 2.9** | **Low** | `#10b981` | Emerald Green | No protection required. Safe for normal exposure. |
| **3.0 – 5.9** | **Moderate** | `#facc15` | Golden Yellow | Seek shade during midday hours. Wear hat/sunglasses. |
| **6.0 – 7.9** | **High** | `#f97316` | Amber Orange | Sun protection essential. Reduce midday sun exposure. |
| **8.0 – 10.9** | **Very High**| `#ef4444` | Crimson Red | Extra precautions required. Minimize outdoor exposure. |
| **11.0+** | **Extreme** | `#a855f7` | Violet Purple | Take all precautions. Unprotected skin burns quickly. |

---

#### Bi-Directional District Synchronization

The interface maintains synchronization between dropdown selectors and spatial features:
* **Dropdown Selection**: Selecting a district from the header dropdown triggers `map.flyTo([centroid_lat, centroid_lon], 9)` and applies a glowing cyan highlight border to the district polygon.
* **Map Click**: Clicking any district polygon or label on the map selects that district, updates the header dropdown, pans the map, and reloads telemetry data across all active panels.

---

## 🔌 Comprehensive REST API Reference

All backend endpoints are rooted under `/api/v1/` and return JSON.

### Endpoint Catalog

| HTTP Method | Endpoint | Purpose | Query / Path Parameters |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/config/public` | Client configuration (map center, zoom, tile URL, default district) | None |
| `GET` | `/api/v1/districts` | Metadata for all 38 districts (code, name, centroid coordinates) | None |
| `GET` | `/api/v1/districts/geojson` | Complete GeoJSON FeatureCollection of 38 district polygons | None |
| `GET` | `/api/v1/actual/summary` | Real-time observation summary across all 38 districts | None |
| `GET` | `/api/v1/actual/districts/{name}` | Detailed single-district multi-vendor and pollutant readings | Path: `name` |
| `GET` | `/api/v1/forecast/available-dates`| List of available forecast dates and day indices (0 to 7) | None |
| `GET` | `/api/v1/forecast/day-wise` | 24-hour hourly forecast timeline for a specific day | `districtName`, `dayIndex` (0–7), `vendor` |
| `GET` | `/api/v1/forecast/combined` | Aggregated multi-day window forecast metrics | `districtName`, `startDate`, `endDate`, `vendor` |
| `GET` | `/api/v1/forecast/statewide-daily`| Statewide daily forecast for all 38 districts on a given day | `dayIndex` (0–7), `vendor` |
| `GET` | `/api/v1/pipeline/status` | Data ingestion status log from `processed_data` | None |

---

### Sample API Invocations & Payloads

#### 1. Public Configuration (`GET /api/v1/config/public`)
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/config/public" | ConvertTo-Json
```
```json
{
  "defaultDistrict": "Chennai",
  "defaultVendor": "open_meteo",
  "stateName": "Tamil Nadu",
  "mapCenterLat": 11.05,
  "mapCenterLon": 78.65,
  "mapZoom": 7,
  "mapTileLayerUrl": "https://server.arcgisonline.com/ArcGIS/rest/services/Canvas/World_Dark_Gray_Base/MapServer/tile/{z}/{y}/{x}"
}
```

#### 2. Day-Wise 24-Hour Forecast (`GET /api/v1/forecast/day-wise`)
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/forecast/day-wise?districtName=Chennai&dayIndex=0&vendor=open_meteo"
```
```json
{
  "districtName": "Chennai",
  "dayIndex": 0,
  "forecastDate": "2026-09-03",
  "vendor": "open_meteo",
  "minTemp": 27.4,
  "maxTemp": 36.1,
  "meanTemp": 31.8,
  "precipitationSum": 1.2,
  "maxWindSpeed": 22.4,
  "hourlyRecords": [
    { "hour": 0,  "timeLabel": "00:00", "temperature": 28.1, "precipitation": 0.0, "windSpeed": 12.0, "windDirection": 140, "humidity": 78 },
    { "hour": 6,  "timeLabel": "06:00", "temperature": 27.5, "precipitation": 0.0, "windSpeed": 9.5,  "windDirection": 120, "humidity": 82 },
    { "hour": 12, "timeLabel": "12:00", "temperature": 35.8, "precipitation": 0.4, "windSpeed": 18.2, "windDirection": 110, "humidity": 58 },
    { "hour": 18, "timeLabel": "18:00", "temperature": 31.2, "precipitation": 0.8, "windSpeed": 21.0, "windDirection": 130, "humidity": 70 }
  ]
}
```

#### 3. Combined Date Range Analytics (`GET /api/v1/forecast/combined`)
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/forecast/combined?districtName=Chennai&startDate=2026-09-03&endDate=2026-09-07&vendor=open_meteo"
```
```json
{
  "districtName": "Chennai",
  "startDate": "2026-09-03",
  "endDate": "2026-09-07",
  "vendor": "open_meteo",
  "totalPrecipitation": 14.8,
  "averageTemperature": 30.7,
  "minTemperature": 26.8,
  "maxTemperature": 36.5,
  "peakWindSpeed": 28.4,
  "dailyBreakdown": [
    { "forecastDate": "2026-09-03", "dayIndex": 0, "dayName": "Thu", "minTemp": 27.4, "maxTemp": 36.1, "meanTemp": 31.8, "precipitationSum": 1.2, "maxWindSpeed": 22.4 },
    { "forecastDate": "2026-09-04", "dayIndex": 1, "dayName": "Fri", "minTemp": 27.1, "maxTemp": 35.8, "meanTemp": 31.2, "precipitationSum": 4.5, "maxWindSpeed": 24.1 },
    { "forecastDate": "2026-09-05", "dayIndex": 2, "dayName": "Sat", "minTemp": 26.8, "maxTemp": 34.9, "meanTemp": 30.1, "precipitationSum": 6.2, "maxWindSpeed": 28.4 },
    { "forecastDate": "2026-09-06", "dayIndex": 3, "dayName": "Sun", "minTemp": 27.0, "maxTemp": 35.2, "meanTemp": 30.5, "precipitationSum": 2.1, "maxWindSpeed": 19.8 },
    { "forecastDate": "2026-09-07", "dayIndex": 4, "dayName": "Mon", "minTemp": 27.3, "maxTemp": 35.7, "meanTemp": 31.0, "precipitationSum": 0.8, "maxWindSpeed": 17.5 }
  ]
}
```

---

## 📁 Directory Structure & Module Breakdown

```
c:\own_weather_application\
├── pom.xml                                  # Maven dependencies (Spring Boot 3.4.0, PostgreSQL 42.7.8, HikariCP)
├── README.md                                # Full operational manual and technical architecture documentation
├── target\                                  # Packaged binary directory
│   └── own-weather-application-1.0.0-SNAPSHOT.jar # Executable Fat JAR
└── src\
    └── main\
        ├── java\com\ownweather\
        │   ├── OwnWeatherApplication.java   # Main Spring Boot entrypoint with @SpringBootApplication
        │   ├── config\
        │   │   ├── CorsConfig.java          # CORS filter allowing cross-origin clients
        │   │   └── VaultDataSourceConfig.java # Dynamic Vault DataSource bootstrapper with fallback
        │   ├── controller\
        │   │   └── WeatherController.java   # REST controller for all /api/v1 routes
        │   ├── model\
        │   │   ├── CombinedForecastResponse.java # Aggregated multi-day window DTO
        │   │   ├── DailyForecastSummaryDto.java  # Daily min/mean/max summary DTO
        │   │   ├── DayWiseForecastResponse.java  # 24-hour detailed day-wise DTO
        │   │   ├── DistrictDto.java              # District metadata & centroid DTO
        │   │   ├── HourlyForecastDto.java        # Single hourly slice DTO
        │   │   ├── PipelineStatusDto.java        # Audit ingestion status DTO
        │   │   └── WeatherActualDto.java         # Multi-vendor actual & air quality DTO
        │   ├── repository\
        │   │   ├── DistrictRepository.java       # PostGIS ST_AsGeoJSON district boundary DAO
        │   │   ├── ForecastRepository.java       # Day-wise hourly & combined range query DAO
        │   │   ├── PipelineStatusRepository.java # Ingestion audit log DAO
        │   │   └── WeatherActualRepository.java  # Spatial ST_Contains multi-vendor join DAO
        │   └── service\
        │       ├── VaultConfigService.java       # Dynamic Vault secret retrieval & public config manager
        │       └── WeatherService.java           # Central business logic and aggregation service
        └── resources\
            ├── application.yml              # Base Spring Boot configuration & fallback credentials
            └── static\                      # Embedded Single Page Application
                ├── index.html               # Main dashboard UI (Actual vs. Forecast views)
                ├── css\
                │   └── style.css            # Dark obsidian glassmorphic design system
                └── js\
                    ├── api.js               # REST client for backend communication
                    ├── app.js               # UI orchestrator, mode switcher, and event hub
                    ├── charts.js            # Chart.js visualizer (24h dual-axis & range envelopes)
                    └── map.js               # Leaflet map engine, Esri canvas, labels, and choropleth
```

---

## 🚀 Build, Deployment & Verification Guide

### Prerequisites

| Component | Minimum Version | Verified Local Path / Port |
| :--- | :--- | :--- |
| **Java Development Kit (JDK)** | 21+ | `C:\Program Files\Java\jdk-21.0.10` |
| **Apache Maven** | 3.9+ | `C:\Users\ajay.v\.m2\wrapper\dists\apache-maven-3.9.8-bin\337e6d14\apache-maven-3.9.8\bin\mvn.cmd` |
| **PostgreSQL + PostGIS** | 18.x with PostGIS 3.x | `localhost:5432` (`Own_Weather` database) |
| **HashiCorp Vault** | 1.15+ | `http://127.0.0.1:8200` (Path: `secret/local/Weather/weather.json`) |

---

### Step-by-Step Build & Run Instructions

#### 1. Navigate to Project Directory
```powershell
cd C:\own_weather_application
```

#### 2. Clean & Package Executable JAR
```powershell
& "C:\Users\ajay.v\.m2\wrapper\dists\apache-maven-3.9.8-bin\337e6d14\apache-maven-3.9.8\bin\mvn.cmd" clean package -DskipTests
```
This produces `target\own-weather-application-1.0.0-SNAPSHOT.jar` containing the embedded Tomcat server and static web assets.

#### 3. Run the Spring Boot Application
```powershell
& "C:\Program Files\Java\jdk-21.0.10\bin\java.exe" -jar target\own-weather-application-1.0.0-SNAPSHOT.jar
```

#### 4. Access the Web Dashboard
Open any modern web browser (Edge, Chrome, Firefox) and navigate to:  
👉 **`http://localhost:8080/`**

---

### Verification Checklist

Verify key system components using PowerShell:

```powershell
# 1. Verify Application Health & District Count (Must return 38)
(Invoke-RestMethod -Uri "http://localhost:8080/api/v1/districts").Count

# 2. Verify GeoJSON Boundary Generation
(Invoke-RestMethod -Uri "http://localhost:8080/api/v1/districts/geojson").features.Count

# 3. Verify Real-time Actual Telemetry (Chennai)
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/actual/districts/Chennai"

# 4. Verify 24-Hour Day-Wise Hourly Slices (Should return 24 records)
(Invoke-RestMethod -Uri "http://localhost:8080/api/v1/forecast/day-wise?districtName=Chennai&dayIndex=0").hourlyRecords.Count

# 5. Verify Combined Date Range Aggregator
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/forecast/combined?districtName=Chennai&startDate=2026-09-03&endDate=2026-09-07"
```

---

## 🛠️ Operational Troubleshooting & FAQ

### Q1: Why did the map previously show "API KEY REQUIRED"?
**Explanation**: The original basemap referenced CARTO Dark Matter (`basemaps.cartocdn.com`). CARTO instituted mandatory API token enforcement, rendering repetitive watermark text on unauthenticated requests.  
**Resolution**: Switched to **Esri World Dark Gray Canvas** (`https://server.arcgisonline.com/ArcGIS/rest/services/Canvas/World_Dark_Gray_Base/MapServer/tile/{z}/{y}/{x}`). Esri allows free public use of these tiles with zero watermarks and no API keys.

### Q2: How can I change districts in the user interface?
You have two synchronized options:
1. **Header Dropdown**: Select any district from the top bar dropdown: `District: [Chennai ▾]`. The map will fly to that district and highlight it in cyan.
2. **Direct Map Interaction**: Click on any district polygon or label on the map. The dropdown and all telemetry cards will update automatically.

### Q3: What happens if HashiCorp Vault is stopped or restarted?
The application includes a graceful fallback mechanism in `VaultDataSourceConfig.java`:
1. It attempts to connect to Vault at `http://127.0.0.1:8200` using the configured token.
2. If Vault is unreachable (connection refused, timeout, or invalid token), it logs a warning and falls back to static properties defined in `src/main/resources/application.yml`.

### Q4: How does the UV choropleth map work?
Click the **[UV]** layer button on the map control bar. The map will query `/api/v1/actual/summary` and color-code each of the 38 districts according to the WHO ultraviolet radiation index scale, ranging from **Emerald Green** (0–2, Low) to **Violet Purple** (11+, Extreme).

### Q5: How are multi-vendor naming discrepancies handled?
Districts with differing transliterations (such as *Tuticorin* vs *Thoothukudi*) are matched via PostGIS spatial containment (`ST_Contains(boundary.geometry, actual.geom)`), ensuring 100% data association across all 38 districts regardless of vendor-specific text variations.

---

## 📄 License & Maintainer Information

* **Platform**: Tamil Nadu Weather & Environmental Intelligence Platform
* **Engineering**: Enterprise Java Full-Stack & Geospatial Analytics
* **Runtime**: Java 21 | Spring Boot 3.4.0 | PostgreSQL 18 PostGIS | Leaflet.js
* **Secret Management**: HashiCorp Vault (`secret/local/Weather/weather.json`)
