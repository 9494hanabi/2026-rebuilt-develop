// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.lib.util;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Transform2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.util.Units;

import edu.wpi.first.apriltag.AprilTagFieldLayout;
import edu.wpi.first.apriltag.AprilTagFields;

import java.util.Map;

// === 担当者 ===
// ひなた
//

public final class Constants {
  public static class OperatorConstants {
    public static final int kDriverControllerPort = 0;      // ドライバー用コントローラのUSBポート番号
    public static final double DEADBAND = 0.08;             // スティック入力の無効域（微小入力を無視）
  }

  public static class VisionConstants {
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

    public static final double kCameraAPitchDegrees = 20.0; // カメラAのピッチ角[deg]
    public static final double kCameraApitchRads = Units.degreesToRadians(kCameraAPitchDegrees);      // カメラAのピッチ角[rad]
    public static final double kCameraAHeightOffGroundMeters = Units.inchesToMeters(8.3787);  // カメラAの地上高[m]
    public static final String kLimelightATableName = "limelight-lb";                           // カメラAのNetworkTables名
    public static final double kRobotToCameraAForward = Units.inchesToMeters(7.8757);         // ロボット中心からカメラAの前方向オフセット[m]
    public static final double kRobotToCameraASide = Units.inchesToMeters(-11.9269);                  // ロボット中心からカメラAの横方向オフセット[m]
    public static final Rotation2d kCameraAYawOffset = Rotation2d.fromDegrees(0.0);    // カメラAのYawオフセット
    public static final Transform2d kRobotToCameraA =                                                 // ロボット座標系からカメラA座標系への変換
            new Transform2d(
                    new Translation2d(kRobotToCameraAForward, kRobotToCameraASide),
                    kCameraAYawOffset);
    
    // Camera B (Front-center camera)
    public static final double kCameraBPitchDegrees = 27.0;                                           // カメラBのピッチ角[deg]
    public static final double kCameraBPitchRads = Units.degreesToRadians(kCameraBPitchDegrees);      // カメラBのピッチ角[rad]
    public static final double kCameraBHeightOffGroundMeters = 0.2;                           // カメラBの地上高[m]
    public static final String kLimelightBTableName = "limelight-fc";                         // カメラBのNetworkTables名
    public static final String kFaceAprilTagTableName = kLimelightBTableName;                          // FaceAprilTag用
    public static final double kRobotToCameraBForward = 0.4275;                               // ロボット中心からカメラBの前方向オフセット[m]
    public static final double kRobotToCameraBSide = 0.0;                                     // ロボット中心からカメラBの横方向オフセット[m]
    public static final Rotation2d kCameraBYawOffset = Rotation2d.fromDegrees(0.0);           // カメラBのYawオフセット
    public static final Transform2d kRobotToCameraB =                                                 // ロボット座標系からカメラB座標系への変換
            new Transform2d(
                    new Translation2d(kRobotToCameraBForward, kRobotToCameraBSide),
                    kCameraBYawOffset);
    
    // Vision processing constants
    public static final double kDefaultAmbiguityThreshold = 0.19;       // タグ判定の曖昧さ閾値
    public static final double kDefaultYawDiffThreshold = 5.0;          // ヨー角差の許容範囲[deg]
    public static final double kTagAreaThresholdForYawCheck = 2.0;      // ヨー確認に必要なタグ面積の閾値
    public static final double kTagMinAreaForSingleTagMegatag = 1.0;    // 単一タグMegaTagに必要な最小面積
    public static final double kDefaultZThreshold = 0.2;                // Z方向の許容誤差閾値[m]
    public static final double kDefaultNormThreshold = 1.0;             // 平行移動ベクトルのノルム閾値
    public static final double kMaxVisionOdomDistanceMeter = 1.5;       // Odomとのフュージョン拒否距離閾値[m]
    public static final double kMinAmbiguityToFlip = 0.08;              // 反転判定に使う最小曖昧度

    public static final double kFieldPoseValidTimeoutSec = 0.5;         // 絶対座標を有効とみなす時間[s]

    public static final double kCameraHorizontalFOVDegrees = 81.0; // カメラの水平視野角[deg]
    public static final double kCameraVerticalFOVDegrees = 55.0;   // カメラの垂直視野角[deg]
    public static final int kCameraImageWidth = 1280; // カメラ画像の幅[pixel]
    public static final int kCameraImageHeight = 800; // カメラ画像の高さ[pixel]

    public static final double kScoringConfidenceThreshold = 0.7; // スコア判定の信頼度閾値

    // NetworkTables constants
    public static final String kBoundingBoxTableName = "BoundingBoxes"; // バウンディングボックスのテーブル名

    // FaceAprilTag: タグからのターゲット座標系オフセット (Interest Offset と整合)
    public static final Transform2d kFaceAprilTagTargetOffset = new Transform2d();
  }

