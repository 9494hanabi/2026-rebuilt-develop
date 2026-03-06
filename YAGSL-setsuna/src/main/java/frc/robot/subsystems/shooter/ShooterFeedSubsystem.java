package frc.robot.subsystems.shooter;

import com.revrobotics.PersistMode;
import com.revrobotics.ResetMode;
import com.revrobotics.spark.SparkLowLevel.MotorType;
import com.revrobotics.spark.SparkMax;
import com.revrobotics.spark.config.SparkBaseConfig.IdleMode;
import com.revrobotics.spark.config.SparkMaxConfig;
import edu.wpi.first.math.MathUtil;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.SubsystemBase;

/* === 担当者 ===
 * はるた
 */

// ShooterFeedSubsystemの責務:
// 1) NEO 1台でボール送り機構を回す
// 2) Auto/調整用に現在の出力状態を可視化する
public class ShooterFeedSubsystem extends SubsystemBase {
  // 以下feeder定数のコード
  private static final int kFeedMotorCanId = 60; // 実機CAN IDに合わせて調整
  private static final boolean kFeedMotorInverted = false;
  private static final int kFeedCurrentLimitA = 35;
  private static final double kDefaultFeedPercent = 0.70;
  private static final double kMaxFeedPercent = 1.00;

  private final SparkMax feedMotor = new SparkMax(kFeedMotorCanId, MotorType.kBrushless);
  private double targetPercent = 0.0;

  public ShooterFeedSubsystem() {
    SparkMaxConfig config = new SparkMaxConfig();

    config
        .idleMode(IdleMode.kBrake)
        .inverted(kFeedMotorInverted)
        .smartCurrentLimit(kFeedCurrentLimitA);

    feedMotor.configure(config, ResetMode.kResetSafeParameters, PersistMode.kPersistParameters);
  }

  // 指定出力でフィード機構を回す
  public void setFeedPercent(double requestedPercent) {
    targetPercent = MathUtil.clamp(requestedPercent, -kMaxFeedPercent, kMaxFeedPercent);
    feedMotor.set(targetPercent);
  }

  // デフォルト出力でフィード機構を回す
  public void feedDefault() {
    setFeedPercent(kDefaultFeedPercent);
  }

  // フィード機構を停止する
  public void stop() {
    targetPercent = 0.0;
    feedMotor.stopMotor();
  }

  public double getTargetPercent() {
    return targetPercent;
  }

  public double getOutputCurrentAmp() {
    return feedMotor.getOutputCurrent();
  }

  @Override
  public void periodic() {
    SmartDashboard.putNumber("ShooterFeed/TargetPercent", targetPercent);
    SmartDashboard.putNumber("ShooterFeed/OutputCurrentA", getOutputCurrentAmp());
  }
}
