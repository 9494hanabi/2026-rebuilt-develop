package frc.robot.subsystems.mechanism;

import edu.wpi.first.wpilibj2.command.SubsystemBase;
import edu.wpi.first.wpilibj.motorcontrol.PWMSparkMax;

public class IntakerSubsystem extends SubsystemBase {
  private static final int kIntakerMotor_CanId = 255;
  private static final boolean kIntakerMotorInverted = false;

  private static PWMSparkMax kIntakerMotor = new PWMSparkMax(kIntakerMotor_CanId);

  private static final double kTargetSpeed = 1;

  public IntakerSubsystem() {
    kIntakerMotor.setInverted(kIntakerMotorInverted);
  }

  public void intaking() {
    kIntakerMotor.set(kTargetSpeed);
  }

  public void stop() {
    kIntakerMotor.stopMotor();
  }
}
