package com.banamex.cops.update_offer.kafka;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Crea la conexión a CitiDataStore solo si existe offer.kafka.datasource.url.
 * No se expone como DataSource para no desactivar el DataSource principal (cops-bd).
 * Si no se configura, el repositorio usa la conexión principal.
 */
@Configuration
@ConditionalOnProperty(prefix = "offer.kafka.datasource", name = "url")
public class CitiDataStoreConfig {

    @Bean(destroyMethod = "close")
    public CitiDataStore citiDataStore(
            @Value("${offer.kafka.datasource.url}") String url,
            @Value("${offer.kafka.datasource.username:}") String username,
            @Value("${offer.kafka.datasource.password:}") String password,
            @Value("${offer.kafka.datasource.driver-class-name:}") String driver) {

        HikariConfig cfg = new HikariConfig();
        cfg.setPoolName("citi-data-store");
        cfg.setJdbcUrl(url);
        cfg.setUsername(username);
        cfg.setPassword(password);
        if (!driver.isBlank()) {
            cfg.setDriverClassName(driver);
        }
        cfg.setMaximumPoolSize(5);
        cfg.setConnectionTimeout(5000);
        cfg.setInitializationFailTimeout(-1);   // no tumba el arranque si la BD no responde
        return new CitiDataStore(new HikariDataSource(cfg));
    }
}
