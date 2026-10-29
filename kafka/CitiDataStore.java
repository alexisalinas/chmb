package com.banamex.cops.update_offer.kafka;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.jdbc.core.JdbcTemplate;

/** Conexión dedicada a CitiDataStore (solo existe si se configura offer.kafka.datasource.url). */
public class CitiDataStore implements AutoCloseable {

    private final HikariDataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    public CitiDataStore(HikariDataSource dataSource) {
        this.dataSource = dataSource;
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }

    @Override
    public void close() {
        dataSource.close();
    }
}
