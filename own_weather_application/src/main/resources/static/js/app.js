/**
 * Tamil Nadu Weather Intelligence - Main Application Orchestrator
 */

const State = {
    mode: 'actual',        // 'actual' | 'forecast'
    subMode: 'daywise',    // 'daywise' | 'combined'
    district: 'Chennai',
    vendor: 'open_meteo',
    dayIndex: 0,
    startDate: null,
    endDate: null,
    availableDates: [],
    actualList: []
};

document.addEventListener('DOMContentLoaded', async () => {
    initEventListeners();
    await initApplication();
});

async function initApplication() {
    try {
        // 0. Load Vault Config
        try {
            const vaultConfig = await WeatherApi.getPublicConfig();
            if (vaultConfig) {
                if (vaultConfig.defaultDistrict) State.district = vaultConfig.defaultDistrict;
                if (vaultConfig.defaultVendor) State.vendor = vaultConfig.defaultVendor;
                const dbLabel = document.getElementById('dbStatusVal');
                if (dbLabel && vaultConfig.vaultConnected) {
                    dbLabel.innerHTML = 'PostgreSQL 18 + PostGIS <span style="color: #34d399; margin-left: 6px;">● Vault Active</span>';
                }
            }
        } catch (e) {
            console.warn('Vault config bootstrap note:', e);
        }

        // 1. Initialize Map
        WeatherMap.init('map', onDistrictSelected);

        // 2. Load GeoJSON Boundaries from PostGIS
        const geoJsonData = await WeatherApi.getGeoJson();
        WeatherMap.loadGeoJson(geoJsonData);

        // 3. Load Actual Weather Summary
        await refreshActualData();

        // 4. Load Forecast Available Dates
        await loadForecastDates();

        // 5. Load Ingestion Status
        await loadPipelineStatus();

    } catch (err) {
        console.error('Initialization error:', err);
    }
}

function initEventListeners() {
    // Mode Switcher (Actual vs Forecast)
    const actualBtn = document.getElementById('modeActualBtn');
    const forecastBtn = document.getElementById('modeForecastBtn');
    const actualView = document.getElementById('actualView');
    const forecastView = document.getElementById('forecastView');

    actualBtn.addEventListener('click', () => {
        State.mode = 'actual';
        actualBtn.classList.add('active');
        forecastBtn.classList.remove('active');
        actualView.classList.add('active');
        forecastView.classList.remove('active');
    });

    forecastBtn.addEventListener('click', () => {
        State.mode = 'forecast';
        forecastBtn.classList.add('active');
        actualBtn.classList.remove('active');
        forecastView.classList.add('active');
        actualView.classList.remove('active');
        loadForecastData();
    });

    // Sub-mode Switcher (Day-Wise vs Combined)
    const dayWiseBtn = document.getElementById('subnavDayWiseBtn');
    const combinedBtn = document.getElementById('subnavCombinedBtn');
    const dayWiseSubView = document.getElementById('dayWiseSubView');
    const combinedSubView = document.getElementById('combinedSubView');

    dayWiseBtn.addEventListener('click', () => {
        State.subMode = 'daywise';
        dayWiseBtn.classList.add('active');
        combinedBtn.classList.remove('active');
        dayWiseSubView.classList.add('active');
        combinedSubView.classList.remove('active');
        loadDayWiseForecast();
    });

    combinedBtn.addEventListener('click', () => {
        State.subMode = 'combined';
        combinedBtn.classList.add('active');
        dayWiseBtn.classList.remove('active');
        combinedSubView.classList.add('active');
        dayWiseSubView.classList.remove('active');
        loadCombinedForecast();
    });

    // Map Metric Toggles
    const metricBtns = document.querySelectorAll('#mapMetricToggles .metric-btn');
    metricBtns.forEach(btn => {
        btn.addEventListener('click', () => {
            metricBtns.forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            const metric = btn.getAttribute('data-metric');
            WeatherMap.setMetric(metric);
        });
    });

    // District Selector Dropdown
    const districtSelect = document.getElementById('districtSelect');
    if (districtSelect) {
        districtSelect.addEventListener('change', (e) => {
            const chosen = e.target.value;
            if (chosen) {
                State.district = chosen;
                document.getElementById('activeDistrictName').textContent = chosen;
                WeatherMap.setSelectedDistrict(chosen, true); // true = fly to district
                updateSelectedDistrictDetails(chosen);
                if (State.mode === 'forecast') {
                    loadForecastData();
                }
            }
        });
    }

    // Vendor Selector
    const vendorSelect = document.getElementById('vendorSelect');
    vendorSelect.addEventListener('change', (e) => {
        State.vendor = e.target.value;
        if (State.mode === 'forecast') {
            loadForecastData();
        }
    });

    // Refresh Button
    document.getElementById('refreshBtn').addEventListener('click', async () => {
        await refreshActualData();
        if (State.mode === 'forecast') {
            loadForecastData();
        }
        await loadPipelineStatus();
    });

    // Apply Date Range Button
    document.getElementById('applyRangeBtn').addEventListener('click', () => {
        const startSelect = document.getElementById('rangeStartDate');
        const endSelect = document.getElementById('rangeEndDate');
        State.startDate = startSelect.value;
        State.endDate = endSelect.value;
        loadCombinedForecast();
    });
}

