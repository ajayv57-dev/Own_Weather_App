"""Own Weather: a read-only Streamlit dashboard for the local PostgreSQL database."""

from __future__ import annotations

import json
import os
from datetime import datetime
from zoneinfo import ZoneInfo

import pandas as pd
import plotly.express as px
import plotly.graph_objects as go
import psycopg2
import streamlit as st
from dotenv import load_dotenv

load_dotenv()

st.set_page_config(page_title="Own Weather", page_icon="🌦️", layout="wide")


def db_config() -> dict[str, object]:
    return {
        "host": os.getenv("WEATHER_DB_HOST", "localhost"),
        "port": int(os.getenv("WEATHER_DB_PORT", "5432")),
        "dbname": os.getenv("WEATHER_DB_NAME", "Own_Weather"),
        "user": os.getenv("WEATHER_DB_USER", "readonly_user"),
        "password": os.getenv("WEATHER_DB_PASSWORD", os.getenv("PGPASSWORD", "")),
    }


@st.cache_resource(show_spinner=False)
def connection():
    config = db_config()
    if not config["password"]:
        raise RuntimeError("Set WEATHER_DB_PASSWORD or PGPASSWORD before starting the dashboard.")
    return psycopg2.connect(**config)


@st.cache_data(ttl=300, show_spinner=False)
def query(sql: str, params: tuple = ()) -> pd.DataFrame:
    with connection().cursor() as cursor:
        cursor.execute(sql, params)
        rows = cursor.fetchall()
        columns = [item.name for item in cursor.description]
    return pd.DataFrame(rows, columns=columns)


@st.cache_data(ttl=300, show_spinner=False)
def load_current() -> pd.DataFrame:
    sources = {
        "OpenWeather": """
            SELECT district_id, district_name, latitude, longitude, updated_time,
                   temperature, humidity, wind_speed, NULL::double precision AS precipitation,
                   NULL::double precision AS uv
            FROM public.open_weather_actual
        """,
        "Open-Meteo": """
            SELECT district_id, district_name, latitude, longitude, updated_time,
                   temperature, relative_humidity AS humidity, wind_speed,
                   precipitation, NULL::double precision AS uv
            FROM public.open_meteo_actual
        """,
        "WeatherAPI": """
            SELECT district_id, district_name, latitude, longitude, updated_time,
                   temperature, humidity, wind_speed, precipitation, uv
            FROM public.weatherapi_actual
        """,
    }
    frames = []
    for provider, sql in sources.items():
        frame = query(sql)
        frame.insert(0, "provider", provider)
        frames.append(frame)
    return pd.concat(frames, ignore_index=True)


@st.cache_data(ttl=300, show_spinner=False)
def load_air_quality() -> pd.DataFrame:
    return query("""
        SELECT district_id, district_name, latitude, longitude, updated_time,
               european_aqi, pm2_5, carbon_monoxide, nitrogen_dioxide,
               sulphur_dioxide, ozone, uv_index
        FROM public.open_meteo_air_quality_actual
    """)


@st.cache_data(ttl=300, show_spinner=False)
def load_forecast(district_id: str) -> pd.DataFrame:
    row = query("""
        SELECT district_name, updated_time,
               time_day_0, temp_mean_day_0, temp_min_day_0, temp_max_day_0, precip_sum_day_0, wind_speed_max_day_0, uv_index_max_day_0,
               time_day_1, temp_mean_day_1, temp_min_day_1, temp_max_day_1, precip_sum_day_1, wind_speed_max_day_1, uv_index_max_day_1,
               time_day_2, temp_mean_day_2, temp_min_day_2, temp_max_day_2, precip_sum_day_2, wind_speed_max_day_2, uv_index_max_day_2,
               time_day_3, temp_mean_day_3, temp_min_day_3, temp_max_day_3, precip_sum_day_3, wind_speed_max_day_3, uv_index_max_day_3,
               time_day_4, temp_mean_day_4, temp_min_day_4, temp_max_day_4, precip_sum_day_4, wind_speed_max_day_4, uv_index_max_day_4,
               time_day_5, temp_mean_day_5, temp_min_day_5, temp_max_day_5, precip_sum_day_5, wind_speed_max_day_5, uv_index_max_day_5,
               time_day_6, temp_mean_day_6, temp_min_day_6, temp_max_day_6, precip_sum_day_6, wind_speed_max_day_6, uv_index_max_day_6
        FROM public.open_meteo_forecast
        WHERE district_id = %s
        LIMIT 1
    """, (district_id,))
    if row.empty:
        return pd.DataFrame()
    record = row.iloc[0]
    days = []
    for day in range(7):
        date_value = record.get(f"time_day_{day}")
        if pd.isna(date_value):
            continue
        days.append({
            "date": pd.to_datetime(date_value),
            "mean_temp": record.get(f"temp_mean_day_{day}"),
            "min_temp": record.get(f"temp_min_day_{day}"),
            "max_temp": record.get(f"temp_max_day_{day}"),
            "precipitation": record.get(f"precip_sum_day_{day}"),
            "wind_speed": record.get(f"wind_speed_max_day_{day}"),
            "uv_index": record.get(f"uv_index_max_day_{day}"),
        })
    return pd.DataFrame(days)


