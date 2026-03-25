package com.rabbit.server;
import com.rabbit.server.database.MigrationRunner;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import java.sql.Connection;
import java.sql.DriverManager;

import io.github.cdimascio.dotenv.Dotenv;

public class Main {
    public static void main(String[] args) throws IOException {
        Dotenv dotenv = Dotenv.load(); // object for reading data from dot env file

        String ip = dotenv.get("DB_IP");
        String port = dotenv.get("DB_PORT");
        String user = dotenv.get("DB_USER");
        String pass = dotenv.get("DB_PASS");
        String name = dotenv.get("DB_NAME");

        String url = String.format("jdbc:postgresql://%s:%s/%s", ip, port, name);

        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
            new MigrationRunner(conn).runMigrations();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Create HTTP server on port 6969
        HttpServer server = HttpServer.create(new InetSocketAddress(6969), 0);

        // Define /hello endpoint
        server.createContext("/hello", new HelloHandler());

        // Start server
        server.setExecutor(null); // default executor
        server.start();

        System.out.println("Server started on port 6969");
    }

    static class HelloHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Only allow GET
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                String response = "Hello from server";

                exchange.sendResponseHeaders(200, response.getBytes().length);

                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            } else {
                exchange.sendResponseHeaders(405, -1); // Method Not Allowed
            }
        }
    }
}