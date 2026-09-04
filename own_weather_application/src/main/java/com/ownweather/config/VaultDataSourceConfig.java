package com.ownweather.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Configuration
public class VaultDataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(VaultDataSourceConfig.class);

    @Value("${vault.enabled:false}")
    private boolean vaultEnabled;

    @Value("${vault.url:http://127.0.0.1:8200}")
    private String vaultUrl;

    @Value("${vault.token:}")
    private String vaultToken;

    @Value("${vault.secret-path:secret/local/Weather/weather.json}")
    private String secretPath;

    @Value("${spring.datasource.url:jdbc:postgresql://localhost:5432/Own_Weather}")
    private String defaultUrl;

    @Value("${spring.datasource.username:readonly_user}")
    private String defaultUsername;

    @Value("${spring.datasource.password:viewer1}")
    private String defaultPassword;

    @Value("${spring.datasource.driver-class-name:org.postgresql.Driver}")
    private String defaultDriver;

    @Bean
    @Primary
    public DataSource dataSource() {
        String url = defaultUrl;
        String username = defaultUsername;
        String password = defaultPassword;
        String driver = defaultDriver;

        if (vaultEnabled && vaultToken != null && !vaultToken.isBlank()) {
            try {
                log.info("Attempting to load datasource credentials from HashiCorp Vault at {} path {}", vaultUrl, secretPath);
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
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode root = mapper.readTree(response.body());
                    JsonNode data = root.path("data");

                    if (data.has("spring.datasource.url")) {
                        url = data.get("spring.datasource.url").asText();
                    }
                    if (data.has("spring.datasource.username")) {
                        username = data.get("spring.datasource.username").asText();
                    }
                    if (data.has("spring.datasource.password")) {
                        password = data.get("spring.datasource.password").asText();
                    }
                    if (data.has("spring.datasource.driver-class-name")) {
                        driver = data.get("spring.datasource.driver-class-name").asText();
                    }
                    log.info("Successfully fetched database credentials from HashiCorp Vault for user: {}", username);
                } else {
                    log.warn("Vault responded with status code {}. Falling back to default datasource properties.", response.statusCode());
                }
            } catch (Exception e) {
                log.warn("Could not retrieve credentials from Vault ({}). Using default datasource properties.", e.getMessage());
            }
        } else {
            log.info("Vault integration disabled or token absent. Using application datasource properties.");
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName(driver);
        config.setPoolName("WeatherHikariCP");
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setReadOnly(true);
        config.setConnectionTimeout(20000);

        return new HikariDataSource(config);
    }
}
