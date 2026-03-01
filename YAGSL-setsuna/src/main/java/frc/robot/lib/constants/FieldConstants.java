package frc.robot.lib.constants;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import frc.robot.lib.util.FmapFieldLayoutLoader;
import edu.wpi.first.apriltag.AprilTagFieldLayout;
import edu.wpi.first.apriltag.AprilTagFields;

// === 担当者 ===
// ひなた
//

public final class FieldConstants {
    public static final double kMidlineBufferMeter = 5.0;   // 中央ライン判定用のバッファ距離[m]

    // === フィールド切り替えフラグ ===
    /** テスト用フィールドを使用するか（true: テスト用, false: 公式） */
        public static final boolean useTestField = true;

        // テスト用fmap (src/main/deploy からの相対パス)
        // Limelight側で使用している Expected マップと一致させる。
        private static final String testFieldFmapPath =
                "maps/fiducials.fmap";

    // 使用中のフィールドレイアウト
    public static final AprilTagFieldLayout kAprilTagLayout =
        useTestField
            ? FmapFieldLayoutLoader.loadFromDeploy(testFieldFmapPath)
            : AprilTagFieldLayout.loadField(AprilTagFields.kDefaultField);

    // フィールドサイズはレイアウトから取得
    public static final double fieldLengthMeter = kAprilTagLayout.getFieldLength();
    public static final double fieldWidthMeter = kAprilTagLayout.getFieldWidth();

    // 初期位置: フィールド中心（WPIBlue座標）
    public static final Pose2d kInitialFieldToRobotPose =
            new Pose2d(fieldLengthMeter / 2.0, fieldWidthMeter / 2.0, Rotation2d.fromDegrees(180.0));
}
