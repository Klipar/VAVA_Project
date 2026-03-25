package com.rabbit.server.database;

import java.sql.*;
import java.util.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

public class MigrationRunner {
    private Connection conn;

    public MigrationRunner(Connection conn) {
        this.conn = conn;
    }

    public void runMigrations() throws Exception {
        // Create schema_version table if not exists
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS schema_version (
                    version INT PRIMARY KEY,
                    description VARCHAR(255),
                    applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
        }

        // Load applied versions
        Set<Integer> applied = new HashSet<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT version FROM schema_version")) {
            while (rs.next()) {
                applied.add(rs.getInt("version"));
            }
        }

        List<String> migrations = loadMigrations("migrations");

        for (String file : migrations) {
            int version = Integer.parseInt(file.split("__")[0].replaceAll("[^0-9]", ""));

            if (!applied.contains(version)) {
                String sql = readResource(file);

                try {
                    conn.setAutoCommit(false);

                    try (Statement stmt = conn.createStatement()) {

                        // Execute each query separately
                        for (String query : sql.split(";")) {
                            if (!query.isBlank()) {
                                stmt.execute(query.trim());
                            }
                        }

                        // Save migration
                        stmt.execute(String.format(
                            "INSERT INTO schema_version (version, description) VALUES (%d, '%s')",
                            version,
                            file
                        ));
                    }

                    conn.commit();
                    System.out.println("Applied migration: " + file);

                } catch (Exception e) {
                    conn.rollback();
                    throw e;
                } finally {
                    conn.setAutoCommit(true);
                }
            }
        }
    }

    // Read file from classpath
    private String readResource(String path) throws IOException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
            if (is == null) {
                throw new RuntimeException("File not found: " + path);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private List<String> loadMigrations(String folder) throws IOException {
        List<String> result = new ArrayList<>();

        var classLoader = getClass().getClassLoader();
        var url = classLoader.getResource(folder);

        if (url == null) {
            throw new RuntimeException("Folder not found: " + folder);
        }

        if (url.getProtocol().equals("file")) {
            // Running from IDE (filesystem)
            File dir = new File(url.getFile());
            for (File file : Objects.requireNonNull(dir.listFiles())) {
                if (file.getName().endsWith(".sql")) {
                    result.add(folder + "/" + file.getName());
                }
            }
        } else if (url.getProtocol().equals("jar")) {
            // Running from JAR
            String path = url.getPath();
            String jarPath = path.substring(5, path.indexOf("!"));

            try (java.util.jar.JarFile jar = new java.util.jar.JarFile(jarPath)) {
                jar.stream()
                    .filter(e -> e.getName().startsWith(folder) && e.getName().endsWith(".sql"))
                    .forEach(e -> result.add(e.getName()));
            }
        }

        Collections.sort(result);

        return result;
    }
}
