package frc.robot.commands;

import frc.robot.RobotState;
import frc.robot.lib.time.RobotTime;
import frc.robot.lib.util.Constants.VisionConstants;
import frc.robot.subsystems.SwerveSubsystem;

import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.wpilibj2.command.Command;

import java.util.function.Supplier;

import org.littletonrobotics.junction.Logger;

// === 担当者 ===
// ひなた
//
public class DriveWhileFieldPoseValidCommand extends Command {
    private final SwerveSubsystem swerve;
    private final RobotState state;
    private final Supplier<ChassisSpeeds> speedsSupplier;

    public DriveWhileFieldPoseValidCommand(
        SwerveSubsystem swerve,
        RobotState state,
        Supplier<ChassisSpeeds> speedsSupplier
    ) {
        this.swerve = swerve;
        this.state = state;
        this.speedsSupplier = speedsSupplier;
        addRequirements(swerve);
    }

    private boolean hasFreshFieldPose() {
        double lastTs = state.lastUsedMegatagTimestamp();
        if (lastTs <= 0.0) {
            return false;
        }
        double now = RobotTime.getTimestampSeconds();
        return (now - lastTs) <= VisionConstants.kFieldPoseValidTimeoutSec;
    }

    @Override
    public void execute() {
        boolean hasPose = hasFreshFieldPose();
        Logger.recordOutput("DriveWhileFieldPose/HasFieldPose", hasPose);
        if (hasPose) {
            swerve.driveFieldOriented(speedsSupplier.get());
        } else {
            swerve.setChassisSpeeds(new ChassisSpeeds());
        }
    }

    @Override
    public void end(boolean interrupted) {
        Logger.recordOutput("DriveWhileFieldPose/HasFieldPose", false);
        swerve.setChassisSpeeds(new ChassisSpeeds());
    }
}
