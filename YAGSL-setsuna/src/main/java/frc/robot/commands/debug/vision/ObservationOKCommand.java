package frc.robot.commands.debug.vision;

import frc.robot.subsystems.SwerveSubsystem;
import frc.robot.lib.constants.VisionConstants;
import frc.robot.lib.limelight.VisionTargetSelector;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.math.kinematics.ChassisSpeeds;

// === 担当者 ===
// ひなた
//

public class ObservationOKCommand extends Command {
    private static final double kBackwardSpeedMetersPerSec = -1.0;
    private static final double kTargetSeenHoldSeconds = 0.20;

    private final SwerveSubsystem swerve;
    private final VisionTargetSelector targetSelector;
    private double lastSeenTimestampSec = Double.NEGATIVE_INFINITY;

    public ObservationOKCommand(
            SwerveSubsystem swerve) {
        this.swerve = swerve;
        this.targetSelector =
            new VisionTargetSelector(
                VisionConstants.kLimelightATableName,
                VisionConstants.kLimelightBTableName);
        addRequirements(swerve);
    }

    @Override
    public void initialize() {
        lastSeenTimestampSec = Double.NEGATIVE_INFINITY;
    }

    @Override
    public void execute() {
        double nowSec = Timer.getFPGATimestamp();
        if (targetSelector.selectBestObservation().isPresent()) {
            lastSeenTimestampSec = nowSec;
        }

        boolean shouldDriveBackward =
            (nowSec - lastSeenTimestampSec) <= kTargetSeenHoldSeconds;
        if (shouldDriveBackward) {
            swerve.setChassisSpeeds(
                new ChassisSpeeds(
                    kBackwardSpeedMetersPerSec,
                    0.0,
                    0.0)); //後進
        } else {
            swerve.setChassisSpeeds(new ChassisSpeeds());  // 停止
        }
    }

    @Override
    public void end(boolean interrupted) {
        swerve.setChassisSpeeds(new ChassisSpeeds());  // 停止
    }
}
