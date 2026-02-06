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
    private final NetworkTable tableA;
    private final NetworkTable tableB;

    public DriveOnTagCommand(SwerveSubsystem swerve) {
        this(swerve, VisionConstants.kLimelightATableName, VisionConstants.kLimelightBTableName);
    }

    public DriveOnTagCommand(SwerveSubsystem swerve, String limelightTableName) {
        this(swerve, limelightTableName, null);
    }

    public DriveOnTagCommand(SwerveSubsystem swerve, String limelightTableNameA, String limelightTableNameB) {
        this.swerve = swerve;
        this.tableA = NetworkTableInstance.getDefault().getTable(limelightTableNameA);
        this.tableB = limelightTableNameB == null
                ? null
                : NetworkTableInstance.getDefault().getTable(limelightTableNameB);
        addRequirements(swerve);
    }

    @Override
    public void execute() {
        boolean tvA = tableA.getEntry("tv").getDouble(0.0) > 0;
        boolean tvB = tableB != null && tableB.getEntry("tv").getDouble(0.0) > 0;
        boolean tv = tvA || tvB;
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
