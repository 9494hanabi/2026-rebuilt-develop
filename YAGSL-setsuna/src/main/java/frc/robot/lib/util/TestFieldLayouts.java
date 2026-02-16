package frc.robot.lib.util;

import edu.wpi.first.apriltag.AprilTag;
import edu.wpi.first.apriltag.AprilTagFieldLayout;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Rotation3d;
import java.util.List;

/**
 * テスト用のAprilTagレイアウト定義を集約するユーティリティ。
 */
public final class TestFieldLayouts {
  private static final double TEST_FIELD_LENGTH_METERS = 9.0;
  private static final double TEST_FIELD_WIDTH_METERS = 6.05;
  private static final double TEST_TAG_HEIGHT_METERS = 0.5;

  private TestFieldLayouts() {
    throw new UnsupportedOperationException("This is a utility class!");
  }

  /**
   * "AprilTag Test Map.fmap" に合わせた8タグレイアウトを生成する。
   * fmapはフィールド中心原点なので、ここで青同盟側コーナー原点へ変換する。
   */
  public static AprilTagFieldLayout createFromTestMapFmap() {
    List<AprilTag> tags = List.of(
        new AprilTag(1, poseFromCenter(4.323223304709560, 3.201776695290440, -135.0)),
        new AprilTag(2, poseFromCenter(4.676776695290440, 2.848223304709560, -135.0)),
        new AprilTag(3, poseFromCenter(4.676776695290440, -2.848223304709560, 135.0)),
        new AprilTag(4, poseFromCenter(4.323223304709560, -3.201776695290440, 135.0)),
        new AprilTag(5, poseFromCenter(-4.323223304709560, 3.201776695290440, -45.0)),
        new AprilTag(6, poseFromCenter(-4.676776695290440, 2.848223304709560, -45.0)),
        new AprilTag(7, poseFromCenter(-4.676776695290440, -2.848223304709560, 45.0)),
        new AprilTag(8, poseFromCenter(-4.323223304709560, -3.201776695290440, 45.0)));

    return new AprilTagFieldLayout(tags, TEST_FIELD_LENGTH_METERS, TEST_FIELD_WIDTH_METERS);
  }

  /**
   * "AprilTag Map Builder.fmap" に合わせた4タグレイアウトを生成する。
   * fmapはフィールド中心原点なので、ここで青同盟側コーナー原点へ変換する。
   */
  public static AprilTagFieldLayout createFromBuilderMapFmap() {
    List<AprilTag> tags = List.of(
        new AprilTag(1, poseFromCenter(4.500004860001312, 3.025000000000000, -135.0)),
        new AprilTag(2, poseFromCenter(4.500002429999999, -3.0250016334999996, 135.0)),
        new AprilTag(3, poseFromCenter(-4.500000000000000, 3.025000000000000, -45.0)),
        new AprilTag(4, poseFromCenter(-4.500000000000000, -3.025000000000000, 45.0)));

    return new AprilTagFieldLayout(tags, TEST_FIELD_LENGTH_METERS, TEST_FIELD_WIDTH_METERS);
  }

  /**
   * 既存呼び出し互換: 現在は Test Map(fmap) の8タグレイアウトを返す。
   */
  public static AprilTagFieldLayout createEightTagTestFieldLayout(
      double fieldLengthMeters,
      double fieldWidthMeters) {
    return createFromTestMapFmap();
  }

  private static Pose3d poseFromCenter(double xCenter, double yCenter, double yawDegrees) {
    return new Pose3d(
        xCenter + TEST_FIELD_LENGTH_METERS / 2.0,
        yCenter + TEST_FIELD_WIDTH_METERS / 2.0,
        TEST_TAG_HEIGHT_METERS,
        new Rotation3d(0, 0, Math.toRadians(yawDegrees)));
  }
}