function onDistrictSelected(districtName) {
    State.district = districtName;
    document.getElementById('activeDistrictName').textContent = districtName;
    
    // Sync the header dropdown
    const districtSelect = document.getElementById('districtSelect');
    if (districtSelect) {
        districtSelect.value = districtName;
    }

    updateSelectedDistrictDetails(districtName);

    if (State.mode === 'forecast') {
        loadForecastData();
    }
}

async function refreshActualData() {
    try {
        const data = await WeatherApi.getActualSummary();
        State.actualList = data;
        WeatherMap.setActualData(data);
        populateDistrictDropdown(data);
        renderStatewideStats(data);
        updateSelectedDistrictDetails(State.district);
    } catch (e) {
        console.error('Error refreshing actual data:', e);
    }
}

function populateDistrictDropdown(list) {
    const districtSelect = document.getElementById('districtSelect');
    if (!districtSelect || !list || list.length === 0) return;

    const currentVal = districtSelect.value || State.district;
    districtSelect.innerHTML = '';

    const sorted = [...list].sort((a, b) => a.districtName.localeCompare(b.districtName));
    sorted.forEach(d => {
        const opt = document.createElement('option');
        opt.value = d.districtName;
        opt.textContent = `${d.districtName} (${d.consensusTemperature ?? '--'}°C)`;
        if (d.districtName.toLowerCase() === currentVal.toLowerCase()) {
            opt.selected = true;
        }
        districtSelect.appendChild(opt);
    });
}

function renderStatewideStats(list) {
    if (!list || list.length === 0) return;

    let totalTemp = 0;
    let countTemp = 0;
    let hottest = list[0];
    let coolest = list[0];
    let rainCount = 0;

    list.forEach(item => {
        const t = item.consensusTemperature;
        if (t != null) {
            totalTemp += t;
            countTemp++;
            if (hottest.consensusTemperature == null || t > hottest.consensusTemperature) hottest = item;
            if (coolest.consensusTemperature == null || t < coolest.consensusTemperature) coolest = item;
        }
        if ((item.openMeteoPrecipitation && item.openMeteoPrecipitation > 0) ||
            (item.weatherApiChanceOfRain && item.weatherApiChanceOfRain > 50)) {
            rainCount++;
        }
    });

    const avg = countTemp > 0 ? (totalTemp / countTemp).toFixed(1) : '--';
    document.getElementById('stateAvgTemp').textContent = `${avg}°C`;
    document.getElementById('stateHotDistrict').textContent = hottest.districtName ? `${hottest.districtName} (${hottest.consensusTemperature}°)` : '--';
    document.getElementById('stateCoolDistrict').textContent = coolest.districtName ? `${coolest.districtName} (${coolest.consensusTemperature}°)` : '--';
    document.getElementById('stateRainCount').textContent = `${rainCount} Districts`;
}

