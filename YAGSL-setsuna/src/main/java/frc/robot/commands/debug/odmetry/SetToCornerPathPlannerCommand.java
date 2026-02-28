package frc.robot.commands.debug.odmetry;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.SequentialCommandGroup;
import frc.robot.RobotState;
import frc.robot.lib.constants.FieldConstants;
import frc.robot.subsystems.SwerveSubsystem;
import com.pathplanner.lib.path.PathConstraints;

import java.util.Set;

public class SetToCornerPathPlannerCommand extends SequentialCommandGroup {

    public enum Corner {
        LEFT_UP,
        RIGHT_UP,
        RIGHT_DOWN,
        LEFT_DOWN
    }

    private static final double kInnerOffsetMeter = 0.5;
    private static final PathConstraints kSafeCornerPathfindingConstraints =
            new PathConstraints(
                    1.0,
                    1.0,
                    Math.toRadians(90.0),
                    Math.toRadians(180.0));
    private static final double kMinimumCornerPathTimeoutSec = 3.0;
    private static final double kCornerPathTimeoutBufferSec = 1.5;

    private Pose2d targetPose;
    private double targetTimeoutSec = kMinimumCornerPathTimeoutSec;

    public SetToCornerPathPlannerCommand(
            SwerveSubsystem swerve,
            RobotState state,
            Corner corner) {
        addCommands(
                Commands.runOnce(() -> {
                    Pose2d currentPose = swerve.getSwerveDrive().getPose();
                    Rotation2d keepHeading = currentPose.getRotation();
                    targetPose = resolveCornerPose(corner, keepHeading);
                    targetTimeoutSec =
                            estimatePathTimeoutSec(
                                    currentPose,
                                    targetPose,
                                    kSafeCornerPathfindingConstraints);
                    state.setTrajectoryTargetPose(targetPose);
                    System.out.printf(
                            "[SetToCornerPathPlanner] START corner=%s target=%s keepHeading=%.1fdeg timeout=%.2fs%n",
                            corner,
                            formatPose(targetPose),
                            keepHeading.getDegrees(),
                            targetTimeoutSec);
                }),
                Commands.defer(
                        () -> {
                            if (!isFinitePose(targetPose)) {
                                DriverStation.reportWarning(
                                        "[SetToCornerPathPlanner] targetPose is invalid, skipping pathfind.",
                                        false);
                                return Commands.none();
                            }
                            return swerve.pathfindToPose(
                                    targetPose,
                                    kSafeCornerPathfindingConstraints,
                                    0.0).withTimeout(targetTimeoutSec);
                        },
                        Set.of(swerve)),
                Commands.runOnce(() ->
                        System.out.printf(
                                "[SetToCornerPathPlanner] END corner=%s target=%s%n",
                                corner,
                                formatPose(targetPose))),
                Commands.runOnce(() -> swerve.driveFieldOriented(new ChassisSpeeds()), swerve));
    }

    private static Pose2d resolveCornerPose(Corner corner, Rotation2d headingToKeep) {
        double nearMinX = 0.0 + kInnerOffsetMeter;
        double nearMaxX = FieldConstants.fieldLengthMeter - kInnerOffsetMeter;
        double nearMinY = 0.0 + kInnerOffsetMeter;
        double nearMaxY = FieldConstants.fieldWidthMeter - kInnerOffsetMeter;

        return switch (corner) {
            case LEFT_UP -> new Pose2d(nearMinX, nearMaxY, headingToKeep);
            case RIGHT_UP -> new Pose2d(nearMaxX, nearMaxY, headingToKeep);
            case RIGHT_DOWN -> new Pose2d(nearMaxX, nearMinY, headingToKeep);
            case LEFT_DOWN -> new Pose2d(nearMinX, nearMinY, headingToKeep);
        };
    }

    private static double estimatePathTimeoutSec(
            Pose2d startPose,
            Pose2d goalPose,
            PathConstraints constraints) {
        if (!isFinitePose(startPose) || !isFinitePose(goalPose) || constraints == null) {
            return kMinimumCornerPathTimeoutSec;
        }

        double distanceMeter = startPose.getTranslation().getDistance(goalPose.getTranslation());
        double maxVelocityMps = constraints.maxVelocityMPS();
        double maxAccelerationMpsSq = constraints.maxAccelerationMPSSq();

        if (!Double.isFinite(distanceMeter)
                || !Double.isFinite(maxVelocityMps)
                || !Double.isFinite(maxAccelerationMpsSq)
                || maxVelocityMps <= 0.0
                || maxAccelerationMpsSq <= 0.0) {
            return kMinimumCornerPathTimeoutSec;
        }

        double accelTimeSec = maxVelocityMps / maxAccelerationMpsSq;
        double accelDistanceMeter =
                0.5 * maxAccelerationMpsSq * accelTimeSec * accelTimeSec;

        double motionTimeSec;
        if (distanceMeter <= 2.0 * accelDistanceMeter) {
            motionTimeSec = 2.0 * Math.sqrt(distanceMeter / maxAccelerationMpsSq);
        } else {
            motionTimeSec =
                    (2.0 * accelTimeSec)
                            + ((distanceMeter - (2.0 * accelDistanceMeter)) / maxVelocityMps);
        }

        if (!Double.isFinite(motionTimeSec)) {
            return kMinimumCornerPathTimeoutSec;
        }

        return Math.max(
                kMinimumCornerPathTimeoutSec,
                motionTimeSec + kCornerPathTimeoutBufferSec);
    }

    private static boolean isFinitePose(Pose2d pose) {
        return pose != null
                && Double.isFinite(pose.getX())
                && Double.isFinite(pose.getY())
                && Double.isFinite(pose.getRotation().getRadians());
    }

    private static String formatPose(Pose2d pose) {
        if (pose == null) {
            return "null";
        }
        return String.format(
                "(%.2f, %.2f, %.1f deg)",
                pose.getX(),
                pose.getY(),
                pose.getRotation().getDegrees());
    }
}