  public static class FieldConstants {
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
            new Pose2d(fieldLengthMeter / 2.0, fieldWidthMeter / 2.0, Rotation2d.fromDegrees(0.0));
  }

  public static class SemiAutoConstants {
    // 速度係数 (m/s)/m
    public static final double translationGain = 1.2; // 目標位置誤差に対する並進ゲイン

    // クランプ m/s
    public static final double velocityMaximum = 1.5; // 並進速度の上限[m/s]

    // 回転速度係数 (rad/s)/rad
    public static final double angularGain = 3.0; // 目標角度誤差に対する回転ゲイン

    // クランプ rad/s
    public static final double omegaMaximum = 4.0; // 回転速度の上限[rad/s]

    // 許容誤差
    public static final double planeDeadbandMeter = 0.03; // 位置誤差のデッドバンド[m]
    public static final double thetaDeadbandDeg = 1; // 角度誤差のデッドバンド[deg]
    public static final double thetaDeadbandRad = Math.toRadians(thetaDeadbandDeg); // 角度誤差のデッドバンド[rad]

    // タグ検出時に一定方向へ進む速度
    public static final double kDriveOnTagSpeedMetersPerSec = 1.0;
    
  }

  public static class PIDConstants {

    // 調整の手順

    // 1. まず Ki と Kd を 0 にして、Kp だけで調整する
    //   - Kp を下げていき、振動しなくなるギリギリの値を見つける
    //   - 目標に到達できるが、オーバーシュートしない程度
    // 2. Kd を少しずつ上げる
    //   - 振動を抑え、目標付近でのブレーキ効果を出す
    //   - 上げすぎるとピクピクの原因になるので注意
    // 3. 最後に Ki を少しだけ入れる
    //   - 定常偏差（目標に近いけど完全に到達しない）がある場合のみ
    //   - Ki は非常に小さい値（0.01〜0.05程度）から始める

    public static final double kHeadingKp = 5.00;
    public static final double kHeadingKd = 0.18;
    public static final double kHeadingKi = 0;
    

    public static final double kTranslationKp = 3;
    public static final double kTranslationKd = 0.10;
    public static final double kTranslationKi = 0;
    
  }

  public static class CommandConstants {
    // Expected-TestFieldLayouts-TestMap.fmap のWPIBlue座標（タグ中心座標 + タグ姿勢）
    private static final double kMaxX = FieldConstants.fieldLengthMeter; // 6.05
    private static final double kMinX = 0.0;
    private static final double kMaxY = FieldConstants.fieldWidthMeter; // 9.00
    private static final double kMinY = 0.0;
    private static final double kInnerOffset = 0.3535533905932738; // sqrt(2)/4 [m]
    private static final double kNearMaxX = kMaxX - kInnerOffset; // 5.6964...
    private static final double kNearMinX = kMinX + kInnerOffset; // 0.3535...
    private static final double kNearMaxY = kMaxY - kInnerOffset; // 8.6464...
    private static final double kNearMinY = kMinY + kInnerOffset; // 0.3535...

    public static final Map<Integer, Pose2d> tagToVertexMap = Map.ofEntries(
        // +X +Y 側
        // tag 1: (6.0500, 8.6464), heading -135.0 deg
        Map.entry(1, new Pose2d(kMaxX, kNearMaxY, Rotation2d.fromDegrees(-135.0))),
        // tag 2: (5.6964, 9.0000), heading -135.0 deg
        Map.entry(2, new Pose2d(kNearMaxX, kMaxY, Rotation2d.fromDegrees(-135.0))),
        // -X +Y 側
        // tag 3: (0.3536, 9.0000), heading -45.0 deg
        Map.entry(3, new Pose2d(kNearMinX, kMaxY, Rotation2d.fromDegrees(-45.0))),
        // tag 4: (0.0000, 8.6464), heading -45.0 deg
        Map.entry(4, new Pose2d(kMinX, kNearMaxY, Rotation2d.fromDegrees(-45.0))),
        // +X -Y 側
        // tag 5: (6.0500, 0.3536), heading 135.0 deg
        Map.entry(5, new Pose2d(kMaxX, kNearMinY, Rotation2d.fromDegrees(135.0))),
        // tag 6: (5.6964, 0.0000), heading 135.0 deg
        Map.entry(6, new Pose2d(kNearMaxX, kMinY, Rotation2d.fromDegrees(135.0))),
        // -X -Y 側
        // tag 7: (0.3536, 0.0000), heading 45.0 deg
        Map.entry(7, new Pose2d(kNearMinX, kMinY, Rotation2d.fromDegrees(45.0))),
        // tag 8: (0.0000, 0.3536), heading 45.0 deg
        Map.entry(8, new Pose2d(kMinX, kNearMinY, Rotation2d.fromDegrees(45.0)))
    );
  }
  public static final double maxSpeed  = Units.feetToMeters(4.5); // 最大走行速度[m/s]
}