function updateSelectedDistrictDetails(districtName) {
    const item = State.actualList.find(d => d.districtName && d.districtName.toLowerCase() === districtName.toLowerCase());
    if (!item) return;

    // Hero Section
    document.getElementById('heroDistrictName').textContent = item.districtName;
    document.getElementById('heroCoords').textContent = 
        `${item.latitude ? item.latitude.toFixed(2) : '--'}° N, ${item.longitude ? item.longitude.toFixed(2) : '--'}° E • District Code: ${item.districtId || '--'}`;
    
    document.getElementById('heroTemp').textContent = `${item.consensusTemperature ?? '--'}°C`;
    document.getElementById('heroHumidity').textContent = `${item.openMeteoHumidity ?? item.openWeatherHumidity ?? '--'}%`;
    document.getElementById('heroWindSpeed').textContent = `${item.openMeteoWindSpeed ?? item.openWeatherWindSpeed ?? '--'} km/h`;
    document.getElementById('heroPrecip').textContent = `${item.openMeteoPrecipitation ?? 0} mm`;
    document.getElementById('heroAqi').textContent = item.aqi != null ? `${item.aqi}` : '--';

    const cond = item.weatherCondition || 'Clear / Sunny';
    document.getElementById('heroConditionText').textContent = cond;
    const badgeIcon = document.querySelector('#heroConditionBadge .badge-icon');
    if (cond.includes('Rain')) badgeIcon.textContent = '🌧️';
    else if (cond.includes('Wind')) badgeIcon.textContent = '💨';
    else if (cond.includes('Cloud')) badgeIcon.textContent = '⛅';
    else badgeIcon.textContent = '☀️';

    // Multi-Vendor Comparison
    document.getElementById('vmTemp').textContent = `${item.openMeteoTemp ?? '--'}°C`;
    document.getElementById('vmHumidity').textContent = `${item.openMeteoHumidity ?? '--'}%`;
    document.getElementById('vmWind').textContent = `${item.openMeteoWindSpeed ?? '--'} km/h`;
    document.getElementById('vmRain').textContent = `${item.openMeteoPrecipitation ?? 0} mm`;

    document.getElementById('vowTemp').textContent = `${item.openWeatherTemp ?? '--'}°C`;
    document.getElementById('vowMinMax').textContent = `${item.openWeatherTempMin ?? '--'} / ${item.openWeatherTempMax ?? '--'}°C`;
    document.getElementById('vowHumidity').textContent = `${item.openWeatherHumidity ?? '--'}%`;
    document.getElementById('vowWind').textContent = `${item.openWeatherWindSpeed ?? '--'} km/h`;

    document.getElementById('vaTemp').textContent = `${item.weatherApiTemp ?? '--'}°C`;
    document.getElementById('vaHumidity').textContent = `${item.weatherApiHumidity ?? '--'}%`;
    document.getElementById('vaWind').textContent = `${item.weatherApiWindSpeed ?? '--'} km/h`;
    document.getElementById('vaRainChance').textContent = `${item.weatherApiChanceOfRain ?? '--'}%`;

    // Air Quality Pollutants
    document.getElementById('aqPm25').textContent = item.pm25 != null ? item.pm25 : '--';
    document.getElementById('aqNo2').textContent = item.no2 != null ? item.no2 : '--';
    document.getElementById('aqSo2').textContent = item.so2 != null ? item.so2 : '--';
    document.getElementById('aqCo').textContent = item.co != null ? item.co : '--';
    document.getElementById('aqOzone').textContent = item.ozone != null ? item.ozone : '--';
    document.getElementById('aqUv').textContent = item.uvIndex != null ? item.uvIndex : '--';
}

async function loadForecastDates() {
    try {
        const dates = await WeatherApi.getAvailableDates();
        State.availableDates = dates;

        // Render Day Pills
        const pillsContainer = document.getElementById('datePillsContainer');
        pillsContainer.innerHTML = '';

        const startSelect = document.getElementById('rangeStartDate');
        const endSelect = document.getElementById('rangeEndDate');
        startSelect.innerHTML = '';
        endSelect.innerHTML = '';

        dates.forEach((d, idx) => {
            // Day Pill
            const pill = document.createElement('div');
            pill.className = `date-pill ${idx === 0 ? 'active' : ''}`;
            pill.innerHTML = `
                <span class="date-pill-day">${idx === 0 ? 'Today' : (idx === 1 ? 'Tomorrow' : 'Day ' + idx)}</span>
                <span class="date-pill-val">${formatDateSimple(d.date)}</span>
            `;
            pill.addEventListener('click', () => {
                document.querySelectorAll('.date-pill').forEach(p => p.classList.remove('active'));
                pill.classList.add('active');
                State.dayIndex = d.dayIndex;
                loadDayWiseForecast();
            });
            pillsContainer.appendChild(pill);

            // Select Dropdowns for Combined Date Range
            const opt1 = document.createElement('option');
            opt1.value = d.date;
            opt1.textContent = `${d.date} (${idx === 0 ? 'Today' : 'Day ' + idx})`;
            startSelect.appendChild(opt1);

            const opt2 = document.createElement('option');
            opt2.value = d.date;
            opt2.textContent = `${d.date} (${idx === 0 ? 'Today' : 'Day ' + idx})`;
            endSelect.appendChild(opt2);
        });

        // Set default range: from Day 0 to Day 4 (or last day)
        if (dates.length > 0) {
            startSelect.value = dates[0].date;
            const endIdx = Math.min(dates.length - 1, 4);
            endSelect.value = dates[endIdx].date;
            State.startDate = dates[0].date;
            State.endDate = dates[endIdx].date;
        }

    } catch (e) {
        console.error('Error loading forecast dates:', e);
    }
}

function loadForecastData() {
    if (State.subMode === 'daywise') {
        loadDayWiseForecast();
    } else {
        loadCombinedForecast();
    }
}

