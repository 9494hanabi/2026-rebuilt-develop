package frc.robot.subsystems.mechanism;

import com.ctre.phoenix6.configs.TalonFXConfiguration;
import com.ctre.phoenix6.controls.MotionMagicVoltage;
import com.ctre.phoenix6.hardware.TalonFX;
import com.ctre.phoenix6.signals.NeutralModeValue;
import edu.wpi.first.math.MathUtil;
import edu.wpi.first.wpilibj2.command.SubsystemBase;

public class ExtenderSubsystem extends SubsystemBase {

  private static final int kExtendMotorCanId = 255;
  private static final double kIntakePositionEncoderOffset = 0.00;
  private static final double kDrivePositionEncoderOffset = 0.00;
  private static final double kExtenderToleranceDouble = 0.01;

  private static final double kMinTargetRot =
      Math.min(kIntakePositionEncoderOffset, kDrivePositionEncoderOffset);
  private static final double kMaxTargetRot =
      Math.max(kIntakePositionEncoderOffset, kDrivePositionEncoderOffset);

  private static final double kPositionKp = 4.0;
  private static final double kPositionKi = 0.0;
  private static final double kPositionKd = 0.1;

  private static final double kMotionMagicCruiseVelocityRps = 8.0;
  private static final double kMotionMagicAccelerationRpsSq = 16.0;
  private static final double kMotionMagicJerkRpsCubed = 80.0;

  private final TalonFX extendMotor = new TalonFX(kExtendMotorCanId);
  private final MotionMagicVoltage motionMagicRequest = new MotionMagicVoltage(0.0).withSlot(0);

  public ExtenderSubsystem() {
    TalonFXConfiguration cfg = new TalonFXConfiguration();

    cfg.MotorOutput.NeutralMode = NeutralModeValue.Brake;

    cfg.Slot0.kP = kPositionKp;
    cfg.Slot0.kI = kPositionKi;
    cfg.Slot0.kD = kPositionKd;

    cfg.MotionMagic.MotionMagicCruiseVelocity = kMotionMagicCruiseVelocityRps;
    cfg.MotionMagic.MotionMagicAcceleration = kMotionMagicAccelerationRpsSq;
    cfg.MotionMagic.MotionMagicJerk = kMotionMagicJerkRpsCubed;

    cfg.Feedback.SensorToMechanismRatio = 1.0;

    extendMotor.getConfigurator().apply(cfg);
    extendMotor.setPosition(0.0);
  }

  public void setIntakePosition() {
    setTargetPositionRot(kIntakePositionEncoderOffset);
  }

  public void setDrivePosition() {
    setTargetPositionRot(kDrivePositionEncoderOffset);
  }

  public void SetIntakePosition() {
    setIntakePosition();
  }

  public void SetDrivePosition() {
    setDrivePosition();
  }

  public void setTargetPositionRot(double requestedTargetRot) {
    double targetRot = MathUtil.clamp(requestedTargetRot, kMinTargetRot, kMaxTargetRot);
    extendMotor.setControl(motionMagicRequest.withPosition(targetRot));
  }

  public double getExtenderPosition() {
    return extendMotor.getPosition().getValueAsDouble();
  }

  public boolean extenderAtIntakePosition() {
    return  kIntakePositionEncoderOffset - kExtenderToleranceDouble < getExtenderPosition() &&
            kIntakePositionEncoderOffset + kExtenderToleranceDouble > getExtenderPosition() 
            ? true 
            : false;
  }

  public boolean extenderAtDrivePosition() {
    return  kDrivePositionEncoderOffset - kExtenderToleranceDouble < getExtenderPosition() &&
            kDrivePositionEncoderOffset + kExtenderToleranceDouble > getExtenderPosition() 
            ? true 
            : false;
  }
}           