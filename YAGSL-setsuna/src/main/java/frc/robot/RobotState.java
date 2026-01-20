package frc.robot;

import frc.robot.subsystems.vision.VisionFieldPoseEstimate;
import frc.robot.lib.util.ConcurrentTimeInterpolatableBuffer;
import frc.robot.lib.util.MathHelpers;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Twist2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.DriverStation.Alliance;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.IntSupplier; 

import org.littletonrobotics.junction.Logger;

// ======================================================================================
// Editer : ひなた
//

public class RobotState {
    // 参照のためのバッファ長
    public static final double LOOKBACK_TIME_SEC = 1.0;

    // ビジョン推定を外部へ引き渡すコールバック
    private final Consumer<VisionFieldPoseEstimate> visionEstimateConsumer;

    // TimeStampを初期化
    // メソッドの副作用(変数更新)を目的とするインターフェース"Consumer"を利用している。
    public RobotState(Consumer<VisionFieldPoseEstimate> visionEstimateConsumer) {
        this.visionEstimateConsumer = visionEstimateConsumer;
        fieldToRobot.addSample(0.0, MathHelpers.kPose2dZero);
        driveYawAngularVelocity.addSample(0.0, 0.0);

        // 機構のポジションを将来的にここに記述する。
    }

    // ---- 走行状態 ----
    // ロボット姿勢の履歴
    private final ConcurrentTimeInterpolatableBuffer<Pose2d> fieldToRobot = 
            ConcurrentTimeInterpolatableBuffer.createBuffer(LOOKBACK_TIME_SEC);
    
    // ロボット座標系速度
    private final AtomicReference<ChassisSpeeds> measuredRobotRelativeChassisSpeeds =
            new AtomicReference<>(new ChassisSpeeds());

    // フィールド座標系速度
    private final AtomicReference<ChassisSpeeds> measuredFieldRelativeChassisSpeeds =
            new AtomicReference<>(new ChassisSpeeds());
    
    // 目標のロボット座標系速度
    private final AtomicReference<ChassisSpeeds> desiredRobotRelativeChassisSpeeds =
            new AtomicReference<>(new ChassisSpeeds());
    
    // 目標のフィールド座標系速度
    private final AtomicReference<ChassisSpeeds> desiredFieldRelativeChassisSpeeds =
            new AtomicReference<>(new ChassisSpeeds());
    
    // ビジョンなどを含む融合速度
    private final AtomicReference<ChassisSpeeds> fusedFieldRelativeChassisSpeeds =
            new AtomicReference<>(new ChassisSpeeds());
    
    // ループの反復回数
    private final AtomicInteger iteration = new AtomicInteger(0);

    // 直近で採用したMegatag推定の情報
    private double lastUsedMegatagTimestamp = 0;
    private Pose2d lastUsedMegatagPose = Pose2d.kZero;

    // 角速度/姿勢/加速度の履歴
    // ヨー角速度
    private final ConcurrentTimeInterpolatableBuffer<Double> driveYawAngularVelocity =
            ConcurrentTimeInterpolatableBuffer.createDoubleBuffer(LOOKBACK_TIME_SEC);
    
    // ロール角速度
    private final ConcurrentTimeInterpolatableBuffer<Double> driveRollAngularVelocity =
            ConcurrentTimeInterpolatableBuffer.createDoubleBuffer(LOOKBACK_TIME_SEC);
    
    // ピッチ角速度
    private final ConcurrentTimeInterpolatableBuffer<Double> drivePitchAngularVelocity =
            ConcurrentTimeInterpolatableBuffer.createDoubleBuffer(LOOKBACK_TIME_SEC);
    
    // 基準水平面からの絶対角(ロール)
    private final ConcurrentTimeInterpolatableBuffer<Double> drivePitchRads =
            ConcurrentTimeInterpolatableBuffer.createDoubleBuffer(LOOKBACK_TIME_SEC);
    
    // 基準水平面からの絶対角(ピッチ)
    private final ConcurrentTimeInterpolatableBuffer<Double> driveRollRads = 
            ConcurrentTimeInterpolatableBuffer.createDoubleBuffer(LOOKBACK_TIME_SEC);
    
    // X軸への加速度
    private final ConcurrentTimeInterpolatableBuffer<Double> accelX = 
            ConcurrentTimeInterpolatableBuffer.createDoubleBuffer(LOOKBACK_TIME_SEC);
    
    // Y軸への加速度
    private final ConcurrentTimeInterpolatableBuffer<Double> accelY =
            ConcurrentTimeInterpolatableBuffer.createDoubleBuffer(LOOKBACK_TIME_SEC);
    
    // 自動経路キャンセルのフラグ
    private final AtomicBoolean enablePathCancel = new AtomicBoolean(false);

    // Auto開始時刻
    private double autoStartTime;

    // 追従中の軌道の目標/現在姿勢
    private Optional<Pose2d> trajectoryTargetPose = Optional.empty();
    private Optional<Pose2d> trajectoryCurrentPose = Optional.empty();

    // Auto開始時刻のセッター
    public void setAutoStartTime(double timestamp) {
        autoStartTime = timestamp;
    }

    // Auto開始時刻のゲッター
    public double getAutoStartTime() {
        return autoStartTime;
    }

    // 経路キャンセルのセッター
    public void enablePathCancel() {
        enablePathCancel.set(true);
    }

    public void disablePathCancel() {
        enablePathCancel.set(false);
    }

    // 経路キャンセルフラグのゲッター
    public boolean getPathCancel() {
        return enablePathCancel.get();
    }

    // ポーズサンプルのadder
    public void addOdometryMeasurement(double timestamp, Pose2d pose) {
        fieldToRobot.addSample(timestamp, pose);
    }

    // イテレーション(periodicなど)のカウンタ
    public void incrementIterationCount() {
        iteration.incrementAndGet();
    }

    // カウンタのゲッター
    public int getIteration() {
        return iteration.get();
    }

    public IntSupplier getIteratioIntSupplier() {
        return () -> getIteration();
    }

    public void addDriveMotionMeasurements() {
        double timestamp;
        double angularRollRads;
    }
}