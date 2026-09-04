# Own Weather App Repository

Monorepo for the **Tamil Nadu Weather & Environmental Intelligence Platform** containing data pipelines, analytics dashboards, and the full enterprise Java web application.

---

## Repository Modules

| Module | Technology | Description |
| :--- | :--- | :--- |
| [**own_weather_application/**](./own_weather_application) | **Java 21, Spring Boot 3.4.0, PostGIS, Leaflet.js** | Enterprise meteorological intelligence platform featuring 38-district PostGIS spatial queries, HashiCorp Vault credential management, Day-Wise 24h & Combined Date Range forecast analytics, WHO UV radiation maps, and dark glassmorphic UI. |
| [**
ifi-flows/**](./nifi-flows) | **Apache NiFi** | Data ingestion flows and transformation templates for external meteorological APIs (Open-Meteo, OpenWeather, WeatherAPI, NCUM). |
| [**streamlit-app/**](./streamlit-app) | **Python, Streamlit** | Rapid visualization and interactive data exploration dashboard. |
| [**jar/**](./jar) | **JAR Binaries** | Auxiliary libraries and database drivers. |

---

## Quick Start: Java Weather Application

To build and run the main full-stack Java platform:

`powershell
cd own_weather_application
mvn clean package -DskipTests
java -jar target/own-weather-application-1.0.0-SNAPSHOT.jar
`
Then navigate to **http://localhost:8080/**.

For detailed architecture, PostGIS spatial join analysis, HashiCorp Vault security schemas, and REST API documentation, see [**own_weather_application/README.md**](./own_weather_application/README.md).