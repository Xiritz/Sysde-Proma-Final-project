package org.example.Service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.example.Util.DatabaseConnection;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.*;
import java.util.Properties;
import java.io.FileInputStream;
import java.io.IOException;

public class BackupService {
    private String supabaseUrl;
    private String supabaseKey;

    private final HttpClient httpClient;

    public BackupService() {
        this.httpClient = HttpClient.newBuilder().build();
        loadCredentials();
    }

    private void loadCredentials() {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream("supabase.properties")) {
            props.load(fis);
            this.supabaseUrl = props.getProperty("SUPABASE_URL");
            this.supabaseKey = props.getProperty("SUPABASE_KEY");
        } catch (IOException e) {
            System.err.println("Could not load supabase.properties. Using default/empty credentials.");
        }
    }

    public void backupAllTables() {
        if (supabaseUrl == null || supabaseKey == null) {
            System.err.println("Backup skipped: Supabase credentials not found in supabase.properties.");
            return;
        }
        System.out.println("Initiating Supabase Backup...");
        String[] tables = {"customers", "inventory", "expenses", "transactions", "users"};
        
        for (String table : tables) {
            try {
                syncTable(table);
            } catch (Exception e) {
                System.err.println("Failed to sync table '" + table + "': " + e.getMessage());
            }
        }
        System.out.println("Backup process finished.");
    }

    private void syncTable(String tableName) throws Exception {
        JsonArray data = fetchTableData(tableName);
        if (data.size() == 0) {
            System.out.println("No data to sync for table: " + tableName);
            return;
        }

        String jsonPayload = data.toString();
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(supabaseUrl + tableName))
                .header("apikey", supabaseKey)
                .header("Authorization", "Bearer " + supabaseKey)
                .header("Content-Type", "application/json")
                .header("Prefer", "resolution=merge-duplicates") // UPSERT mode
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                System.out.println("Successfully synced table: " + tableName);
            } else {
                System.err.println("Error syncing table '" + tableName + "'. Status code: " + response.statusCode() + " - " + response.body());
            }
        } catch (java.io.IOException e) {
            System.err.println("Network error: Could not connect to Supabase. App is likely offline.");
        }
    }

    private JsonArray fetchTableData(String tableName) throws SQLException {
        JsonArray jsonArray = new JsonArray();
        String query = "SELECT * FROM " + tableName;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            while (rs.next()) {
                JsonObject jsonObject = new JsonObject();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnName(i).toLowerCase();
                    Object value = rs.getObject(i);
                    
                    if (value == null) {
                        jsonObject.addProperty(columnName, (String) null);
                    } else if (value instanceof java.sql.Timestamp) {
                        // Convert Timestamp to ISO 8601 format for Supabase
                        jsonObject.addProperty(columnName, value.toString().replace(" ", "T"));
                    } else if (value instanceof Number) {
                        jsonObject.addProperty(columnName, (Number) value);
                    } else if (value instanceof Boolean) {
                        jsonObject.addProperty(columnName, (Boolean) value);
                    } else {
                        jsonObject.addProperty(columnName, value.toString());
                    }
                }
                jsonArray.add(jsonObject);
            }
        }
        return jsonArray;
    }
}
