package frc.robot.commands.debug.vision;

import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableInstance;
import java.util.Optional;

/**
 * A/B 2台のLimelightから、現在使うべきターゲット観測を1つ選ぶヘルパー。
 *
 * 方針:
 * 1) tv==1 かつ tid>=0 を「有効観測」とみなす
 * 2) 両方見えている場合は ta が大きい方を優先する
 */
final class DebugVisionTargetSelector {
  static final class TargetObservation {
    private final String tableName;
    private final int tagId;
    private final double area;

    TargetObservation(String tableName, int tagId, double area) {
      this.tableName = tableName;
      this.tagId = tagId;
      this.area = area;
    }

    String tableName() {
      return tableName;
    }

    int tagId() {
      return tagId;
    }

    double area() {
      return area;
    }
  }

  private final String tableNameA;
  private final String tableNameB;
  private final NetworkTable tableA;
  private final NetworkTable tableB;

  DebugVisionTargetSelector(String tableNameA, String tableNameB) {
    this.tableNameA = isValidTableName(tableNameA) ? tableNameA : null;
    this.tableNameB = isValidTableName(tableNameB) ? tableNameB : null;
    this.tableA =
        this.tableNameA == null ? null : NetworkTableInstance.getDefault().getTable(this.tableNameA);
    this.tableB =
        this.tableNameB == null ? null : NetworkTableInstance.getDefault().getTable(this.tableNameB);
  }

  Optional<TargetObservation> selectBestObservation() {
    Optional<TargetObservation> a = readObservation(tableNameA, tableA);
    Optional<TargetObservation> b = readObservation(tableNameB, tableB);
    if (a.isPresent() && b.isPresent()) {
      return b.get().area() > a.get().area() ? b : a;
    }
    return a.isPresent() ? a : b;
  }

  Optional<TargetObservation> observationForTag(int tagId) {
    Optional<TargetObservation> a = readObservation(tableNameA, tableA).filter(o -> o.tagId() == tagId);
    Optional<TargetObservation> b = readObservation(tableNameB, tableB).filter(o -> o.tagId() == tagId);
    if (a.isPresent() && b.isPresent()) {
      return b.get().area() > a.get().area() ? b : a;
    }
    return a.isPresent() ? a : b;
  }

  boolean isTagVisible(String tableName, int tagId) {
    return readObservation(tableName, tableFor(tableName))
        .map(observation -> observation.tagId() == tagId)
        .orElse(false);
  }

  private Optional<TargetObservation> readObservation(String tableName, NetworkTable table) {
    if (tableName == null || table == null) {
      return Optional.empty();
    }
    boolean seesTarget = table.getEntry("tv").getDouble(0.0) > 0.0;
    int tagId = (int) table.getEntry("tid").getDouble(-1.0);
    if (!seesTarget || tagId < 0) {
      return Optional.empty();
    }
    double area = table.getEntry("ta").getDouble(0.0);
    return Optional.of(new TargetObservation(tableName, tagId, area));
  }

  private NetworkTable tableFor(String tableName) {
    if (tableName == null) {
      return null;
    }
    if (tableName.equals(tableNameA)) {
      return tableA;
    }
    if (tableName.equals(tableNameB)) {
      return tableB;
    }
    return null;
  }

  private boolean isValidTableName(String tableName) {
    return tableName != null && !tableName.isBlank();
  }
}
