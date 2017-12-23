package com.rudyii.hsw.database;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Created by jack on 04.11.16.
 */
@Component
public class DataSourceProvider {
    private static Logger LOG = LogManager.getLogger(DataSourceProvider.class);

    @Value("${jdbc.driverClassName}")
    private String driverClassName;

    @Value("${jdbc.url}")
    private String databaseUrl;

    @Bean
    public DataSource jdbcDataSource() {
        SingleConnectionDataSource ds = new SingleConnectionDataSource();
        ds.setDriverClassName(driverClassName);
        ds.setUrl(databaseUrl);
        return ds;
    }

    @Bean
    public Connection getConnection() throws SQLException {
        return jdbcDataSource().getConnection();
    }

    public synchronized void closeConnection() {
        try {
            jdbcDataSource().getConnection().close();
        } catch (SQLException e) {
            LOG.error("Failed to close the connection!", e);
        }
    }

    @PreDestroy
    public synchronized void optimizeDB() throws Exception {
        getConnection().createStatement().execute("VACUUM");
        closeConnection();
    }
}