@st.cache_data(ttl=300, show_spinner=False)
def load_forecast_map() -> pd.DataFrame:
    fields = ["district_id", "district_name", "latitude", "longitude"]
    for day in range(7):
        fields.extend([
            f"time_day_{day}", f"temp_mean_day_{day}", f"temp_min_day_{day}",
            f"temp_max_day_{day}", f"precip_sum_day_{day}",
            f"wind_speed_max_day_{day}", f"uv_index_max_day_{day}",
        ])
    rows = query(
        "SELECT " + ", ".join(fields) + " FROM public.open_meteo_forecast"
    )
    normalized = []
    for _, record in rows.iterrows():
        for day in range(7):
            date_value = record.get(f"time_day_{day}")
            if pd.isna(date_value):
                continue
            normalized.append({
                "district_id": record["district_id"],
                "district_name": record["district_name"],
                "latitude": record["latitude"],
                "longitude": record["longitude"],
                "date": pd.to_datetime(date_value).date(),
                "temperature": record.get(f"temp_mean_day_{day}"),
                "temperature_min": record.get(f"temp_min_day_{day}"),
                "temperature_max": record.get(f"temp_max_day_{day}"),
                "precipitation": record.get(f"precip_sum_day_{day}"),
                "wind_speed": record.get(f"wind_speed_max_day_{day}"),
                "uv_index": record.get(f"uv_index_max_day_{day}"),
            })
    return pd.DataFrame(normalized)


@st.cache_data(ttl=300, show_spinner=False)
def load_air_quality_forecast_map() -> pd.DataFrame:
    fields = ["district_id", "district_name"]
    for day in range(8):
        fields.extend([f"time_day_{day}", f"european_aqi_day_{day}"])
    rows = query(
        "SELECT " + ", ".join(fields) + " FROM public.open_meteo_air_quality_forecast"
    )
    normalized = []
    for _, record in rows.iterrows():
        for day in range(8):
            date_value = record.get(f"time_day_{day}")
            if pd.isna(date_value):
                continue
            normalized.append({
                "district_id": record["district_id"],
                "date": pd.to_datetime(date_value).date(),
                "air_quality": record.get(f"european_aqi_day_{day}"),
            })
    return pd.DataFrame(normalized)


@st.cache_data(ttl=300, show_spinner=False)
def load_air_quality_forecast_details(district_id: str) -> pd.DataFrame:
    fields = ["district_name"]
    for day in range(8):
        fields.extend([
            f"time_day_{day}", f"european_aqi_day_{day}", f"pm2_5_day_{day}",
            f"carbon_monoxide_day_{day}", f"nitrogen_dioxide_day_{day}",
            f"sulphur_dioxide_day_{day}", f"uv_index_day_{day}",
        ])
    data = query(
        "SELECT " + ", ".join(fields)
        + " FROM public.open_meteo_air_quality_forecast WHERE district_id = %s LIMIT 1",
        (district_id,),
    )
    if data.empty:
        return pd.DataFrame()
    record = data.iloc[0]
    forecast_rows = []
    for day in range(8):
        date_value = record.get(f"time_day_{day}")
        if pd.isna(date_value):
            continue
        forecast_rows.append({
            "Date": pd.Timestamp(date_value).date(),
            "AQI": record.get(f"european_aqi_day_{day}"),
            "PM2.5": record.get(f"pm2_5_day_{day}"),
            "CO": record.get(f"carbon_monoxide_day_{day}"),
            "NO₂": record.get(f"nitrogen_dioxide_day_{day}"),
            "SO₂": record.get(f"sulphur_dioxide_day_{day}"),
            "UV": record.get(f"uv_index_day_{day}"),
        })
    return pd.DataFrame(forecast_rows)


