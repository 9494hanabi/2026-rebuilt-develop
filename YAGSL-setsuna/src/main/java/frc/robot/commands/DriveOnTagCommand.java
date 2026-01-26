package frc.robot.commands;

import frc.robot.subsystems.SwerveSubsystem;
import frc.robot.lib.util.Constants.VisionConstants;
import frc.robot.lib.util.Constants.SemiAutoConstants;

import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.wpilibj2.command.Command;

public class DriveOnTagCommand extends Command {
    private final SwerveSubsystem swerve;
    private final NetworkTable table =
        NetworkTableInstance.getDefault().getTable(VisionConstants.kFaceAprilTagTableName);

    public DriveOnTagCommand(SwerveSubsystem swerve) {
        this.swerve = swerve;
        addRequirements(swerve);
    }

    @Override
    public void execute() {
        boolean tv = table.getEntry("tv").getDouble(0) == 1.0;
        if (tv) {
            swerve.setChassisSpeeds(
                new ChassisSpeeds(SemiAutoConstants.kDriveOnTagSpeedMetersPerSec, 0.0, 0.0));
        } else {
            swerve.setChassisSpeeds(new ChassisSpeeds(0.0, 0.0, 0.0));
        }
    }

    @Override
    public void end(boolean interrupted) {
        swerve.setChassisSpeeds(new ChassisSpeeds(0.0, 0.0, 0.0));
    }
}
