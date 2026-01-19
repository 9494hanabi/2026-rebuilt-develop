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

// import org.littletonrobotics.junction.Logger;

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

    public void addDriveMotionMeasurements(
                double timestamp,
                double angularRollRadsPerS,
                double angularPitchRadsPerS,
                double angularYawRadsPerS,
                double pitchRads,
                double rollRads,
                double accelX,
                double accelY,
                ChassisSpeeds desiredRobotRelativeChassisSpeeds,
                ChassisSpeeds desiredFieldRelativeSpeeds,
                ChassisSpeeds measuredSpeeds,
                ChassisSpeeds measuredFieldRelativeSpeeds,
                ChassisSpeeds fusedFieldRelativeSpeeds) {
        // 走行状態をまとめて更新
        this.driveRollAngularVelocity.addSample(timestamp, angularRollRadsPerS);
        this.drivePitchAngularVelocity.addSample(timestamp, angularPitchRadsPerS);
        this.driveYawAngularVelocity.addSample(timestamp, angularYawRadsPerS);
        this.drivePitchRads.addSample(timestamp, pitchRads);
        this.driveRollRads.addSample(timestamp, rollRads);
        this.accelY.addSample(timestamp, accelY);
        this.accelX.addSample(timestamp, accelX);
        this.desiredRobotRelativeChassisSpeeds.set(desiredRobotRelativeChassisSpeeds);
        this.desiredFieldRelativeChassisSpeeds.set(desiredFieldRelativeSpeeds);
        this.measuredRobotRelativeChassisSpeeds.set(measuredSpeeds);
        this.measuredFieldRelativeChassisSpeeds.set(measuredFieldRelativeSpeeds);
        this.fusedFieldRelativeChassisSpeeds.set(fusedFieldRelativeSpeeds);
    }

    public Map.Entry<Double, Pose2d> getLatestFieldToRobot() {
        return fieldToRobot.getLatest();
    }

    // 現在の速度から短時間先の姿勢を予測する。
    public Pose2d getPredicatedFieldToRobot(double lookaheadTimeS) {
        var maybeFieldToRobot = getLatestFieldToRobot();
        Pose2d fieldToRobot =
                maybeFieldToRobot == null ? MathHelpers.kPose2dZero : maybeFieldToRobot.getValue();
        var delta = getLatestRobotRelativeChassisSpeed();
        delta = delta.times(lookaheadTimeS);
        return fieldToRobot.exp(
                new Twist2d(
                        delta.vxMetersPerSecond,
                        delta.vyMetersPerSecond,
                        delta.omegaRadiansPerSecond));
    }

    // 予測時に-方向の草土をゼロに制限する。(非ホロノミック用)
    public Pose2d getPredicateCappedFieldToRobot(double lookaheadTimeS) {
        var maybeFieldToRobot = getLatestFieldToRobot();
        Pose2d fieldToRobot = 
                        maybeFieldToRobot == null ? MathHelpers.kPose2dZero : maybeFieldToRobot.getValue();
        var delta = getLatestRobotRelativeChassisSpeed();
        delta = delta.times(lookaheadTimeS);
        return fieldToRobot.exp(
                new Twist2d(
                        Math.max(0.0, delta.vxMetersPerSecond),
                        Math.max(0.0, delta.vyMetersPerSecond),
                        delta.omegaRadiansPerSecond
                )
        );
    }

    public Optional<Pose2d> getFieldToRobot(double timestamp) {
        return fieldToRobot.getSample(timestamp);
    }

    public ChassisSpeeds getLatestMeasuredFieldRelativeChassisSpeeds() {
        return measuredFieldRelativeChassisSpeeds.get();
    }

    public ChassisSpeeds getLatestRobotRelativeChassisSpeed() {
        return measuredRobotRelativeChassisSpeeds.get();
    }

    public ChassisSpeeds getLatestDesiredRobotRelativeChassisSpeeds() {
        return desiredRobotRelativeChassisSpeeds.get();
    }

    public ChassisSpeeds getLatestDesiredFieldRelativeChassisSpeed() {
        return desiredFieldRelativeChassisSpeeds.get();
    }

    public ChassisSpeeds getLatestFusedFieldRelativeChassisSpeed() {
        return fusedFieldRelativeChassisSpeeds.get();
    }

    public ChassisSpeeds getLatestFusedRobotRelativeChassisSpeeds() {
        var speeds = getLatestRobotRelativeChassisSpeed();
        speeds.omegaRadiansPerSecond = 
                        getLatestFusedFieldRelativeChassisSpeed().omegaRadiansPerSecond;
        return speeds;
    }

    // ledは使う予定無いので未再現
    //     public void setLedState(LedState state) {
    //         ledState.set(state);
    //     }

    //     public LedState getLedState() {
    //         return ledState.get();
    //     }

    private Optional<Double> getMaxAbsoluteInRnage(
                ConcurrentTimeInterpolatableBuffer<Dobule> buffer, double minTime, double maxTime) {
        var submap = buffer.getInternalBuffer().subMap(minTime, maxTime).values();
        var max = submap.stream().max(Double::compare);
        var min = submap.stream().min(Double::compare);
        if (max.isEmpty() || min.isEmpty()) return Optional.empty();
        if (Math.abs(max.get()) >= Math.abs(min.get()) return max);
        else return min;
    }

    public Optional<Double> getMaxAbsDriveYawAngularVelocityInRnage(
            double minTime, double maxTime) {
        if (Robot.isReal()) return getMaxAbsValueInRnage(driveYawAngularVelocity, minTime, maxTime);
        return Optional.of(measuredRobotRelativeChassisSpeeds.get().omegaRadiansPerSecond);
    }

    public Optional<Double> getMaxAbsDrivePitchAngularVelocityInRange(
            double minTime, double maxTime) {
        return getMaxAbsValueInRange(drivePitchAngularVelocity, minTime, maxTime);
    }

    public Optional<Double> getMaxAbsDriveRollAngularVelocityInRange(
            double mitTime, double maxTime) {
        return getMaxAbsValueInRange(driveRollAngularVelocity, mitTime, maxTime);
    }

    public void updateMegatagEstimate(VisionFieldPoseEstimate megatagEstimate) {
        lastUsedMegatagTimestamp = megatagEstimate.getTimestampSeconds();
        lastUsedMegatagPose = megatagEstimate.getVisionRobotPoseMeters();
        visionEstimateConsumer.accept(megatagEstimate);
    }

    public double lastUsedMegatagTimestamp() {
        return lastUsedMegatagTimestamp;
    }

    public Pose2d lastUsedMegatagPose() {
        return lastUsedMegatagPose;
    }

    public boolean isRedAlliance() {
        return DriverStation.getAlliance().isPresent()
                && DriverStation.getAlliance().equals(Optional.of(Alliance.Red));
    }

    public void updateLogger() {
        if (this.driverYawAngularVelocity.getInternalBuffer().lastEntry() != null) {
            Logger.recordOutput(
                "RobotState/YwaAngularVelocity",
                this.driveYawAngularVelocity.getInternalBuffer().lastEntry().getValue()
            );
        }
        if (this.)
    }
}
