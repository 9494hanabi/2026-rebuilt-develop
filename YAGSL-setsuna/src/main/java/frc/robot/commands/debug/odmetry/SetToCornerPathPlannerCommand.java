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

    private Pose2d targetPose;

    public SetToCornerPathPlannerCommand(
            SwerveSubsystem swerve,
            RobotState state,
            Corner corner) {
        addCommands(
                Commands.runOnce(() -> {
                    Rotation2d keepHeading = swerve.getSwerveDrive().getPose().getRotation();
                    targetPose = resolveCornerPose(corner, keepHeading);
                    state.setTrajectoryTargetPose(targetPose);
                    System.out.printf(
                            "[SetToCornerPathPlanner] START corner=%s target=%s keepHeading=%.1fdeg%n",
                            corner,
                            formatPose(targetPose),
                            keepHeading.getDegrees());
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
                                    0.0);
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