@st.cache_data(ttl=300, show_spinner=False)
def load_forecast_comparison(district_id: str, forecast_date: object) -> pd.DataFrame:
    provider_queries = {
        "OpenWeather": """
            SELECT district_name, time_day_0, temp_day_0, temp_min_day_0, temp_max_day_0, humidity_day_0, wind_speed_day_0,
                   time_day_1, temp_day_1, temp_min_day_1, temp_max_day_1, humidity_day_1, wind_speed_day_1,
                   time_day_2, temp_day_2, temp_min_day_2, temp_max_day_2, humidity_day_2, wind_speed_day_2,
                   time_day_3, temp_day_3, temp_min_day_3, temp_max_day_3, humidity_day_3, wind_speed_day_3,
                   time_day_4, temp_day_4, temp_min_day_4, temp_max_day_4, humidity_day_4, wind_speed_day_4,
                   time_day_5, temp_day_5, temp_min_day_5, temp_max_day_5, humidity_day_5, wind_speed_day_5,
                   time_day_6, temp_day_6, temp_min_day_6, temp_max_day_6, humidity_day_6, wind_speed_day_6
            FROM public.open_weather_forecast WHERE district_id = %s LIMIT 1
        """,
        "Open-Meteo": """
            SELECT district_name, time_day_0, temp_mean_day_0, temp_min_day_0, temp_max_day_0, precip_sum_day_0, wind_speed_max_day_0, uv_index_max_day_0,
                   time_day_1, temp_mean_day_1, temp_min_day_1, temp_max_day_1, precip_sum_day_1, wind_speed_max_day_1, uv_index_max_day_1,
                   time_day_2, temp_mean_day_2, temp_min_day_2, temp_max_day_2, precip_sum_day_2, wind_speed_max_day_2, uv_index_max_day_2,
                   time_day_3, temp_mean_day_3, temp_min_day_3, temp_max_day_3, precip_sum_day_3, wind_speed_max_day_3, uv_index_max_day_3,
                   time_day_4, temp_mean_day_4, temp_min_day_4, temp_max_day_4, precip_sum_day_4, wind_speed_max_day_4, uv_index_max_day_4,
                   time_day_5, temp_mean_day_5, temp_min_day_5, temp_max_day_5, precip_sum_day_5, wind_speed_max_day_5, uv_index_max_day_5,
                   time_day_6, temp_mean_day_6, temp_min_day_6, temp_max_day_6, precip_sum_day_6, wind_speed_max_day_6, uv_index_max_day_6
            FROM public.open_meteo_forecast WHERE district_id = %s LIMIT 1
        """,
        "WeatherAPI": """
            SELECT district_name, time_day_0, temp_mean_day_0, temp_min_day_0, temp_max_day_0, precip_sum_day_0, wind_speed_max_day_0, uv_index_max_day_0,
                   time_day_1, temp_mean_day_1, temp_min_day_1, temp_max_day_1, precip_sum_day_1, wind_speed_max_day_1, uv_index_max_day_1,
                   time_day_2, temp_mean_day_2, temp_min_day_2, temp_max_day_2, precip_sum_day_2, wind_speed_max_day_2, uv_index_max_day_2,
                   time_day_3, temp_mean_day_3, temp_min_day_3, temp_max_day_3, precip_sum_day_3, wind_speed_max_day_3, uv_index_max_day_3,
                   time_day_4, temp_mean_day_4, temp_min_day_4, temp_max_day_4, precip_sum_day_4, wind_speed_max_day_4, uv_index_max_day_4,
                   time_day_5, temp_mean_day_5, temp_min_day_5, temp_max_day_5, precip_sum_day_5, wind_speed_max_day_5, uv_index_max_day_5,
                   time_day_6, temp_mean_day_6, temp_min_day_6, temp_max_day_6, precip_sum_day_6, wind_speed_max_day_6, uv_index_max_day_6
            FROM public.weatherapi_forecast WHERE district_id = %s LIMIT 1
        """,
    }
    result = []
    target_date = pd.Timestamp(forecast_date).date()
    for provider, sql in provider_queries.items():
        data = query(sql, (district_id,))
        if data.empty:
            continue
        record = data.iloc[0]
        for day in range(7):
            date_value = record.get(f"time_day_{day}")
            if pd.isna(date_value) or pd.Timestamp(date_value).date() != target_date:
                continue
            if provider == "OpenWeather":
                result.append({
                    "Provider": provider, "Temperature °C": record.get(f"temp_day_{day}"),
                    "Min °C": record.get(f"temp_min_day_{day}"), "Max °C": record.get(f"temp_max_day_{day}"),
                    "Rain mm": None, "Wind speed": record.get(f"wind_speed_day_{day}"), "UV index": None,
                })
            else:
                result.append({
                    "Provider": provider, "Temperature °C": record.get(f"temp_mean_day_{day}"),
                    "Min °C": record.get(f"temp_min_day_{day}"), "Max °C": record.get(f"temp_max_day_{day}"),
                    "Rain mm": record.get(f"precip_sum_day_{day}"),
                    "Wind speed": record.get(f"wind_speed_max_day_{day}"),
                    "UV index": record.get(f"uv_index_max_day_{day}"),
                })
            break
    return pd.DataFrame(result)


