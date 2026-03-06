package frc.robot.lib.util;

import static frc.robot.lib.constants.RobotConstants.*;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.interpolation.InterpolatingDoubleTreeMap;

import frc.robot.commands.debug.mechanism.ShootSetpoint;
import frc.robot.subsystems.mechanism.ShootAngleSubsystems;

/**
 * 弾道計算ヘルパ。
 * 放物線が指定座標(水平距離, 高さ)を通過するようにRPSと射出角度を算出する。
 *
 * 放物線の式: y = x·tanθ - g·x² / (2·v²·cos²θ)
 * 射出原点は (0, launchHeight) とし、指定座標を通過する条件から解を求める。
 */
public class TrajectoryCalculationHelper {

    // ===== 物理定数 =====
    private static final double kGravity = 9.80665; // 重力加速度 [m/s^2]

    // ===== 補間テーブル =====
    // 距離[m] → シューターRPS
    private static final InterpolatingDoubleTreeMap kDistanceToRpsMap = new InterpolatingDoubleTreeMap();
    // 距離[m] → 射出角度モーター回転[rot]
    private static final InterpolatingDoubleTreeMap kDistanceToAngleRotMap = new InterpolatingDoubleTreeMap();

    static {
        // === 補間テーブルデータ（実機で計測して更新すること） ===
        kDistanceToRpsMap.put(1.0, 50.0);
        kDistanceToRpsMap.put(2.0, 65.0);
        kDistanceToRpsMap.put(3.0, 80.0);
        kDistanceToRpsMap.put(4.0, 90.0);
        kDistanceToRpsMap.put(5.0, 95.0);

        kDistanceToAngleRotMap.put(1.0, -0.55);
        kDistanceToAngleRotMap.put(2.0, -0.48);
        kDistanceToAngleRotMap.put(3.0, -0.42);
        kDistanceToAngleRotMap.put(4.0, -0.38);
        kDistanceToAngleRotMap.put(5.0, -0.35);
    }

    private TrajectoryCalculationHelper() {}

    // ==================== 射出高さ計算 ====================

    /**
     * 投射角[deg]から実際のボール射出高さ[m]を算出する。
     *
     * θ' = 90 - launchAngleDeg
     * h_release = sin(θ') * kRollerToBallCenterMeters + kRollerAxisHeightMeters
     *
     * @param launchAngleDeg 投射角 [deg]
     * @return ボール射出高さ [m]
     */
    public static double calculateLaunchHeightMeters(double launchAngleDeg) {
        double thetaPrimeRad = Math.toRadians(90.0 - launchAngleDeg);
        return Math.sin(thetaPrimeRad) * kRollerToBallCenterMeters + kRollerAxisHeightMeters;
    }

    /**
     * モーター回転[rot]から実際のボール射出高さ[m]を算出する。
     */
    public static double calculateLaunchHeightMeters_fromMotorRot(double motorRot) {
        double launchAngleDeg = ShootAngleSubsystems.motorRotToLaunchAngleDeg(motorRot);
        return calculateLaunchHeightMeters(launchAngleDeg);
    }

    // ==================== 通過点指定方式 ====================

    /**
     * 放物線が指定座標 (distanceMeters, targetHeightMeters) を通過するような
     * 最適な ShootSetpoint (RPS, angleRot) を算出する。
     *
     * 射出原点: (0, launchHeight)
     * 通過点:   (distanceMeters, targetHeightMeters)
     *
     * 射出角度は機構の可動範囲内で探索し、その角度で通過点を通る速度を逆算する。
     * 可動範囲内で最も低いRPSとなる角度を選択する。
     *
     * @param distanceMeters 通過点の水平距離 [m]
     * @param targetHeightMeters 通過点の高さ [m] (地面=0)
     * @param safetyMargin 速度の安全マージン倍率 (例: 1.15)
     * @return 計算された ShootSetpoint。解がない場合は null。
     */
    public static ShootSetpoint calculateSetpointThroughPoint(
            double distanceMeters, double targetHeightMeters, double safetyMargin) {
        if (distanceMeters <= 0) {
            return null;
        }

        // 機構の可動範囲を投射角[deg]で取得
        double minAngleDeg = ShootAngleSubsystems.motorRotToLaunchAngleDeg(-0.8);   // ~44.84°
        double maxAngleDeg = ShootAngleSubsystems.motorRotToLaunchAngleDeg(-0.249);  // ~64.68°

        double bestRps = Double.MAX_VALUE;
        double bestAngleDeg = Double.NaN;

        // 可動範囲内を1°刻みで探索し、最も低RPSな角度を見つける
        for (double angleDeg = minAngleDeg; angleDeg <= maxAngleDeg; angleDeg += 1.0) {
            double speed = calculateSpeedThroughPoint(distanceMeters, targetHeightMeters, angleDeg);
            if (Double.isNaN(speed) || speed <= 0) {
                continue;
            }
            double rps = launchSpeedToRps(speed);
            if (rps < bestRps) {
                bestRps = rps;
                bestAngleDeg = angleDeg;
            }
        }

        // 粗探索で見つかった付近を0.1°刻みで精密探索
        if (!Double.isNaN(bestAngleDeg)) {
            double searchMin = Math.max(bestAngleDeg - 1.0, minAngleDeg);
            double searchMax = Math.min(bestAngleDeg + 1.0, maxAngleDeg);
            for (double angleDeg = searchMin; angleDeg <= searchMax; angleDeg += 0.1) {
                double speed = calculateSpeedThroughPoint(distanceMeters, targetHeightMeters, angleDeg);
                if (Double.isNaN(speed) || speed <= 0) {
                    continue;
                }
                double rps = launchSpeedToRps(speed);
                if (rps < bestRps) {
                    bestRps = rps;
                    bestAngleDeg = angleDeg;
                }
            }
        }

        if (Double.isNaN(bestAngleDeg) || bestRps == Double.MAX_VALUE) {
            return null;
        }

        double finalRps = bestRps * safetyMargin;
        double angleRot = ShootAngleSubsystems.launchAngleDegToMotorRot(bestAngleDeg);
        return new ShootSetpoint(finalRps, angleRot);
    }

