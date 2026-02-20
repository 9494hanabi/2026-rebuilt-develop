package frc.robot.commands;

import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import frc.robot.RobotState;
import frc.robot.subsystems.SwerveSubsystem;
import frc.robot.subsystems.TurretSubsystem;

public final class AutoCommand {
    private AutoCommand() {
        throw new UnsupportedOperationException("This is a utility class!");
    }

    public static Command doNothing() {
        return Commands.none();
    }

    public static Command stopDrive(SwerveSubsystem drivebase) {
        return Commands.runOnce(
                () -> drivebase.setChassisSpeeds(new ChassisSpeeds(0.0, 0.0, 0.0)),
                drivebase);
    }

    public static Command pathPlannerAuto(SwerveSubsystem drivebase, String autoName) {
        return Commands.sequence(
                drivebase.getAutonomousCommand(autoName),
                stopDrive(drivebase));
    }

    public static Command runChassisFor(
            SwerveSubsystem drivebase,
            ChassisSpeeds speeds,
            double seconds) {
        return Commands.run(() -> drivebase.setChassisSpeeds(speeds), drivebase)
                .withTimeout(seconds)
                .andThen(stopDrive(drivebase));
    }

    public static Command driveForwardFor(
            SwerveSubsystem drivebase,
            double metersPerSec,
            double seconds) {
        return runChassisFor(drivebase, new ChassisSpeeds(metersPerSec, 0.0, 0.0), seconds);
    }

    public static Command driveOnTagFor(
            SwerveSubsystem drivebase,
            RobotState state,
            double seconds) {
        return Commands.waitSeconds(seconds).andThen(stopDrive(drivebase));
    }

    public static Command faceTagFor(
            SwerveSubsystem drivebase,
            RobotState state,
            int tagID,
            double seconds) {
        return Commands.waitSeconds(seconds).andThen(stopDrive(drivebase));
    }

    public static Command alignAndApproachTag(
            SwerveSubsystem drivebase,
            RobotState state,
            int tagID,
            double alignSec,
            double approachSec) {
        return Commands.sequence(
                faceTagFor(drivebase, state, tagID, alignSec),
                driveOnTagFor(drivebase, state, approachSec),
                stopDrive(drivebase));
    }

    public static Command turretToAngle(
            TurretSubsystem turret,
            double angleDeg,
            double timeoutSec) {
        return Commands.runOnce(turret::stop, turret);
    }

    public static Command turretStow(TurretSubsystem turret) {
        return Commands.runOnce(turret::stop, turret);
    }

    public static Command logMarker(String markerName) {
        return Commands.none();
    }

    public static Command safeStopAll(SwerveSubsystem drivebase, TurretSubsystem turret) {
        return Commands.sequence(
                stopDrive(drivebase),
                Commands.runOnce(turret::stop, turret));
    }

    public static Command scoreCycleBasicSafe(
            SwerveSubsystem drivebase,
            RobotState state,
            TurretSubsystem turret) {
        return scoreCycleBasic(drivebase, state, turret)
                .withTimeout(5.0)
                .andThen(safeStopAll(drivebase, turret));
    }

    public static Command scoreCycleBasic(
            SwerveSubsystem drivebase,
            RobotState state,
            TurretSubsystem turret) {
        return Commands.sequence(
                Commands.none(),
                safeStopAll(drivebase, turret));
    }
}