@st.cache_data(ttl=300, show_spinner=False)
def load_hourly(district_id: str) -> pd.DataFrame:
    row = query("""
        SELECT date, updated_time,
               temperature_hour0, rain_qty_hour0, windspeed_hour0,
               temperature_hour1, rain_qty_hour1, windspeed_hour1,
               temperature_hour2, rain_qty_hour2, windspeed_hour2,
               temperature_hour3, rain_qty_hour3, windspeed_hour3,
               temperature_hour4, rain_qty_hour4, windspeed_hour4,
               temperature_hour5, rain_qty_hour5, windspeed_hour5,
               temperature_hour6, rain_qty_hour6, windspeed_hour6,
               temperature_hour7, rain_qty_hour7, windspeed_hour7,
               temperature_hour8, rain_qty_hour8, windspeed_hour8,
               temperature_hour9, rain_qty_hour9, windspeed_hour9,
               temperature_hour10, rain_qty_hour10, windspeed_hour10,
               temperature_hour11, rain_qty_hour11, windspeed_hour11,
               temperature_hour12, rain_qty_hour12, windspeed_hour12,
               temperature_hour13, rain_qty_hour13, windspeed_hour13,
               temperature_hour14, rain_qty_hour14, windspeed_hour14,
               temperature_hour15, rain_qty_hour15, windspeed_hour15,
               temperature_hour16, rain_qty_hour16, windspeed_hour16,
               temperature_hour17, rain_qty_hour17, windspeed_hour17,
               temperature_hour18, rain_qty_hour18, windspeed_hour18,
               temperature_hour19, rain_qty_hour19, windspeed_hour19,
               temperature_hour20, rain_qty_hour20, windspeed_hour20,
               temperature_hour21, rain_qty_hour21, windspeed_hour21,
               temperature_hour22, rain_qty_hour22, windspeed_hour22,
               temperature_hour23, rain_qty_hour23, windspeed_hour23
        FROM public.open_meteo_forecast_hourly_0
        WHERE district_id = %s
        LIMIT 1
    """, (district_id,))
    if row.empty:
        return pd.DataFrame()
    record = row.iloc[0]
    result = []
    # Hourly values are stored against a GMT/UTC date in the database; show
    # their actual local time on the dashboard.
    base_date = pd.to_datetime(record["date"]).tz_localize("UTC")
    for hour in range(24):
        result.append({
            "time": (base_date + pd.Timedelta(hours=hour)).tz_convert("Asia/Kolkata"),
            "temperature": record.get(f"temperature_hour{hour}"),
            "rain": record.get(f"rain_qty_hour{hour}"),
            "wind_speed": record.get(f"windspeed_hour{hour}"),
        })
    return pd.DataFrame(result)


def number(value: object, suffix: str = "", digits: int = 1) -> str:
    if value is None or pd.isna(value):
        return "—"
    return f"{float(value):.{digits}f}{suffix}"


def to_ist(value: object) -> pd.Timestamp:
    timestamp = pd.Timestamp(value)
    source_zone = ZoneInfo(os.getenv("WEATHER_DB_TIMESTAMP_TZ", "UTC"))
    if timestamp.tzinfo is None:
        timestamp = timestamp.tz_localize(source_zone)
    return timestamp.tz_convert("Asia/Kolkata")


