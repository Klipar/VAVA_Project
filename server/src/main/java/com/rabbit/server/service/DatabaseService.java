package com.rabbit.server.service;

import io.github.cdimascio.dotenv.Dotenv;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.nio.file.*;

public class DatabaseService {

    private static volatile DatabaseService instance;
    private static final Object LOCK = new Object();

    private String dbUrl;
    private String dbUser;
    private String dbPassword;

    private DatabaseService() {
        loadConfig();
        loadDriver();
    }

    private void loadConfig() {
        Dotenv dotenv = Dotenv.load();

        String dbHost = dotenv.get("DB_IP");
        String dbPort = dotenv.get("DB_PORT");
        String dbName = dotenv.get("DB_NAME");
        this.dbUser = dotenv.get("DB_USER");
        this.dbPassword = dotenv.get("DB_PASS");

        this.dbUrl = String.format("jdbc:postgresql://%s:%s/%s", dbHost, dbPort, dbName);
    }

    private void loadDriver() {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("PostgreSQL JDBC Driver not found", e);
        }
    }

    public static DatabaseService getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new DatabaseService();
                }
            }
        }
        return instance;
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
    }

    public List<Map<String, Object>> query(String sql, Object... params) throws SQLException {
        List<Map<String, Object>> result = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                int columnCount = meta.getColumnCount();

                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        row.put(meta.getColumnName(i), rs.getObject(i));
                    }
                    result.add(row);
                }
            }
        }

        return result;
    }

    public int update(String sql, Object... params) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }

            return stmt.executeUpdate();
        }
    }

    public long insertAndGetId(String sql, Object... params) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }

            stmt.executeUpdate();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1);
                }
                throw new SQLException("No generated key returned");
            }
        }
    }
}