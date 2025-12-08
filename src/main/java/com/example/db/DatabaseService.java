package com.example.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Database Service for managing query history and results
 * Uses H2 embedded database
 */
public class DatabaseService {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseService.class);
    
    private final String dbUrl;
    private final String dbUser;
    private final String dbPassword;
    
    public DatabaseService(String dbUrl, String dbUser, String dbPassword) {
        this.dbUrl = dbUrl;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
        initializeDatabase();
    }
    
    /**
     * Initialize the database schema
     */
    private void initializeDatabase() {
        try {
            Class.forName("org.h2.Driver");
            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
                Statement stmt = conn.createStatement();
                
                // Create queries table
                stmt.execute(
                    "CREATE TABLE IF NOT EXISTS queries (" +
                    "  id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                    "  query VARCHAR(2048) NOT NULL," +
                    "  response VARCHAR(4096) NOT NULL," +
                    "  created_at TIMESTAMP NOT NULL," +
                    "  processing_time_ms LONG" +
                    ")"
                );
                
                logger.info("Database initialized successfully");
            }
        } catch (ClassNotFoundException e) {
            logger.error("H2 driver not found", e);
        } catch (SQLException e) {
            logger.error("Error initializing database", e);
        }
    }
    
    /**
     * Save a query and its response
     */
    public void saveQueryResult(String query, String response, long processingTimeMs) {
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
            String sql = "INSERT INTO queries (query, response, created_at, processing_time_ms) VALUES (?, ?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, query);
                pstmt.setString(2, response);
                pstmt.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
                pstmt.setLong(4, processingTimeMs);
                pstmt.executeUpdate();
                
                logger.debug("Query saved to database - Time: {} ms", processingTimeMs);
            }
        } catch (SQLException e) {
            logger.error("Error saving query result", e);
        }
    }
    
    /**
     * Retrieve recent queries
     */
    public List<QueryRecord> getRecentQueries(int limit) {
        List<QueryRecord> records = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
            String sql = "SELECT id, query, response, created_at, processing_time_ms FROM queries ORDER BY created_at DESC LIMIT ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, limit);
                ResultSet rs = pstmt.executeQuery();
                
                while (rs.next()) {
                    records.add(new QueryRecord(
                        rs.getLong("id"),
                        rs.getString("query"),
                        rs.getString("response"),
                        rs.getTimestamp("created_at").toLocalDateTime(),
                        rs.getLong("processing_time_ms")
                    ));
                }
            }
        } catch (SQLException e) {
            logger.error("Error retrieving queries", e);
        }
        return records;
    }
    
    /**
     * Get database statistics
     */
    public DatabaseStats getStats() {
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
            String sql = "SELECT COUNT(*) as total, AVG(processing_time_ms) as avg_time FROM queries";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    return new DatabaseStats(
                        rs.getLong("total"),
                        rs.getDouble("avg_time")
                    );
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting database stats", e);
        }
        return new DatabaseStats(0, 0.0);
    }
    
    /**
     * QueryRecord: represents a stored query
     */
    public static class QueryRecord {
        public final long id;
        public final String query;
        public final String response;
        public final LocalDateTime createdAt;
        public final long processingTimeMs;
        
        public QueryRecord(long id, String query, String response, LocalDateTime createdAt, long processingTimeMs) {
            this.id = id;
            this.query = query;
            this.response = response;
            this.createdAt = createdAt;
            this.processingTimeMs = processingTimeMs;
        }
    }
    
    /**
     * DatabaseStats: represents database statistics
     */
    public static class DatabaseStats {
        public final long totalQueries;
        public final double averageProcessingTimeMs;
        
        public DatabaseStats(long totalQueries, double averageProcessingTimeMs) {
            this.totalQueries = totalQueries;
            this.averageProcessingTimeMs = averageProcessingTimeMs;
        }
    }
}