def format_ist(value: object) -> str:
    """Display database timestamps as IST (source timezone is configurable)."""
    if value is None or pd.isna(value):
        return "—"
    return to_ist(value).strftime("%d %b %Y, %I:%M:%S %p IST")


@st.cache_data(show_spinner=False)
def load_district_boundaries() -> dict | None:
    try:
        rows = query(
            "SELECT district_name, ST_AsGeoJSON(geometry) AS geometry, "
            "dtname, stname, stcode11, dtcode11, year_stat, objectid, test, "
            "dist_lgd, state_lgd, remarks, \"st_area(shape)\", \"st_length(shape)\" "
            "FROM public.weather_district_boundaries "
            "WHERE layer_name = %s ORDER BY district_name",
            ("tamil_nadu_districts",),
        )
    except psycopg2.errors.UndefinedTable:
        # The table is created/populated once by scripts/import_district_boundaries.py.
        connection().rollback()
        return None
    if rows.empty:
        return None
    # Normalize names used by the boundary source to names in the weather tables.
    aliases = {
        "Kanniyakumari": "Kanyakumari", "Thiruvarur": "Tiruvarur",
        "Thiruvallur": "Tiruvallur", "Tuticorin": "Thoothukudi",
        "Villupuram": "Viluppuram", "Kanchipuram": "Kancheepuram",
    }
    property_columns = [
        "dtname", "stname", "stcode11", "dtcode11", "year_stat", "objectid",
        "test", "dist_lgd", "state_lgd", "remarks", "st_area(shape)",
        "st_length(shape)",
    ]
    features = []
    for _, row in rows.iterrows():
        geometry = row["geometry"]
        if isinstance(geometry, str):
            geometry = json.loads(geometry)
        properties = {
            key: (None if pd.isna(row[key]) else row[key])
            for key in property_columns
        }
        source_name = properties.get("dtname") or properties.get("dist") or row["district_name"]
        properties["district_name"] = aliases.get(source_name, source_name)
        features.append({"type": "Feature", "properties": properties, "geometry": geometry})
    return {"type": "FeatureCollection", "features": features}


st.title("Own Weather")
st.caption("Tamil Nadu district weather dashboard · read-only PostgreSQL data")

try:
    with st.spinner("Loading weather data..."):
        current = load_current()
        air = load_air_quality()
except Exception as exc:  # Keep the UI actionable when credentials/config are missing.
    st.error(f"Database connection failed: {exc}")
    st.code("$env:WEATHER_DB_PASSWORD = '<your password>'\nstreamlit run app.py", language="powershell")
    st.stop()

districts = current[["district_id", "district_name"]].drop_duplicates().sort_values("district_name")
with st.sidebar:
    st.header("Filters")
    provider = st.selectbox("Source", ["OpenWeather", "Open-Meteo", "WeatherAPI"])
    district_name = st.selectbox("District", districts["district_name"].tolist())
    district_id = districts.loc[districts["district_name"] == district_name, "district_id"].iloc[0]
    
    # st.markdown("**Actual map filters**")
    map_metric = st.selectbox(
        "Weather",
        ["Temperature", "Wind speed", "Humidity", "Rain", "UV index", "Air quality (AQI)"],
    )
    map_type = st.selectbox("Map type", ["Street map", "Light map", "Dark map"], index=0)
    transparent_boundaries = st.toggle("Transparent boundary fill", value=False)
    boundary_transparency = st.slider(
        "Boundary transparency (%)", min_value=0, max_value=90,
        value=10, step=5, disabled=not transparent_boundaries,
        help="0% is opaque; higher values reveal the basemap underneath.",
    )
    selected = current[(current["district_id"] == district_id) & (current["provider"] == provider)].iloc[0]
    st.caption(f"Last refresh: {format_ist(selected['updated_time'])}")

active_district_id = district_id
active_district_name = current.loc[
    current["district_id"] == active_district_id, "district_name"
].iloc[0]
selected = current[
    (current["district_id"] == active_district_id) & (current["provider"] == provider)
].iloc[0]
selected_air = air[air["district_id"] == active_district_id].iloc[0]
latest = pd.to_datetime(current["updated_time"]).max()
age_hours = (datetime.now(ZoneInfo("Asia/Kolkata")) - to_ist(latest).to_pydatetime()).total_seconds() / 3600
if age_hours > 24:
    st.warning(f"Data freshness: latest provider snapshot is {format_ist(latest)}; refresh ingestion is recommended.")

