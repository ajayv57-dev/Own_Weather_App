/**
 * Tamil Nadu Weather Intelligence - API Client
 */
const API_BASE = '/api/v1';

const WeatherApi = {
    async getPublicConfig() {
        const res = await fetch(`${API_BASE}/config/public`);
        if (!res.ok) throw new Error('Failed to load public configuration');
        return await res.json();
    },

    async getDistricts() {
        const res = await fetch(`${API_BASE}/districts`);
        if (!res.ok) throw new Error('Failed to load districts');
        return await res.json();
    },

    async getGeoJson() {
        const res = await fetch(`${API_BASE}/districts/geojson`);
        if (!res.ok) throw new Error('Failed to load district boundaries');
        return await res.json();
    },

    async getActualSummary() {
        const res = await fetch(`${API_BASE}/actual/summary`);
        if (!res.ok) throw new Error('Failed to load actual weather summary');
        return await res.json();
    },

    async getDistrictActual(districtName) {
        const res = await fetch(`${API_BASE}/actual/districts/${encodeURIComponent(districtName)}`);
        if (!res.ok) throw new Error(`Failed to load weather for ${districtName}`);
        return await res.json();
    },

    async getAvailableDates() {
        const res = await fetch(`${API_BASE}/forecast/available-dates`);
        if (!res.ok) throw new Error('Failed to load forecast dates');
        return await res.json();
    },

    async getDayWiseForecast(districtName, dayIndex = 0, vendor = 'open_meteo') {
        const res = await fetch(`${API_BASE}/forecast/day-wise?districtName=${encodeURIComponent(districtName)}&dayIndex=${dayIndex}&vendor=${encodeURIComponent(vendor)}`);
        if (!res.ok) throw new Error(`Failed to load day-wise forecast for ${districtName}`);
        return await res.json();
    },

    async getCombinedForecast(districtName, startDate, endDate, vendor = 'open_meteo') {
        let url = `${API_BASE}/forecast/combined?districtName=${encodeURIComponent(districtName)}&vendor=${encodeURIComponent(vendor)}`;
        if (startDate) url += `&startDate=${encodeURIComponent(startDate)}`;
        if (endDate) url += `&endDate=${encodeURIComponent(endDate)}`;
        const res = await fetch(url);
        if (!res.ok) throw new Error(`Failed to load combined forecast for ${districtName}`);
        return await res.json();
    },

    async getPipelineStatus() {
        const res = await fetch(`${API_BASE}/pipeline/status`);
        if (!res.ok) throw new Error('Failed to load pipeline status');
        return await res.json();
    }
};

window.WeatherApi = WeatherApi;
