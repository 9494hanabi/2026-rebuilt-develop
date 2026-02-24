package frc.robot.lib.constants.commandconstants;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import frc.robot.lib.constants.FieldConstants;

import java.util.Map;

// === 担当者 ===
// ひなた
//

public final class SetToTagCommandConstants {

  // Expected-TestFieldLayouts-TestMap.fmap のWPIBlue座標（タグ中心座標 + タグ姿勢）
  private static final double kMaxX = FieldConstants.fieldLengthMeter; // 6.05
  private static final double kMinX = 0.0;
  private static final double kMaxY = FieldConstants.fieldWidthMeter; // 9.00
  private static final double kMinY = 0.0;
  private static final double kInnerOffsetMeter = 0.5; 
  private static final double kNearMaxX = kMaxX - kInnerOffsetMeter; // 5.55
  private static final double kNearMinX = kMinX + kInnerOffsetMeter; // 0.5
  private static final double kNearMaxY = kMaxY - kInnerOffsetMeter; // 8.5
  private static final double kNearMinY = kMinY + kInnerOffsetMeter; // 0.5

  public static final Map<Integer, Pose2d> tagToVertexMap = Map.ofEntries(
      // +X +Y 側
      // tag 1: (5.5500, 9.0000), heading -135.0 deg
      Map.entry(1, new Pose2d(kNearMaxX, kNearMaxY, Rotation2d.fromDegrees(-135.0))),
      // tag 2: (6.0500, 8.5000), heading -135.0 deg
      Map.entry(2, new Pose2d(kNearMaxX, kNearMaxY, Rotation2d.fromDegrees(-135.0))),
      // +X -Y 側
      // tag 3: (6.0500, 0.0500), heading -45.0 deg
      Map.entry(3, new Pose2d(kNearMaxX, kNearMinY, Rotation2d.fromDegrees(135.0))),
      // tag 4: (5.5500, 9.0000), heading -45.0 deg
      Map.entry(4, new Pose2d(kNearMaxX, kNearMinY, Rotation2d.fromDegrees(135.0))),
      // +X -Y 側
      // tag 5: (0.5000, 0.0000), heading 135.0 deg
      Map.entry(5, new Pose2d(kNearMinX, kNearMinY, Rotation2d.fromDegrees(45.0))),
      // tag 6: (0.0000, 0.5000), heading 135.0 deg
      Map.entry(6, new Pose2d(kNearMinX, kNearMinY, Rotation2d.fromDegrees(45.0))),
      // -X -Y 側
      // tag 7: (0.0000, 8.5000), heading 45.0 deg
      Map.entry(7, new Pose2d(kNearMinX, kNearMaxY, Rotation2d.fromDegrees(-45.0))),
      // tag 8: (0.5000, 9.0000), heading 45.0 deg
      Map.entry(8, new Pose2d(kNearMinX, kNearMaxY, Rotation2d.fromDegrees(-45.0)))
  );
} 