async function loadDayWiseForecast() {
    try {
        const district = State.district;
        const res = await WeatherApi.getDayWiseForecast(district, State.dayIndex, State.vendor);

        document.getElementById('hourlyChartTitle').textContent = `24-Hour Forecast: ${district}`;
        document.getElementById('hourlyChartSub').textContent = `Date: ${res.date || '--'} • Vendor: ${formatVendor(res.vendor)}`;

        // Render Chart
        if (res.hourly && res.hourly.length > 0) {
            WeatherCharts.renderHourlyChart('hourlyChartCanvas', res.hourly);
            renderHourlyRail(res.hourly);
        }
    } catch (e) {
        console.error('Error loading day-wise forecast:', e);
    }
}

function renderHourlyRail(hourlyList) {
    const rail = document.getElementById('hourlyRail');
    rail.innerHTML = '';

    hourlyList.forEach(item => {
        const card = document.createElement('div');
        card.className = 'hourly-card';
        card.innerHTML = `
            <span class="hourly-time">${item.timeLabel}</span>
            <span class="hourly-temp">${item.temperature != null ? item.temperature + '°' : '--'}</span>
            <span class="hourly-rain">${item.rainQty != null && item.rainQty > 0 ? item.rainQty + 'mm' : '0mm'}</span>
            <span class="hourly-wind">${item.windSpeed != null ? item.windSpeed + 'km' : '--'}</span>
        `;
        rail.appendChild(card);
    });
}

async function loadCombinedForecast() {
    try {
        const district = State.district;
        const res = await WeatherApi.getCombinedForecast(district, State.startDate, State.endDate, State.vendor);

        document.getElementById('combinedChartTitle').textContent = `Combined Outlook (${res.totalDays} Days): ${district}`;
        document.getElementById('combinedChartSub').textContent = `Window: ${res.startDate || '--'} to ${res.endDate || '--'} • Vendor: ${formatVendor(res.vendor)}`;

        document.getElementById('rangeCumRain').textContent = `${res.cumulativeRainfall ?? 0} mm`;
        document.getElementById('rangeAvgTemp').textContent = `${res.avgTemperature ?? '--'}°C`;
        document.getElementById('rangeTempEnvelope').textContent = `${res.minTemperature ?? '--'} / ${res.maxTemperature ?? '--'}°C`;
        document.getElementById('rangePeakWind').textContent = `${res.avgMaxWindSpeed ?? '--'} km/h`;

        // Render Combined Chart
        if (res.dailyBreakdown && res.dailyBreakdown.length > 0) {
            WeatherCharts.renderCombinedChart('combinedChartCanvas', res.dailyBreakdown);
            renderDailyTable(res.dailyBreakdown);
        }
    } catch (e) {
        console.error('Error loading combined forecast:', e);
    }
}

function renderDailyTable(dailyList) {
    const tbody = document.querySelector('#combinedDailyTable tbody');
    tbody.innerHTML = '';

    dailyList.forEach(item => {
        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td><strong>${item.date}</strong></td>
            <td>Day ${item.dayIndex}</td>
            <td style="color: #38bdf8;">${item.tempMin != null ? item.tempMin + '°C' : '--'}</td>
            <td>${item.tempMean != null ? item.tempMean + '°C' : '--'}</td>
            <td style="color: #f87171;">${item.tempMax != null ? item.tempMax + '°C' : '--'}</td>
            <td style="color: #818cf8;">${item.precipSum != null ? item.precipSum + ' mm' : '0.0 mm'}</td>
            <td>${item.windSpeedMax != null ? item.windSpeedMax + ' km/h' : '--'}</td>
        `;
        tbody.appendChild(tr);
    });
}

async function loadPipelineStatus() {
    try {
        const list = await WeatherApi.getPipelineStatus();
        if (list && list.length > 0) {
            const latest = list[0];
            const timeStr = latest.updatedTime ? new Date(latest.updatedTime).toLocaleString() : 'Recent';
            document.getElementById('lastSyncVal').textContent = `${timeStr} (${latest.vendor || 'multi-source'})`;
        }
    } catch (e) {
        console.error('Error loading pipeline status:', e);
    }
}

function formatDateSimple(dateStr) {
    if (!dateStr) return '';
    const parts = dateStr.split('-');
    if (parts.length === 3) {
        const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
        const m = parseInt(parts[1], 10) - 1;
        return `${parts[2]} ${months[m] || ''}`;
    }
    return dateStr;
}

function formatVendor(v) {
    if (v === 'open_weather') return 'OpenWeather';
    if (v === 'weatherapi') return 'WeatherAPI';
    return 'Open-Meteo';
}
