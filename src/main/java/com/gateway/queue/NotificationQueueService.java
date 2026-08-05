package com.gateway.queue;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.*;

public class NotificationQueueService {
    private static final HikariDataSource dataSource;

    // Initialize H2 Database and Connection Pool
    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:mem:gateway_db;DB_CLOSE_DELAY=-1");
        config.setUsername("sa");
        config.setPassword("");
        config.setMaximumPoolSize(5);
        dataSource = new HikariDataSource(config);

        // Auto-create database table
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE notification_tasks (" +
                    "task_id VARCHAR(36) PRIMARY KEY, " +
                    "tenant_id VARCHAR(50), " +
                    "channel VARCHAR(20), " +
                    "recipient VARCHAR(100), " +
                    "message TEXT, " +
                    "status VARCHAR(20)" +
                    ")");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Bounded Task Queue (Prevents OutOfMemoryError under high load)
    private final BlockingQueue taskQueue = new ArrayBlockingQueue<>(1000);
    private final ExecutorService workerPool = Executors.newFixedThreadPool(4);

    public NotificationQueueService() {
        // Start 4 background worker threads
        for (int i = 0; i < 4; i++) {
            workerPool.submit(this::processQueue);
        }
    }

    public boolean enqueue(NotificationTask task) {
        return taskQueue.offer(task); // Backpressure check: returns false if full
    }

    private void processQueue() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                NotificationTask task = (NotificationTask) taskQueue.take(); // Blocks until a job arrives
                
                // Simulate third-party network call (e.g., SendGrid/Twilio API)
                Thread.sleep(150);

                // Save to database with JDBC transaction
                try (Connection conn = dataSource.getConnection()) {
                    conn.setAutoCommit(false);
                    String sql = "INSERT INTO notification_tasks (task_id, tenant_id, channel, recipient, message, status) VALUES (?, ?, ?, ?, ?, ?)";
                    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                        pstmt.setString(1, task.taskId());
                        pstmt.setString(2, task.tenantId());
                        pstmt.setString(3, task.channel());
                        pstmt.setString(4, task.recipient());
                        pstmt.setString(5, task.message());
                        pstmt.setString(6, "SENT");
                        pstmt.executeUpdate();
                        conn.commit();
                        System.out.println("[WORKER] Processed and persisted Task ID: " + task.taskId());
                    } catch (Exception e) {
                        conn.rollback();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}