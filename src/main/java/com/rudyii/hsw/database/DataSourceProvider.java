package com.rudyii.hsw.database;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private DataSource dataSource;

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

        Connection connection = dataSource.getConnection();

        //create tables
        connection.createStatement().execute("CREATE TABLE IF NOT EXISTS RECORD_FILES (FILE_ID STRING UNIQUE, CREATED INTEGER)");
        connection.createStatement().execute("CREATE TABLE IF NOT EXISTS SETTINGS (KEY STRING NOT NULL UNIQUE, VALUE)");

        //cleanup deprecated tables
        connection.createStatement().execute("DROP TABLE IF EXISTS DROPBOX_FILES");

        return connection;
    }

    @PreDestroy
    public synchronized void optimizeDB() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            connection.createStatement().execute("VACUUM");
        }
    }
}
