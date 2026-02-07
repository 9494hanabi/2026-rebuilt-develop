package frc.robot.lib.util;

import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.RobotBase;
import edu.wpi.first.wpilibj.Timer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public class LogCleanupService {
    private static final Path LOG_DIR = Path.of("/home/lvuser/logs");

    // Cleanup cadence and retention policy.
    private static final double CLEANUP_INTERVAL_SEC = 600.0; // 10 min
    private static final Duration RETENTION = Duration.ofDays(3);
    private static final Duration ACTIVE_FILE_GUARD = Duration.ofMinutes(30);
    private static final long MAX_TOTAL_BYTES = 512L * 1024L * 1024L; // 512 MB

    private double nextCleanupSec = 0.0;

    public void periodic() {
        if (!RobotBase.isReal()) {
            return;
        }

        double nowSec = Timer.getFPGATimestamp();
        if (nowSec < nextCleanupSec) {
            return;
        }
        nextCleanupSec = nowSec + CLEANUP_INTERVAL_SEC;

        try {
            int deleted = cleanupNow();
            if (deleted > 0) {
                DriverStation.reportWarning(
                        "LogCleanup: deleted " + deleted + " old log files from " + LOG_DIR, false);
            }
        } catch (IOException e) {
            DriverStation.reportWarning(
                    "LogCleanup failed: " + e.getClass().getSimpleName() + ": " + e.getMessage(), false);
        }
    }

    private int cleanupNow() throws IOException {
        if (!Files.isDirectory(LOG_DIR)) {
            return 0;
        }

        Instant now = Instant.now();
        Instant retentionCutoff = now.minus(RETENTION);
        Instant activeGuardCutoff = now.minus(ACTIVE_FILE_GUARD);

        int deletedCount = 0;
        List<Path> files = listRegularFiles();

        // First pass: remove files older than retention.
        for (Path file : files) {
            Instant modified = getLastModified(file);
            if (modified.isBefore(retentionCutoff) && modified.isBefore(activeGuardCutoff)) {
                if (Files.deleteIfExists(file)) {
                    deletedCount++;
                }
            }
        }

        // Second pass: cap total size by deleting oldest files first.
        files = listRegularFiles();
        long totalBytes = 0L;
        List<Path> candidates = new ArrayList<>();
        for (Path file : files) {
            totalBytes += safeSize(file);
            if (getLastModified(file).isBefore(activeGuardCutoff)) {
                candidates.add(file);
            }
        }

        if (totalBytes > MAX_TOTAL_BYTES) {
            candidates.sort(Comparator.comparing(this::getLastModified));
            for (Path file : candidates) {
                if (totalBytes <= MAX_TOTAL_BYTES) {
                    break;
                }
                long size = safeSize(file);
                if (Files.deleteIfExists(file)) {
                    deletedCount++;
                    totalBytes = Math.max(0L, totalBytes - size);
                }
            }
        }

        return deletedCount;
    }

    private List<Path> listRegularFiles() throws IOException {
        List<Path> files = new ArrayList<>();
        try (Stream<Path> stream = Files.list(LOG_DIR)) {
            stream.filter(Files::isRegularFile).forEach(files::add);
        }
        return files;
    }

    private Instant getLastModified(Path path) {
        try {
            FileTime time = Files.getLastModifiedTime(path);
            return time.toInstant();
        } catch (IOException e) {
            return Instant.EPOCH;
        }
    }

    private long safeSize(Path path) {
        try {
            return Files.size(path);
        } catch (IOException e) {
            return 0L;
        }
    }
}
