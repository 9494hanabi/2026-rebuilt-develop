package frc.robot.commands.debug.odmetry;

import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.wpilibj2.command.Command;
import frc.robot.RobotState;
import frc.robot.subsystems.SwerveSubsystem;

public class SetDriveHorizonCommand extends Command {

    private final SwerveSubsystem swerve;
    
    public SetDriveHorizonCommand(
        SwerveSubsystem swerve
    ) {
        this.swerve = swerve;
        addRequirements(swerve);
    }

    @Override
    public void execute() {
        swerve.driveFieldOriented(new ChassisSpeeds(0.0, 1.0, 0));
    }

    @Override
    public void end(boolean interrupted) {
        swerve.setChassisSpeeds(new ChassisSpeeds());
    }
}
