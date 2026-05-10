package org.example.Util;

import org.example.Service.BackupService;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class BackupScheduler {
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final BackupService backupService = new BackupService();

    public void startAutoBackup() {
        // Run the backup immediately, then every 1 hour
        scheduler.scheduleAtFixedRate(() -> {
            try {
                backupService.backupAllTables();
            } catch (Exception e) {
                System.err.println("Unexpected error in backup scheduler: " + e.getMessage());
                e.printStackTrace();
            }
        }, 0, 60, TimeUnit.MINUTES);
        
        System.out.println("Auto-backup scheduler started (Frequency: 1 hour).");
    }

    public void stop() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
        System.out.println("Auto-backup scheduler stopped.");
    }
}
