package frc.robot.subsystems.mechanism;

import com.ctre.phoenix6.configs.TalonFXConfiguration;
import com.ctre.phoenix6.controls.VelocityVoltage;
import com.ctre.phoenix6.hardware.TalonFX;
import com.ctre.phoenix6.signals.NeutralModeValue;
import edu.wpi.first.math.MathUtil;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.SubsystemBase;

/* === 担当者 ===
 * はるた
 */

// ShooterSubsystemの責務:
// 1) TalonFXを速度閉ループで回す
// 2) 目標回転数に入ったか(Ready)を判定する
// 3) Auto/チューニング用のログを出す
public class ShooterSubsystem extends SubsystemBase {
  private static final int kShooterMotor_1CanId = 15;
  private static final int kShooterMotor_2CanId = 16;

  // 安全のための上限（実機で要調整）
  private static final double kMaxTargetRps = 120.0;

  // Auto/Teleop共通で使う基準回転数（実機で調整）
  public static final double kNominalShotRps = 95;

  // あなた指定の基準
  private static final double kReadyToleranceRps = 2.0; // ±2 RPS
  private static final double kReadyHoldSec = 0.150;    // 150ms

  // 初期ゲイン（必ず実機で調整）
  private static final double kS = 0.20;
  private static final double kV = 0.12;
  private static final double kP = 0.25;
  private static final double kI = 0.00;
  private static final double kD = 0.00;

  private final TalonFX shooterMotor_1 = new TalonFX(kShooterMotor_1CanId);
  private final TalonFX shooterMotor_2 = new TalonFX(kShooterMotor_2CanId);
  private final VelocityVoltage velocityRequest = new VelocityVoltage(0.0);

  private double targetRps = 0.0;
  private double readyWindowStartSec = Double.NaN;
  private boolean atSpeed = false;

  public ShooterSubsystem() {
    TalonFXConfiguration config = new TalonFXConfiguration();
    config.MotorOutput.NeutralMode = NeutralModeValue.Coast;

    // Slot0: VelocityVoltageで使う閉ループゲイン
    config.Slot0.kS = kS;
    config.Slot0.kV = kV;
    config.Slot0.kP = kP;
    config.Slot0.kI = kI;
    config.Slot0.kD = kD;

    config.CurrentLimits.SupplyCurrentLimitEnable = true;
    config.CurrentLimits.SupplyCurrentLimit = 40.0;

    shooterMotor_1.getConfigurator().apply(config);
    shooterMotor_2.getConfigurator().apply(config);
  }

  // 目標RPSを設定して速度閉ループ開始
  public void setTargetRps(double requestedRps) {
    targetRps = MathUtil.clamp(requestedRps, 0.0, kMaxTargetRps);
    readyWindowStartSec = Double.NaN;
    atSpeed = false;
    shooterMotor_1.setControl(velocityRequest.withVelocity(-targetRps));
    shooterMotor_2.setControl(velocityRequest.withVelocity(targetRps));
  }

  public void stop() {
    targetRps = 0.0;
    readyWindowStartSec = Double.NaN;
    atSpeed = false;
    shooterMotor_1.stopMotor();
    shooterMotor_2.stopMotor();
  }

  public double getTargetRps() {
    return targetRps;
  }

  public double getVelocityRps() {
    double v1 = shooterMotor_1.getVelocity().getValueAsDouble();
    double v2 = shooterMotor_2.getVelocity().getValueAsDouble();
    return (Math.abs(v1) + Math.abs(v2)) / 2.0;
  }

  public double getVelocityErrorRps() {
    return targetRps - getVelocityRps();
  }

  public boolean atSpeed() {
    return atSpeed;
  }

  // Ready判定: 誤差内に入ってから150ms連続維持できたらtrue
  private void updateReadyState() {
    if (targetRps <= 0.0) {
      readyWindowStartSec = Double.NaN;
      atSpeed = false;
      return;
    }

    double nowSec = Timer.getFPGATimestamp();
    double absError = Math.abs(getVelocityErrorRps());

    if (absError <= kReadyToleranceRps) {
      if (!Double.isFinite(readyWindowStartSec)) {
        readyWindowStartSec = nowSec;
      }
      atSpeed = (nowSec - readyWindowStartSec) >= kReadyHoldSec;
    } else {
      readyWindowStartSec = Double.NaN;
      atSpeed = false;
    }
  }

  @Override
  public void periodic() {
    updateReadyState();

    SmartDashboard.putNumber("Shooter/TargetRps", getTargetRps());
    SmartDashboard.putNumber("Shooter/VelocityRps", getVelocityRps());
    SmartDashboard.putNumber("Shooter/ErrorRps", getVelocityErrorRps());
    SmartDashboard.putNumber("Shooter/ReadyToleranceRps", kReadyToleranceRps);
    SmartDashboard.putNumber("Shooter/ReadyHoldSec", kReadyHoldSec);
    SmartDashboard.putBoolean("Shooter/AtSpeed", atSpeed());
  }
}
