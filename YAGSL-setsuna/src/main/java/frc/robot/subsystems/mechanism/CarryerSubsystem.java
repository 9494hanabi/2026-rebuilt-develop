package frc.robot.subsystems.mechanism;

import edu.wpi.first.wpilibj2.command.SubsystemBase;
import edu.wpi.first.wpilibj.motorcontrol.PWMSparkMax;

public class CarryerSubsystem extends SubsystemBase {
    private static final int kCarryerMotor_1CanId = 255;
    private static final boolean kCarryerMotorInverted = false;
    
    private final PWMSparkMax kCarryerMotor = new PWMSparkMax(kCarryerMotor_1CanId);

    private static final double kTargetSpeed = 1;

    public CarryerSubsystem() {
        kCarryerMotor.setInverted(kCarryerMotorInverted);
    }

    public void carrying() {
        kCarryerMotor.set(kTargetSpeed);
    }

    public void stop() {
        kCarryerMotor.stopMotor();
    }
}