st.subheader(f"Current conditions · {active_district_name}")
metrics = st.columns(5)
metrics[0].metric("Temperature", number(selected["temperature"], " °C"))
metrics[1].metric("Humidity", number(selected["humidity"], " %"))
metrics[2].metric("Wind", number(selected["wind_speed"], " km/h"))
metrics[3].metric("Precipitation", number(selected["precipitation"], " mm"))
metrics[4].metric("European AQI", number(selected_air["european_aqi"], digits=0))

st.subheader("Air quality details")
aq_cols = ["european_aqi", "pm2_5", "carbon_monoxide", "nitrogen_dioxide", "sulphur_dioxide", "ozone", "uv_index"]
st.dataframe(pd.DataFrame([selected_air[aq_cols].rename({
    "european_aqi": "AQI", "pm2_5": "PM2.5", "carbon_monoxide": "CO",
    "nitrogen_dioxide": "NO₂", "sulphur_dioxide": "SO₂", "ozone": "O₃", "uv_index": "UV",
})]).round(1), hide_index=True, width="stretch")

left, right = st.columns([1.25, 1])
with left:
    st.subheader("District map")
    st.caption(f"Boundary fill color: {map_metric}")
    map_data = current[current["provider"] == provider].rename(
        columns={"latitude": "lat", "longitude": "lon"}
    ).copy()
    # UV and AQI live in the Open-Meteo air-quality table and can be shown
    # for every provider because the districts share the same coordinates.
    map_data = map_data.merge(
        air[["district_id", "european_aqi", "uv_index"]], on="district_id", how="left"
    )
    metric_columns = {
        "Temperature": ("temperature", "Temperature (°C)", "RdYlBu_r"),
        "Wind speed": ("wind_speed", "Wind speed", "Viridis"),
        "Humidity": ("humidity", "Humidity (%)", "Blues"),
        "Rain": ("precipitation", "Rain (mm)", "Blues"),
        "UV index": ("uv_index", "UV index", "YlOrRd"),
        "Air quality (AQI)": ("european_aqi", "European AQI", "RdYlGn_r"),
    }
    metric_column, metric_label, colors = metric_columns[map_metric]
    map_data["map_value"] = pd.to_numeric(map_data[metric_column], errors="coerce")
    available = map_data["map_value"].notna().sum()
    if available == 0:
        st.info(f"{map_metric} is not available from {provider} in the current snapshot. Try another provider.")
    else:
        if available < len(map_data):
            st.caption(f"{available} of {len(map_data)} districts have {map_metric.lower()} data.")
        hover = {
            "map_value": ":.1f", "temperature": ":.1f", "humidity": ":.0f",
            "wind_speed": ":.1f", "precipitation": ":.1f", "uv_index": ":.1f",
            "european_aqi": ":.0f", "lat": False, "lon": False,
        }
        boundaries = load_district_boundaries()
        if boundaries:
            fig = px.choropleth_map(
                map_data.dropna(subset=["map_value"]), geojson=boundaries,
                locations="district_name", featureidkey="properties.district_name",
                color="map_value", hover_name="district_name", hover_data=hover,
                labels={"map_value": metric_label}, color_continuous_scale=colors,
                center={"lat": 11.1271, "lon": 78.6569}, zoom=6.1, height=480,
            )
        else:
            st.error("District boundary GeoJSON is missing; the map cannot be displayed.")
            st.stop()
        map_styles = {
            "Street map": "open-street-map",
            "Light map": "carto-positron",
            "Dark map": "carto-darkmatter",
        }
        fig.update_layout(
            map_style=map_styles[map_type], uirevision="tamil-nadu",
            margin={"r": 0, "t": 0, "l": 0, "b": 0},
        )
        # Choropleth traces do not render text labels themselves. Overlay a
        # text-only map trace at each district's weather location so names are
        # visible without requiring a hover.
        labels = map_data.dropna(subset=["map_value", "lat", "lon"]).drop_duplicates("district_name")
        fig.add_trace(go.Scattermap(
            lat=labels["lat"], lon=labels["lon"], text=labels["district_name"],
            mode="text", textfont={"size": 10, "color": "#111827"},
            hoverinfo="skip", showlegend=False,
        ))
        fig.update_traces(
            marker_opacity=1 - (boundary_transparency / 100),
            selector={"type": "choroplethmap"},
        )
        st.plotly_chart(fig, width="stretch", key="district_map")
