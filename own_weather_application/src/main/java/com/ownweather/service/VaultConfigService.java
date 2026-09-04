package com.ownweather.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class VaultConfigService {

    private static final Logger log = LoggerFactory.getLogger(VaultConfigService.class);

    @Value("${vault.enabled:true}")
    private boolean vaultEnabled;

    @Value("${vault.url:http://127.0.0.1:8200}")
    private String vaultUrl;

    @Value("${vault.token:f99bd67c-2949-55b0-a736-83ab84dd65bb}")
    private String vaultToken;

    @Value("${vault.secret-path:secret/local/Weather/weather.json}")
    private String secretPath;

    private final Map<String, String> cachedSecrets = new ConcurrentHashMap<>();
    private final ObjectMapper mapper = new ObjectMapper();

    public VaultConfigService() {}

    /**
     * Fetches or refreshes secrets dynamically from Vault.
     */
    public synchronized Map<String, String> refreshSecrets() {
        if (!vaultEnabled || vaultToken == null || vaultToken.isBlank()) {
            log.info("Vault is disabled or token is not provided. Skipping Vault refresh.");
            return cachedSecrets;
        }

        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(3))
                    .build();

            String fullUrl = vaultUrl + "/v1/" + secretPath;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(fullUrl))
                    .header("X-Vault-Token", vaultToken)
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonNode root = mapper.readTree(response.body());
                JsonNode data = root.path("data");

                cachedSecrets.clear();
                data.fieldNames().forEachRemaining(fieldName -> {
                    cachedSecrets.put(fieldName, data.get(fieldName).asText());
                });

                log.info("Successfully refreshed {} configuration keys from Vault path: {}", cachedSecrets.size(), secretPath);
            } else {
                log.warn("Failed to fetch secrets from Vault. Status code: {}", response.statusCode());
            }
        } catch (Exception e) {
            log.warn("Exception during Vault secrets fetch: {}", e.getMessage());
        }

        return cachedSecrets;
    }

    public String get(String key, String defaultValue) {
        if (cachedSecrets.isEmpty()) {
            refreshSecrets();
        }
        return cachedSecrets.getOrDefault(key, defaultValue);
    }

    /**
     * Returns a sanitized map of public, safe configuration properties for the UI.
     */
    public Map<String, Object> getPublicConfig() {
        if (cachedSecrets.isEmpty()) {
            refreshSecrets();
        }

        Map<String, Object> publicMap = new LinkedHashMap<>();
        publicMap.put("defaultDistrict", get("app.weather.default-district", "Chennai"));
        publicMap.put("defaultVendor", get("app.weather.default-vendor", "open_meteo"));
        publicMap.put("stateName", get("app.weather.state-name", "Tamil Nadu"));
        publicMap.put("centerLat", Double.parseDouble(get("app.weather.map.center-lat", "11.05")));
        publicMap.put("centerLon", Double.parseDouble(get("app.weather.map.center-lon", "78.65")));
        publicMap.put("zoom", Integer.parseInt(get("app.weather.map.zoom", "7")));
        publicMap.put("tileLayerUrl", get("app.weather.map.tile-layer-url", "https://server.arcgisonline.com/ArcGIS/rest/services/Canvas/World_Dark_Gray_Base/MapServer/tile/{z}/{y}/{x}"));
        publicMap.put("vaultConnected", !cachedSecrets.isEmpty());

        return publicMap;
    }

    public Map<String, String> getAllConfig() {
        if (cachedSecrets.isEmpty()) {
            refreshSecrets();
        }
        return Collections.unmodifiableMap(cachedSecrets);
    }
}
