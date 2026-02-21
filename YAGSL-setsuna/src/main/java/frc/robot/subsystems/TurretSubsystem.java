// src/main/java/frc/robot/subsystems/TurretSubsystem.java
package frc.robot.subsystems;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.controller.ProfiledPIDController;
import edu.wpi.first.math.controller.SimpleMotorFeedforward;
import edu.wpi.first.math.system.plant.DCMotor;
import edu.wpi.first.math.system.plant.LinearSystemId;
import edu.wpi.first.math.trajectory.TrapezoidProfile;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.wpilibj.RobotBase;
import edu.wpi.first.wpilibj.RobotController;
import edu.wpi.first.wpilibj.motorcontrol.PWMSparkMax;
import edu.wpi.first.wpilibj.simulation.DCMotorSim;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.SubsystemBase;

/* === 担当者 ===
 * はるた
 */
public class TurretSubsystem extends SubsystemBase {
  // WPILib標準ループ周期
  private static final double kLoopPeriodSec = 0.02;

  // ===== ハード情報（TODO: 実機仕様で更新）=====
  private static final int kTurretMotorPwmPort = 1;
  private static final double kMinAngleDeg = -170.0;
  private static final double kMaxAngleDeg = 170.0;
  private static final double kMaxCommandVolts = 6.0; // 初期は安全側

  // ===== Motion Profile制約（TODO: 実機に合わせて調整）=====
  private static final double kMaxVelRadPerSec = Units.degreesToRadians(240.0);
  private static final double kMaxAccRadPerSec2 = Units.degreesToRadians(720.0);

  // ===== PID初期値（WPILibチューニング手順で詰める）=====
  private static final double kP = 2.0;
  private static final double kI = 0.0;  // まず0固定
  private static final double kD = 0.15;

  // ===== FF初期値（摩擦が目立つ時に使う。最初は0でも可）=====
  private static final double kS = 0.0;
  private static final double kV = 0.0;
  private static final double kA = 0.0;  // まず0運用推奨

  // ===== Simモデル（TODO: 実機ギア比と慣性で更新）=====
  private static final double kSimGearRatio = 100.0;
  private static final double kSimJkgm2 = 0.0025;

  private final PWMSparkMax turretMotor = new PWMSparkMax(kTurretMotorPwmPort);

  // 目標角への遷移を台形プロファイルで制限するPID
  private final ProfiledPIDController controller =
      new ProfiledPIDController(
          kP, kI, kD,
          new TrapezoidProfile.Constraints(kMaxVelRadPerSec, kMaxAccRadPerSec2),
          kLoopPeriodSec);

  // 摩擦/速度補償（必要なければ0ゲインで実質無効）
  private final SimpleMotorFeedforward feedforward =
      new SimpleMotorFeedforward(kS, kV, kA);

  // Sim用モータモデル
  private final DCMotorSim turretSim =
      new DCMotorSim(
          LinearSystemId.createDCMotorSystem(DCMotor.getNEO(1), kSimJkgm2, kSimGearRatio),
          DCMotor.getNEO(1));

  private double goalRad = 0.0;
  private double simAngleRad = 0.0;

  public TurretSubsystem() {
    // 到達判定のしきい値（位置, 速度）
    controller.setTolerance(Units.degreesToRadians(2.0), Units.degreesToRadians(20.0));

    // 注意: スリップリングが無いタレットは continuous input を有効化しない
    // controller.enableContinuousInput(-Math.PI, Math.PI);

    // 初期化時に現在角度へ同期
    controller.reset(getAngleRad());
  }

  // Auto/Teleopから目標角をセット（deg入力で使いやすく）
  public void setGoalDeg(double targetDeg) {
    double clampedDeg = MathUtil.clamp(targetDeg, kMinAngleDeg, kMaxAngleDeg);
    goalRad = Units.degreesToRadians(clampedDeg);
  }

  public double getGoalDeg() {
    return Units.radiansToDegrees(goalRad);
  }

  public double getAngleDeg() {
    return Units.radiansToDegrees(getAngleRad());
  }

  public boolean atGoal() {
    return controller.atGoal();
  }

  // コマンド開始時に呼ぶと、初期の飛び出しを抑えやすい
  public void resetControllerToCurrentAngle() {
    controller.reset(getAngleRad());
  }

  public void stop() {
    turretMotor.stopMotor();
  }

  @Override
  public void periodic() {
    double measurementRad = getAngleRad();

    // PID出力（位置追従）
    double pidVolts = controller.calculate(measurementRad, goalRad);

    // FF出力（速度追従補助）
    double ffVolts = feedforward.calculate(controller.getSetpoint().velocity);

    // 合成して安全クランプ
    double commandVolts = MathUtil.clamp(
        pidVolts + ffVolts,
        -kMaxCommandVolts,
        kMaxCommandVolts);

    turretMotor.setVoltage(commandVolts);

    // 監視用（AdvantageScope/SmartDashboard）
    SmartDashboard.putNumber("Turret/AngleDeg", getAngleDeg());
    SmartDashboard.putNumber("Turret/GoalDeg", getGoalDeg());
    SmartDashboard.putNumber("Turret/SetpointDeg",
        Units.radiansToDegrees(controller.getSetpoint().position));
    SmartDashboard.putNumber("Turret/SetpointVelDegPerSec",
        Units.radiansToDegrees(controller.getSetpoint().velocity));
    SmartDashboard.putNumber("Turret/CommandVolts", commandVolts);
    SmartDashboard.putBoolean("Turret/AtGoal", atGoal());
  }

  @Override
  public void simulationPeriodic() {
    turretSim.setInputVoltage(turretMotor.get() * RobotController.getBatteryVoltage());
    turretSim.update(kLoopPeriodSec);
    simAngleRad = turretSim.getAngularPositionRad();
  }

  private double getAngleRad() {
    if (RobotBase.isSimulation()) {
      return simAngleRad;
    }
    // TODO: 実機エンコーダ値(rad)へ置き換え
    return 0.0;
  }
}
