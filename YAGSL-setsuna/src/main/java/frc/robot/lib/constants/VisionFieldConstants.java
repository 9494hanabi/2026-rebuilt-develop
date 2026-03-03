package frc.robot.lib.constants;

import edu.wpi.first.apriltag.AprilTagFieldLayout;
import edu.wpi.first.apriltag.AprilTagFields;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Transform2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.util.Units;
import frc.robot.lib.util.FmapFieldLayoutLoader;

public class VisionFieldConstants {
    public static final boolean useTestField = true;
    public static final boolean useOfficialField = false;

    public static final boolean useMegaTag2 = true;         // MegaTag2推定を使うか
    public static final String limelightName = "limelight"; // LimelightのNetworkTables名
    public static final double kLargeVariance = 1e6;        // 推定を無効に近づけるための大きな分散値
    public static final int kMegatag1XStdDevIndex = 0;      // MegaTag1のX分散インデックス
    public static final int kMegatag1YStdDevIndex = 1;      // MegaTag1のY分散インデックス
    public static final int kMegatag1YawStdDevIndex = 5;    // MegaTag1のYaw分散インデックス

    public static final int kMegatag2XStdDevIndex = 6;      // MegaTag2のX分散インデックス
    public static final int kMegatag2YStdDevIndex = 7;      // MegaTag2のY分散インデックス
    public static final int kMegatag2YawStdDevIndex = 11;   // MegaTag2のYaw分散インデックス

    public static final int kExpectedStdDevArrayLength = 12;// 期待される分散配列の長さ
    public static final int kMinFiducialCount = 1;          // 推定に必要な最小タグ数
    public static final double kMidlineBufferMeter = 5.0;   // 中央ライン判定用のバッファ距離[m]

    // テスト用fmap (src/main/deploy からの相対パス)
    // Limelight側で使用している Expected マップと一致させる。
    private static final String kTestFieldFmapPath = "maps/fiducials.fmap";

    static {
        if (useTestField == useOfficialField) {
            throw new IllegalStateException(
                    "Exactly one of useTestField or useOfficialField must be true.");
        }
    }

    // 使用中のフィールドレイアウト
    public static final AprilTagFieldLayout kAprilTagLayout =
            useTestField
                    ? FmapFieldLayoutLoader.loadFromDeploy(kTestFieldFmapPath)
                    : AprilTagFieldLayout.loadField(AprilTagFields.kDefaultField);

    // フィールドサイズはレイアウトから取得
    public static final double fieldLengthMeter = kAprilTagLayout.getFieldLength();
    public static final double fieldWidthMeter = kAprilTagLayout.getFieldWidth();

    // 初期位置: フィールド中心（WPIBlue座標）
    public static final Pose2d kInitialFieldToRobotPose =
            new Pose2d(
                    fieldLengthMeter / 2.0,
                    fieldWidthMeter / 2.0,
                    Rotation2d.fromDegrees(180.0));

    public static final double kCameraAPitchDegrees = 20.0; // カメラAのピッチ角[deg]
    public static final double kCameraApitchRads =
            Units.degreesToRadians(kCameraAPitchDegrees);      // カメラAのピッチ角[rad]
    public static final double kCameraAHeightOffGroundMeters =
            Units.inchesToMeters(8.3787);  // カメラAの地上高[m]
    public static final String kLimelightATableName = "limelight-lb"; // カメラAのNetworkTables名
    public static final double kRobotToCameraAForward =
            Units.inchesToMeters(7.8757);  // ロボット中心からカメラAの前方向オフセット[m]
    public static final double kRobotToCameraASide =
            Units.inchesToMeters(-11.9269); // ロボット中心からカメラAの横方向オフセット[m]
    public static final Rotation2d kCameraAYawOffset =
            Rotation2d.fromDegrees(0.0);    // カメラAのYawオフセット
    public static final Transform2d kRobotToCameraA =
            new Transform2d(
                    new Translation2d(kRobotToCameraAForward, kRobotToCameraASide),
                    kCameraAYawOffset);

    // Camera B (Front-center camera)
    public static final double kCameraBPitchDegrees = 30.0; // カメラBのピッチ角[deg]
    public static final double kCameraBPitchRads =
            Units.degreesToRadians(kCameraBPitchDegrees);      // カメラBのピッチ角[rad]
    public static final double kCameraBHeightOffGroundMeters = 0.2; // カメラBの地上高[m]
    public static final String kLimelightBTableName = "limelight-fc"; // カメラBのNetworkTables名
    public static final String kFaceAprilTagTableName = kLimelightBTableName; // FaceAprilTag用
    public static final double kRobotToCameraBForward = 0.4275; // ロボット中心からカメラBの前方向オフセット[m]
    public static final double kRobotToCameraBSide = 0.0; // ロボット中心からカメラBの横方向オフセット[m]
    public static final Rotation2d kCameraBYawOffset =
            Rotation2d.fromDegrees(0.0); // カメラBのYawオフセット
    public static final Transform2d kRobotToCameraB =
            new Transform2d(
                    new Translation2d(kRobotToCameraBForward, kRobotToCameraBSide),
                    kCameraBYawOffset);

    // Vision processing constants
    public static final double kDefaultAmbiguityThreshold = 0.19; // タグ判定の曖昧さ閾値
    public static final double kDefaultYawDiffThreshold = 5.0; // ヨー角差の許容範囲[deg]
    public static final double kTagAreaThresholdForYawCheck = 2.0; // ヨー確認に必要なタグ面積の閾値
    public static final double kTagMinAreaForSingleTagMegatag = 1.0; // 単一タグMegaTagに必要な最小面積
    public static final double kDefaultZThreshold = 0.2; // Z方向の許容誤差閾値[m]
    public static final double kDefaultNormThreshold = 1.0; // 平行移動ベクトルのノルム閾値
    public static final double kMaxVisionOdomDistanceMeter = 1.5; // Odomとのフュージョン拒否距離閾値[m]
    public static final double kMinAmbiguityToFlip = 0.08; // 反転判定に使う最小曖昧度

    public static final double kFieldPoseValidTimeoutSec = 0.5; // 絶対座標を有効とみなす時間[s]

    public static final double kCameraHorizontalFOVDegrees = 81.0; // カメラの水平視野角[deg]
    public static final double kCameraVerticalFOVDegrees = 55.0;   // カメラの垂直視野角[deg]
    public static final int kCameraImageWidth = 1280; // カメラ画像の幅[pixel]
    public static final int kCameraImageHeight = 800; // カメラ画像の高さ[pixel]

    public static final double kScoringConfidenceThreshold = 0.7; // スコア判定の信頼度閾値

    // NetworkTables constants
    public static final String kBoundingBoxTableName = "BoundingBoxes"; // バウンディングボックスのテーブル名

    // FaceAprilTag: タグからのターゲット座標系オフセット (Interest Offset と整合)
    public static final Transform2d kFaceAprilTagTargetOffset = new Transform2d();

    protected VisionFieldConstants() {
        throw new UnsupportedOperationException("This is a constants class!");
    }
}
