package frc.robot.bindings;

import edu.wpi.first.wpilibj2.command.button.CommandXboxController;
import frc.robot.RobotState;
import frc.robot.subsystems.SwerveSubsystem;
import frc.robot.subsystems.vision.VisionSubsystem;

public class DebugBindings {
    public DebugBindings(
            SwerveSubsystem drivebase,
            VisionSubsystem vision,
            RobotState robotState,
            CommandXboxController controller) {}

    public void configure() {}
}