with right:
    st.subheader("Provider comparison")
    comparison = current[current["district_id"] == active_district_id][
        ["district_id", "provider", "temperature", "humidity", "wind_speed", "precipitation"]
    ].copy()
    comparison = comparison.merge(
        air[["district_id", "european_aqi", "uv_index"]],
        left_on="district_id", right_on="district_id", how="left",
    )
    comparison = comparison.drop(columns=["district_id"])
    comparison.columns = ["Provider", "Temp °C", "Humidity %", "Wind", "Rain mm", "AQI", "UV"]
    st.dataframe(comparison.round(1), hide_index=True, width="stretch")
    comparison_chart_fields = {
        "Temperature": ("Temp °C", "Temperature (°C)"),
        "Wind speed": ("Wind", "Wind speed"),
        "Humidity": ("Humidity %", "Humidity (%)"),
        "Rain": ("Rain mm", "Rain (mm)"),
        "UV index": ("UV", "UV index"),
        "Air quality (AQI)": ("AQI", "European AQI"),
    }
    comparison_field, comparison_label = comparison_chart_fields[map_metric]
    bar = px.bar(
        comparison, x="Provider", y=comparison_field, color="Provider", height=270,
        labels={comparison_field: comparison_label},
    )
    bar.update_layout(showlegend=False, margin={"r": 0, "t": 10, "l": 0, "b": 0})
    st.plotly_chart(bar, width="stretch")

hourly = load_hourly(active_district_id)
if not hourly.empty:
    hourly_fields = {
        "Temperature": ("temperature", "Temperature °C"),
        "Rain": ("rain", "Rain (mm)"),
        "Wind speed": ("wind_speed", "Wind speed"),
    }
    st.subheader(f"Next 24 hours · {map_metric}")
    if map_metric in hourly_fields:
        hourly_field, hourly_label = hourly_fields[map_metric]
        hourly_chart = px.line(hourly, x="time", y=hourly_field, markers=True)
        hourly_chart.update_layout(yaxis_title=hourly_label, xaxis_title=None, legend_title=None)
        st.plotly_chart(hourly_chart, width="stretch")
    else:
        st.info(f"Hourly {map_metric.lower()} values are not stored in the current forecast table.")

st.subheader("Forecast map")
forecast_date = None
forecast_color_metric = "Temperature"
with st.spinner("Loading forecast map..."):
    forecast_map_data = load_forecast_map()
    air_quality_forecast = load_air_quality_forecast_map()
if not air_quality_forecast.empty:
    forecast_map_data = forecast_map_data.merge(
        air_quality_forecast, on=["district_id", "date"], how="left"
    )
if forecast_map_data.empty:
    st.info("No forecast map data is available.")
