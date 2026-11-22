package com.legara.campusconnect.dao;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import com.legara.campusconnect.exception.DataAccessException;

public class ConnectionFactory {
    public static final String DEFAULT_CONFIG_FILE = "db.properties";

    private final String url;
    private final String user;
    private final String password;

    public ConnectionFactory(String url, String user, String password) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("Database URL is required.");
        }
        if (user == null || user.isBlank()) {
            throw new IllegalArgumentException("Database user is required.");
        }
        this.url = url.trim();
        this.user = user.trim();
        this.password = password == null ? "" : password;
    }

    public static ConnectionFactory fromDefaultFile() throws DataAccessException {
        return fromFile(DEFAULT_CONFIG_FILE);
    }

    public static ConnectionFactory fromFile(String fileName) throws DataAccessException {
        File file = new File(fileName);
        if (!file.isFile()) {
            throw new DataAccessException("Database settings file not found: " + file.getAbsolutePath()
                    + ". Copy db.properties.example to db.properties and enter your MySQL password.");
        }
        Properties properties = new Properties();
        try (FileInputStream in = new FileInputStream(file)) {
            properties.load(in);
        } catch (IOException e) {
            throw new DataAccessException("Could not read " + file.getAbsolutePath() + ": " + e.getMessage(), e);
        }
        try {
            return new ConnectionFactory(properties.getProperty("db.url"), properties.getProperty("db.user"),
                    properties.getProperty("db.password"));
        } catch (IllegalArgumentException e) {
            throw new DataAccessException("Invalid settings in " + file.getAbsolutePath() + ": " + e.getMessage(), e);
        }
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    public String describe() {
        return url + " (user " + user + ")";
    }
}
