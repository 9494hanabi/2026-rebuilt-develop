package frc.robot.lib.limelight;

import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableInstance;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 複数台のLimelightから、現在使うべきターゲット観測を選ぶヘルパー。
 *
 * 方針:
 * 1) tv==1 かつ tid>=0 を「有効観測」とみなす
 * 2) 複数のカメラで見えている場合は ta が大きい方を優先する
 */

// === 担当者 ===
// ひなた
//

public class VisionTargetSelector {
  public static final class TargetObservation {
    private final String tableName;
    private final int tagId;
    private final double area;

    public TargetObservation(String tableName, int tagId, double area) {
      this.tableName = tableName;
      this.tagId = tagId;
      this.area = area;
    }

    public String tableName() {
      return tableName;
    }

    public int tagId() {
      return tagId;
    }

    public double area() {
      return area;
    }
  }

  private static final class CameraEntry {
    final String name;
    final NetworkTable table;

    CameraEntry(String name) {
      this.name = name;
      this.table = NetworkTableInstance.getDefault().getTable(name);
    }
  }

  private final List<CameraEntry> cameras = new ArrayList<>();

  public VisionTargetSelector(String... tableNames) {
    for (String name : tableNames) {
      if (name != null && !name.isBlank()) {
        cameras.add(new CameraEntry(name));
      }
    }
  }

  /** 全カメラから最もtaが大きい観測を返す */
  public Optional<TargetObservation> selectBestObservation() {
    TargetObservation best = null;
    for (CameraEntry camera : cameras) {
      var obs = readObservation(camera);
      if (obs.isPresent()) {
        if (best == null || obs.get().area() > best.area()) {
          best = obs.get();
        }
      }
    }
    return Optional.ofNullable(best);
  }

  /** 指定タグIDの観測を返す（複数カメラで見えていればtaが大きい方） */
  public Optional<TargetObservation> observationForTag(int tagId) {
    TargetObservation best = null;
    for (CameraEntry camera : cameras) {
      var obs = readObservation(camera).filter(o -> o.tagId() == tagId);
      if (obs.isPresent()) {
        if (best == null || obs.get().area() > best.area()) {
          best = obs.get();
        }
      }
    }
    return Optional.ofNullable(best);
  }

  /** 指定カメラで指定タグが見えているか */
  public boolean isTagVisible(String tableName, int tagId) {
    for (CameraEntry camera : cameras) {
      if (camera.name.equals(tableName)) {
        return readObservation(camera)
            .map(o -> o.tagId() == tagId)
            .orElse(false);
      }
    }
    return false;
  }

  private Optional<TargetObservation> readObservation(CameraEntry camera) {
    boolean seesTarget = camera.table.getEntry("tv").getDouble(0.0) > 0.0;
    int tagId = (int) camera.table.getEntry("tid").getDouble(-1.0);
    if (!seesTarget || tagId < 0) {
      return Optional.empty();
    }
    double area = camera.table.getEntry("ta").getDouble(0.0);
    return Optional.of(new TargetObservation(camera.name, tagId, area));
  }
}