else:
    available_dates = sorted(forecast_map_data["date"].dropna().unique())
    today_ist = datetime.now(ZoneInfo("Asia/Kolkata")).date()
    future_dates = [item for item in available_dates if item >= today_ist] or available_dates
    forecast_controls = st.columns(2)
    with forecast_controls[0]:
        forecast_date = st.selectbox(
            "Forecast date", future_dates,
            format_func=lambda item: pd.Timestamp(item).strftime("%a, %d %b %Y"),
            key="forecast_map_date",
        )
    with forecast_controls[1]:
        forecast_color_metric = st.selectbox(
            "Forecast map color",
            ["Temperature", "Rain", "Wind speed", "UV index", "Air quality (AQI)"],
            key="forecast_map_metric",
        )
    forecast_columns = {
        "Temperature": ("temperature", "Mean temperature (°C)", "RdYlBu_r"),
        "Rain": ("precipitation", "Precipitation (mm)", "Blues"),
        "Wind speed": ("wind_speed", "Maximum wind speed", "Viridis"),
        "UV index": ("uv_index", "UV index", "YlOrRd"),
        "Air quality (AQI)": ("air_quality", "European AQI", "RdYlGn_r"),
    }
    forecast_column, forecast_label, forecast_colors = forecast_columns[forecast_color_metric]
    forecast_layer = forecast_map_data[forecast_map_data["date"] == forecast_date].copy()
    forecast_layer["map_value"] = pd.to_numeric(forecast_layer[forecast_column], errors="coerce")
    forecast_layer = forecast_layer.dropna(subset=["map_value"])
    forecast_boundaries = load_district_boundaries()
    forecast_comparison = load_forecast_comparison(active_district_id, forecast_date)
    forecast_left, forecast_right = st.columns([1, 1.45])
    with forecast_left:
        st.markdown(f"**Forecast provider comparison · {active_district_name}**")
        if forecast_comparison.empty:
            st.info("No provider forecast is available for this date.")
        else:
            st.dataframe(forecast_comparison.round(1), hide_index=True, width="stretch")
    with forecast_right:
        st.markdown(f"**Forecast map · {pd.Timestamp(forecast_date).strftime('%d %b %Y')}**")
        if forecast_layer.empty:
            st.info(f"No {forecast_color_metric.lower()} forecast is available for that date.")
        elif forecast_boundaries:
            forecast_fig = px.choropleth_map(
                forecast_layer, geojson=forecast_boundaries,
                locations="district_name", featureidkey="properties.district_name",
                color="map_value", hover_name="district_name",
                hover_data={"map_value": ":.1f"}, labels={"map_value": forecast_label},
                color_continuous_scale=forecast_colors,
                center={"lat": 11.1271, "lon": 78.6569}, zoom=6.1, height=520,
            )
            forecast_fig.update_traces(marker_opacity=1 - (boundary_transparency / 100))
            forecast_fig.add_trace(go.Scattermap(
                lat=forecast_layer["latitude"], lon=forecast_layer["longitude"],
                text=forecast_layer["district_name"], mode="text",
                textfont={"size": 10, "color": "#111827"}, hoverinfo="skip", showlegend=False,
            ))
            forecast_fig.update_layout(
                map_style="open-street-map", uirevision="forecast-tamil-nadu",
                margin={"r": 0, "t": 0, "l": 0, "b": 0},
            )
            st.plotly_chart(forecast_fig, width="stretch", key="forecast_map")
        else:
            st.info("District boundary GeoJSON is missing; forecast map cannot draw polygons.")

st.subheader("Forecast · Open-Meteo")
forecast = load_forecast(active_district_id)
if forecast.empty:
    st.info("No forecast rows found for this district.")
else:
    forecast_plot = forecast.copy()
    if forecast_color_metric == "Temperature":
        forecast_y = ["min_temp", "mean_temp", "max_temp"]
        forecast_y_label = "Temperature °C"
    else:
        forecast_metric_fields = {
            "Rain": ("precipitation", "Precipitation (mm)"),
            "Wind speed": ("wind_speed", "Maximum wind speed"),
            "UV index": ("uv_index", "UV index"),
        }
        if forecast_color_metric == "Air quality (AQI)":
            forecast_air_quality_chart = load_air_quality_forecast_details(active_district_id)
            if not forecast_air_quality_chart.empty:
                forecast_air_quality_chart["date"] = pd.to_datetime(forecast_air_quality_chart["Date"])
                forecast_plot = forecast_plot.merge(
                    forecast_air_quality_chart[["date", "AQI"]], on="date", how="left"
                )
            forecast_metric_fields["Air quality (AQI)"] = ("AQI", "European AQI")
        forecast_field, forecast_y_label = forecast_metric_fields[forecast_color_metric]
        forecast_y = [forecast_field]
    forecast_chart = px.line(forecast_plot, x="date", y=forecast_y, markers=True)
    forecast_chart.update_layout(yaxis_title=forecast_y_label, xaxis_title=None, legend_title=None)
    st.plotly_chart(forecast_chart, width="stretch")
    forecast_display = forecast.copy()
    forecast_display[["mean_temp", "min_temp", "max_temp", "precipitation"]] = forecast_display[
        ["mean_temp", "min_temp", "max_temp", "precipitation"]
    ].round(1)
    st.dataframe(forecast_display, hide_index=True, width="stretch")
    forecast_air_quality = load_air_quality_forecast_details(active_district_id)
    st.subheader("Forecast air quality details")
    if forecast_air_quality.empty:
        st.info("No forecast air-quality data is available.")
    else:
        st.dataframe(forecast_air_quality.round(1), hide_index=True, width="stretch")
