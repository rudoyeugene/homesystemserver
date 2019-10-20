package com.rudyii.hsw.database;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import javax.sql.DataSource;
import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;

@Slf4j
@Component
public class DataSourceProvider {
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
        new File("database").mkdirs();

        Connection connection = jdbcDataSource().getConnection();

        connection.createStatement().execute("CREATE TABLE IF NOT EXISTS DROPBOX_FILES (FILE_NAME STRING UNIQUE, UPLOAD_DATE_TIME DATETIME DEFAULT CURRENT_TIMESTAMP)");
        connection.createStatement().execute("CREATE UNIQUE INDEX IF NOT EXISTS DROPBOX_FILES_INDEX ON DROPBOX_FILES (FILE_NAME)");

        connection.createStatement().execute("CREATE TABLE IF NOT EXISTS SETTINGS (KEY STRING NOT NULL UNIQUE, VALUE)");
        connection.createStatement().execute("CREATE UNIQUE INDEX IF NOT EXISTS SETTINGS_INDEX ON SETTINGS (KEY)");

        return connection;
    }

    private synchronized void closeConnection() {
        try {
            jdbcDataSource().getConnection().close();
        } catch (SQLException e) {
            log.error("Failed to close the connection!", e);
        }
    }

    @PreDestroy
    public synchronized void optimizeDB() throws Exception {
        getConnection().createStatement().execute("VACUUM");
        closeConnection();
    }
}
