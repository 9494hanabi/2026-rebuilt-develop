package frc.robot.subsystems.shooter;

import com.ctre.phoenix6.configs.TalonFXConfiguration;
import com.ctre.phoenix6.controls.DutyCycleOut;
import com.ctre.phoenix6.controls.PositionVoltage;
import com.ctre.phoenix6.hardware.TalonFX;
import com.ctre.phoenix6.signals.NeutralModeValue;
import edu.wpi.first.math.MathUtil;
import edu.wpi.first.units.measure.Velocity;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.SubsystemBase;

/* === 担当者 ===
 * はるた
 */
public class ShootAngleSubsystems extends SubsystemBase {
  private static final int kAngleMotorCanId = 17;

  // キャリブレーション値（Rot）
  private static final double kMotorRotAtMinAngle = -0.25;
  private static final double kMotorRotAtMaxAngle = 0.91;

  private static final double kMinTargetRot = Math.min(kMotorRotAtMinAngle, kMotorRotAtMaxAngle);
  private static final double kMaxTargetRot = Math.max(kMotorRotAtMinAngle, kMotorRotAtMaxAngle);
  private static final double kAtTargetToleranceRot = 0.01;

  private static final double kManualMaxPercent = 0.5;

  // 位置制御ゲイン（Rotベース）
  /* P→どれだけズレているか
   *    上げる：速く目的まで行く、強く押してくれる、力が増える
   *    上げすぎ：行きすぎる、震える
   * I→ずっと残ってる誤差を押し切る
   *    上げる：誤差を潰す
   *    上げすぎ：行きすぎる、暴れる、止まらない→危ない
   * D→どんくらいの勢いでズレが変わっているか
   *    上げる：収まりやすい、オーバーシュート
   *    上げすぎ：動きが鈍い、ビリビリする、ノイズに反応してガタつく
   */
  private static final double kPositionKp = 1.2;
  private static final double kPositionKi = 0.0;
  private static final double kPositionKd = 0.2;

  private final TalonFX angleMotor = new TalonFX(kAngleMotorCanId);
  private final PositionVoltage positionRequest = new PositionVoltage(0.0).withSlot(0);
  private final DutyCycleOut dutyRequest = new DutyCycleOut(0.0);

  private double targetMotorRot = kMotorRotAtMinAngle;

  public ShootAngleSubsystems() {
    TalonFXConfiguration cfg = new TalonFXConfiguration();
    cfg.MotorOutput.NeutralMode = NeutralModeValue.Brake;

    cfg.Slot0.kP = kPositionKp;
    cfg.Slot0.kI = kPositionKi;
    cfg.Slot0.kD = kPositionKd;

    cfg.CurrentLimits.SupplyCurrentLimitEnable = true;
    cfg.CurrentLimits.SupplyCurrentLimit = 50;

    cfg.SoftwareLimitSwitch.ForwardSoftLimitEnable = true;
    cfg.SoftwareLimitSwitch.ReverseSoftLimitEnable = true;
    cfg.SoftwareLimitSwitch.ForwardSoftLimitThreshold = kMaxTargetRot;
    cfg.SoftwareLimitSwitch.ReverseSoftLimitThreshold = kMinTargetRot;

    angleMotor.getConfigurator().apply(cfg);

    angleMotor.setPosition(kMotorRotAtMinAngle);
    setTargetMotorRot(kMotorRotAtMinAngle);
  }

  public void setTargetMotorRot(double requestedMotorRot) {
    targetMotorRot = MathUtil.clamp(requestedMotorRot, kMinTargetRot, kMaxTargetRot);
    angleMotor.setControl(positionRequest.withPosition(targetMotorRot));
  }

  public void manualPercent(double percent) {
    double cmd = MathUtil.clamp(percent, -kManualMaxPercent, kManualMaxPercent);
    double currentRot = getMotorPosRot();

    if ((currentRot >= kMaxTargetRot && cmd > 0.0) || (currentRot <= kMinTargetRot && cmd < 0.0)) {
      cmd = 0.0;
    }

    angleMotor.setControl(dutyRequest.withOutput(cmd));
  }

  public void stop() {
    angleMotor.stopMotor();
  }

  public double getMotorPosRot() {
    return angleMotor.getPosition().getValueAsDouble();
  }

  public double getTargetMotorRot() {
    return targetMotorRot;
  }

  public boolean atTarget() {
    return Math.abs(targetMotorRot - getMotorPosRot()) <= kAtTargetToleranceRot;
  }

  // 起動後に現在位置を任意Rotとして再ゼロ化
  public void zeroAtCurrentRotAs(double currentRot) {
    double clamped = MathUtil.clamp(currentRot, kMinTargetRot, kMaxTargetRot);
    angleMotor.setPosition(clamped);
    targetMotorRot = clamped;
  }

  @Override
  public void periodic() {
    SmartDashboard.putNumber("ShootAngle/MotorPosRot", getMotorPosRot());
    SmartDashboard.putNumber("ShootAngle/TargetRot", targetMotorRot);
    SmartDashboard.putNumber("ShootAngle/PosErrorRot", targetMotorRot - getMotorPosRot());
    SmartDashboard.putBoolean("ShootAngle/AtTarget", atTarget());

    SmartDashboard.putNumber("ShootAngle/ClosedLoopError", angleMotor.getClosedLoopError().getValueAsDouble());
    SmartDashboard.putNumber("ShootAngle/CalibMinRot", kMotorRotAtMinAngle);
    SmartDashboard.putNumber("ShootAngle/CalibMaxRot", kMotorRotAtMaxAngle);
  }
}
