package com.legara.bookbridge.dao;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import com.legara.bookbridge.exception.DataAccessException;

public class ConnectionFactory {
    private final String url;
    private final String user;
    private final String password;

    public ConnectionFactory(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    public static ConnectionFactory fromFile(String path) throws DataAccessException {
        Properties settings = new Properties();
        try (FileInputStream in = new FileInputStream(path)) {
            settings.load(in);
        } catch (IOException e) {
            throw new DataAccessException("Cannot read database settings from '" + path
                    + "'. Copy db.properties.example to db.properties and fill in your MySQL password.", e);
        }
        String url = settings.getProperty("db.url");
        String user = settings.getProperty("db.user");
        String password = settings.getProperty("db.password");
        if (url == null || user == null || password == null) {
            throw new DataAccessException("db.properties must define db.url, db.user and db.password.");
        }
        return new ConnectionFactory(url.trim(), user.trim(), password);
    }

    public Connection open() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }
}
