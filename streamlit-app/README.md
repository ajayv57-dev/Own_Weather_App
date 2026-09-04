# Own Weather dashboard

Read-only Streamlit dashboard for the local `Own_Weather` PostgreSQL database. NCUM is intentionally not used.

## Run on Windows PowerShell

```powershell
$env:WEATHER_DB_PASSWORD = "your_password_here"
python -m streamlit run app.py
```

The dashboard uses OpenWeather, Open-Meteo, and WeatherAPI data for the 38 districts, plus Open-Meteo air quality. It includes current and forecast district maps with separate filters for each map, selectable forecast dates and metrics (including AQI), forecast provider comparison, and day-wise forecast air-quality details. It does not write to the database.

Displayed refresh times and hourly chart times are converted from the database's GMT/UTC timestamps to IST. Override `WEATHER_DB_TIMESTAMP_TZ` only if the ingestion service uses another source timezone.

The district choropleth reads from `public.weather_district_boundaries` in PostgreSQL. Each GeoJSON property (`dtname`, `stname`, `dtcode11`, and so on) is stored in its own column, along with a PostGIS `geometry` polygon column (EPSG:4326). The generated `centroid_lat` and `centroid_lon` columns provide explicit latitude/longitude values. Create the table as an owner/admin with `sql/weather_map_layers.sql`, then import the source GeoJSON once using:

```powershell
$env:WEATHER_DB_USER = "your_admin_user"
$env:WEATHER_DB_PASSWORD = "your_admin_password"
python scripts/import_district_boundaries.py
```

The source file is the public [Tamil Nadu district GeoJSON](https://github.com/datta07/INDIAN-SHAPEFILES/blob/master/STATES/TAMIL%20NADU/TAMIL%20NADU_DISTRICTS.geojson). Verify the boundary source and licensing before redistributing the dashboard.