    /**
     * 指定の投射角[deg]で、放物線が (distance, targetHeight) を通過するために
     * 必要な射出速度[m/s]を算出する。
     *
     * 放物線: y = x·tanθ - g·x² / (2·v²·cos²θ)
     * 通過条件: targetHeight - launchHeight = d·tanθ - g·d² / (2·v²·cos²θ)
     * → v² = g·d² / (2·cos²θ·(d·tanθ - deltaH))
     *
     * @param distanceMeters 通過点の水平距離 [m]
     * @param targetHeightMeters 通過点の高さ [m]
     * @param launchAngleDeg 投射角 [deg]
     * @return 必要な射出速度 [m/s]。解がない場合は NaN。
     */
    public static double calculateSpeedThroughPoint(
            double distanceMeters, double targetHeightMeters, double launchAngleDeg) {
        double launchHeight = calculateLaunchHeightMeters(launchAngleDeg);
        double deltaH = targetHeightMeters - launchHeight;
        double thetaRad = Math.toRadians(launchAngleDeg);
        double cosTheta = Math.cos(thetaRad);
        double tanTheta = Math.tan(thetaRad);
        double d = distanceMeters;

        // v² = g·d² / (2·cos²θ·(d·tanθ - deltaH))
        double denominator = d * tanTheta - deltaH;
        if (denominator <= 0) {
            // 放物線がこの角度では通過点に届かない
            return Double.NaN;
        }

        double v2 = (kGravity * d * d) / (2.0 * cosTheta * cosTheta * denominator);
        if (v2 <= 0) {
            return Double.NaN;
        }
        return Math.sqrt(v2);
    }

    /**
     * 地上の指定距離を通過する ShootSetpoint を算出する。
     * targetHeight = 0 (地面) として calculateSetpointThroughPoint を呼ぶ。
     *
     * @param distanceMeters 通過点の水平距離 [m]
     * @param safetyMargin 速度の安全マージン倍率
     * @return 計算された ShootSetpoint。解がない場合は null。
     */
    public static ShootSetpoint calculateOptimalSetpoint(double distanceMeters, double safetyMargin) {
        return calculateSetpointThroughPoint(distanceMeters, 0.0, safetyMargin);
    }

    // ==================== 補間テーブル方式 ====================

    /**
     * 補間テーブルから距離に応じた ShootSetpoint を取得する。
     */
    public static ShootSetpoint getSetpointFromTable(double distanceMeters) {
        double rps = kDistanceToRpsMap.get(distanceMeters);
        double angleRot = kDistanceToAngleRotMap.get(distanceMeters);
        return new ShootSetpoint(rps, angleRot);
    }

    /**
     * ロボットの現在位置とターゲット位置から補間テーブルで ShootSetpoint を取得する。
     */
    public static ShootSetpoint getSetpointFromTable(Pose2d robotPose, Translation2d targetPosition) {
        double distance = getHorizontalDistance(robotPose, targetPosition);
        return getSetpointFromTable(distance);
    }

    // ==================== 速度変換 ====================

    /**
     * モーターRPSから射出速度[m/s]を算出する。
     */
    public static double rpsToLaunchSpeed(double motorRps) {
        double rollerRps = motorRps * kShooterGearRatio;
        return 2.0 * Math.PI * kRollerEffectiveRadiusMeters * rollerRps * kSpinEfficiency;
    }

    /**
     * 射出速度[m/s]からモーターRPSを逆算する。
     */
    public static double launchSpeedToRps(double launchSpeedMps) {
        return launchSpeedMps / (2.0 * Math.PI * kRollerEffectiveRadiusMeters * kShooterGearRatio * kSpinEfficiency);
    }

    // ==================== ユーティリティ ====================

    /**
     * 2点間の水平距離を計算する。
     */
    public static double getHorizontalDistance(Pose2d robotPose, Translation2d targetPosition) {
        return robotPose.getTranslation().getDistance(targetPosition);
    }

    /**
     * ロボットからターゲットへの方位角を計算する。
     */
    public static double getAngleToTarget(Pose2d robotPose, Translation2d targetPosition) {
        Translation2d diff = targetPosition.minus(robotPose.getTranslation());
        return Math.atan2(diff.getY(), diff.getX());
    }

    /**
     * 射出角度[deg]をモーター回転[rot]に変換する。
     */
    public static double degreesToMotorRot(double degrees) {
        return ShootAngleSubsystems.launchAngleDegToMotorRot(degrees);
    }

    /**
     * モーター回転[rot]を射出角度[deg]に変換する。
     */
    public static double motorRotToDegrees(double rot) {
        return ShootAngleSubsystems.motorRotToLaunchAngleDeg(rot);
    }
}
