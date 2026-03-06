package frc.robot.lib.limelight;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.wpi.first.wpilibj.Filesystem;

import java.io.File;
import java.io.IOException;
import java.util.List;

// === 担当 ===
// ひなた
//
// limelights.json から設定を読み込むクラス

public class LimelightConfig {
    private static LimelightConfig instance;
    private LimelightsJson config;

    // JSON構造に対応するクラス
    public static class LimelightsJson {
        public List<LimelightEntry> limelights;
    }

    public static class LimelightEntry {
        public String name;
        public String table;
        public boolean enabled;
        public RobotToCamera robotToCamera;
        public CameraPose cameraPose;
        public double stdDevScale;
        public double pipeline;
    }

    public static class RobotToCamera {
        // WPILib座標系 (Blue origin準拠):
        // +X=forward, +Y=left, yaw+CCW
        // Limelight RobotSpace(+Y=right)への変換はVisionIOHardwareLimelightで行う。
        public double x;
        public double y;
        public double yawDeg;
    }

    public static class CameraPose {
        public double heightMeters;
        public double pitchDeg;
    }

    private LimelightConfig() {
        loadConfig();
    }

    public static LimelightConfig getInstance() {
        if (instance == null) {
            instance = new LimelightConfig();
        }
        return instance;
    }

    private void loadConfig() {
        try {
            File configFile = new File(Filesystem.getDeployDirectory(), "limelight/limelights.json");
            ObjectMapper mapper = new ObjectMapper()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            config = mapper.readValue(configFile, LimelightsJson.class);
        } catch (IOException e) {
            System.err.println("Failed to load limelights.json: " + e.getMessage());
            config = new LimelightsJson();
            config.limelights = List.of();
            return;
        }

        if (config == null || config.limelights == null) {
            config = new LimelightsJson();
            config.limelights = List.of();
        }
    }

    /** 少なくとも1つのLimelightが有効かどうか */
    public boolean isAnyLimelightEnabled() {
        if (config == null || config.limelights == null) {
            return false;
        }
        return config.limelights.stream().anyMatch(ll -> ll.enabled);
    }

    /** 有効なLimelightのリストを取得 */
    public List<LimelightEntry> getEnabledLimelights() {
        if (config == null || config.limelights == null) {
            return List.of();
        }
        return config.limelights.stream()
                .filter(ll -> ll != null && ll.enabled)
                .toList();
    }

    /** 全てのLimelight設定を取得 */
    public List<LimelightEntry> getAllLimelights() {
        if (config == null || config.limelights == null) {
            return List.of();
        }
        return config.limelights.stream()
                .filter(ll -> ll != null)
                .toList();
    }

    /** 有効なLimelightのNetworkTables名を取得 */
    public String[] getEnabledTableNames() {
        return getEnabledLimelights().stream()
                .map(ll -> ll.table)
                .filter(table -> table != null && !table.isBlank())
                .distinct()
                .toArray(String[]::new);
    }
}
