/**
 * Tamil Nadu Weather Intelligence - Charts Integration (Chart.js)
 */

let hourlyChartInstance = null;
let combinedChartInstance = null;

const WeatherCharts = {
    renderHourlyChart(canvasId, hourlyData) {
        const ctx = document.getElementById(canvasId);
        if (!ctx) return;

        if (hourlyChartInstance) {
            hourlyChartInstance.destroy();
        }

        const labels = hourlyData.map(d => d.timeLabel);
        const temps = hourlyData.map(d => d.temperature);
        const rains = hourlyData.map(d => d.rainQty);

        const gradient = ctx.getContext('2d').createLinearGradient(0, 0, 0, 220);
        gradient.addColorStop(0, 'rgba(56, 189, 248, 0.45)');
        gradient.addColorStop(1, 'rgba(56, 189, 248, 0.0)');

        hourlyChartInstance = new Chart(ctx, {
            type: 'line',
            data: {
                labels: labels,
                datasets: [
                    {
                        label: 'Temperature (°C)',
                        data: temps,
                        borderColor: '#38bdf8',
                        backgroundColor: gradient,
                        borderWidth: 2.5,
                        fill: true,
                        tension: 0.35,
                        pointBackgroundColor: '#38bdf8',
                        pointBorderColor: '#0f172a',
                        pointRadius: 3,
                        pointHoverRadius: 6,
                        yAxisID: 'yTemp'
                    },
                    {
                        type: 'bar',
                        label: 'Rainfall (mm)',
                        data: rains,
                        backgroundColor: 'rgba(99, 102, 241, 0.65)',
                        borderColor: '#818cf8',
                        borderWidth: 1,
                        borderRadius: 4,
                        barThickness: 8,
                        yAxisID: 'yRain'
                    }
                ]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                interaction: {
                    mode: 'index',
                    intersect: false
                },
                plugins: {
                    legend: { display: false },
                    tooltip: {
                        backgroundColor: 'rgba(15, 23, 42, 0.95)',
                        titleColor: '#f8fafc',
                        bodyColor: '#94a3b8',
                        borderColor: 'rgba(255, 255, 255, 0.1)',
                        borderWidth: 1,
                        padding: 10,
                        boxPadding: 4,
                        usePointStyle: true,
                        callbacks: {
                            afterBody: function(context) {
                                const index = context[0].dataIndex;
                                const item = hourlyData[index];
                                return [
                                    `Wind: ${item.windSpeed != null ? item.windSpeed + ' km/h' : '--'}`,
                                    `Humidity: ${item.humidity != null ? item.humidity + '%' : '--'}`
                                ];
                            }
                        }
                    }
                },
                scales: {
                    x: {
                        grid: { color: 'rgba(255, 255, 255, 0.04)' },
                        ticks: { color: '#64748b', font: { family: 'Plus Jakarta Sans', size: 11 } }
                    },
                    yTemp: {
                        type: 'linear',
                        position: 'left',
                        grid: { color: 'rgba(255, 255, 255, 0.05)' },
                        ticks: {
                            color: '#38bdf8',
                            font: { family: 'Plus Jakarta Sans', size: 11 },
                            callback: v => `${v}°C`
                        }
                    },
                    yRain: {
                        type: 'linear',
                        position: 'right',
                        grid: { drawOnChartArea: false },
                        ticks: {
                            color: '#818cf8',
                            font: { family: 'Plus Jakarta Sans', size: 11 },
                            callback: v => `${v} mm`
                        },
                        min: 0,
                        suggestedMax: 5
                    }
                }
            }
        });
    },

    renderCombinedChart(canvasId, dailyBreakdown) {
        const ctx = document.getElementById(canvasId);
        if (!ctx) return;

        if (combinedChartInstance) {
            combinedChartInstance.destroy();
        }

        const labels = dailyBreakdown.map(d => d.date);
        const minTemps = dailyBreakdown.map(d => d.tempMin);
        const maxTemps = dailyBreakdown.map(d => d.tempMax);
        const rainSums = dailyBreakdown.map(d => d.precipSum || 0);

        combinedChartInstance = new Chart(ctx, {
            type: 'line',
            data: {
                labels: labels,
                datasets: [
                    {
                        label: 'Max Temp (°C)',
                        data: maxTemps,
                        borderColor: '#f87171',
                        backgroundColor: 'transparent',
                        borderWidth: 2,
                        tension: 0.3,
                        pointBackgroundColor: '#f87171',
                        yAxisID: 'y'
                    },
                    {
                        label: 'Min Temp (°C)',
                        data: minTemps,
                        borderColor: '#38bdf8',
                        backgroundColor: 'transparent',
                        borderWidth: 2,
                        tension: 0.3,
                        pointBackgroundColor: '#38bdf8',
                        yAxisID: 'y'
                    },
                    {
                        type: 'bar',
                        label: 'Precipitation (mm)',
                        data: rainSums,
                        backgroundColor: 'rgba(99, 102, 241, 0.6)',
                        borderColor: '#818cf8',
                        borderWidth: 1,
                        borderRadius: 6,
                        barThickness: 18,
                        yAxisID: 'yRain'
                    }
                ]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                interaction: {
                    mode: 'index',
                    intersect: false
                },
                plugins: {
                    legend: {
                        display: true,
                        labels: {
                            color: '#94a3b8',
                            font: { family: 'Plus Jakarta Sans', size: 12 },
                            usePointStyle: true
                        }
                    },
                    tooltip: {
                        backgroundColor: 'rgba(15, 23, 42, 0.95)',
                        padding: 10
                    }
                },
                scales: {
                    x: {
                        grid: { color: 'rgba(255, 255, 255, 0.04)' },
                        ticks: { color: '#64748b', font: { family: 'Plus Jakarta Sans', size: 11 } }
                    },
                    y: {
                        position: 'left',
                        grid: { color: 'rgba(255, 255, 255, 0.05)' },
                        ticks: {
                            color: '#94a3b8',
                            font: { family: 'Plus Jakarta Sans', size: 11 },
                            callback: v => `${v}°C`
                        }
                    },
                    yRain: {
                        position: 'right',
                        grid: { drawOnChartArea: false },
                        ticks: {
                            color: '#818cf8',
                            font: { family: 'Plus Jakarta Sans', size: 11 },
                            callback: v => `${v} mm`
                        },
                        min: 0
                    }
                }
            }
        });
    }
};

window.WeatherCharts = WeatherCharts;
