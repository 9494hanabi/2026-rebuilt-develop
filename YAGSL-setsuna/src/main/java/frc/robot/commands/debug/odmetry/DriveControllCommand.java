package frc.robot.commands.debug.odmetry;

import static frc.robot.lib.constants.PIDConstants.*;
import static frc.robot.lib.constants.LogConstants.*;
import static frc.robot.lib.constants.SemiAutoConstants.omegaMaximum;
import static frc.robot.lib.constants.SemiAutoConstants.velocityMaximum;

import frc.robot.RobotState;
import frc.robot.lib.util.OdomHeadingController;
import frc.robot.lib.util.OdomTranslationController;
import frc.robot.subsystems.SwerveSubsystem;

import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.kinematics.ChassisSpeeds;

import java.util.function.Supplier;


public class DriveControllCommand extends Command{

    private final SwerveSubsystem swerve;
    private final RobotState state;

    private final Supplier<Pose2d> currentRobotPose;
    private final Supplier<Pose2d> targetRobotPose;

    private final PIDController translationPidX =
        new PIDController(kTranslationKp, kTranslationKi, kTranslationKd);
    private final PIDController translationPidY =
        new PIDController(kTranslationKp, kTranslationKi, kTranslationKd);
    private final PIDController headingPid =
        new PIDController(kHeadingKp, kHeadingKi, kHeadingKd);

    private final OdomHeadingController headingControl;
    private final OdomTranslationController translationControl;

    private double lastStatusLogSec = Double.NEGATIVE_INFINITY;
    private double lastWarningLogSec = Double.NEGATIVE_INFINITY;


    public DriveControllCommand(
            SwerveSubsystem swerve,
            RobotState state,
            Supplier<Pose2d> currentRobotPose,
            Supplier<Pose2d> targetRobotPose) {
        this(
                swerve,
                state,
                currentRobotPose,
                targetRobotPose,
                velocityMaximum,
                omegaMaximum);
    }

    public DriveControllCommand(
            SwerveSubsystem swerve,
            RobotState state,
            Supplier<Pose2d> currentRobotPose,
            Supplier<Pose2d> targetRobotPose,
            double maxTranslationMeterPerSec,
            double maxOmegaRadPerSec) {
        this.swerve = swerve;
        this.state = state;
        addRequirements(swerve);

        this.currentRobotPose = currentRobotPose;
        this.targetRobotPose = targetRobotPose;

        translationPidX.setTolerance(kTranslationToleranceMeters, kTranslationVelocityToleranceMPerSec);
        translationPidX.setIntegratorRange(-kTranslationIntegralContributionLimit, kTranslationIntegralContributionLimit);
        translationPidY.setTolerance(kTranslationToleranceMeters, kTranslationVelocityToleranceMPerSec);
        translationPidY.setIntegratorRange(-kTranslationIntegralContributionLimit, kTranslationIntegralContributionLimit);
        translationControl =
                new OdomTranslationController(
                        translationPidX,
                        translationPidY,
                        maxTranslationMeterPerSec);

        headingPid.disableContinuousInput();
        headingPid.setTolerance(kHeadingToleranceRad, kHeadingVelocityToleranceRadPerSec);
        headingPid.setIntegratorRange(-kHeadingIntegralContributionLimit, kHeadingIntegralContributionLimit);
        headingControl = new OdomHeadingController(headingPid, maxOmegaRadPerSec);
    }

    @Override
    public void initialize() {
        translationPidX.reset();
        translationPidY.reset();
        headingControl.reset();
        translationControl.reset();
    }

    @Override
    public void execute() {
        double nowSec = Timer.getFPGATimestamp();

        Pose2d currentRobotPose2d = currentRobotPose.get();
        Pose2d targetRobotPose2d = targetRobotPose.get();

        double currentXMeter = currentRobotPose2d.getX();
        double currentYMeter = currentRobotPose2d.getY();
        double currentHeadingRad = currentRobotPose2d.getRotation().getRadians();

        double targetXMeter = targetRobotPose2d.getX();
        double targetYMeter = targetRobotPose2d.getY();
        double targetHeadingRad = targetRobotPose2d.getRotation().getRadians();

        if (!Double.isFinite(currentXMeter)
                || !Double.isFinite(currentYMeter)
                || !Double.isFinite(currentHeadingRad)
                || !Double.isFinite(targetXMeter)
                || !Double.isFinite(targetYMeter)
                || !Double.isFinite(targetHeadingRad)) {
            if (nowSec - lastWarningLogSec >= kWarningLogPeriodSec) {
                lastWarningLogSec = nowSec;
                System.out.println("[DriveControl] !!INVALID!!");
                System.out.printf ("                current=(%s, %s, %s)%n", currentXMeter, currentYMeter, currentHeadingRad);
                System.out.printf ("                target =(%s, %s, %s)%n", targetXMeter, targetYMeter, targetHeadingRad);
                System.out.println("[DriveControl] ROBOT STOPPED");
            }
            translationPidX.reset();
            translationPidY.reset();
            headingControl.reset();
            swerve.driveFieldOriented(new ChassisSpeeds());
            return;
        }

        var maybeHeadingResult = headingControl.calculate(currentHeadingRad, targetHeadingRad);
        var maybeTranslationResult = translationControl.calculate(currentXMeter, currentYMeter, targetXMeter, targetYMeter);

        // 変化量が0の時停止
        if (maybeHeadingResult.isEmpty()
            || maybeTranslationResult.isEmpty()) {
            if (nowSec - lastWarningLogSec >= kWarningLogPeriodSec) {
                lastWarningLogSec = nowSec;
                System.out.println("[DriveControl] !!INVALID!!");
                System.out.printf ("                heading     result is %s%n", maybeHeadingResult.isEmpty() ? "empty" : "occupied");
                System.out.printf ("                translation result is %s%n", maybeTranslationResult.isEmpty() ? "empty" : "occupied");
                System.out.println("[DriveControl] ROBOT STOPPED");
            }
            headingControl.reset();
            translationControl.reset();
            swerve.driveFieldOriented(new ChassisSpeeds());
            return;
        }

        var headingResult     = maybeHeadingResult.get();
        var translationResult = maybeTranslationResult.get();

        double omega        = headingResult.omegaRadPerSec();
        double translationX = translationResult.translationXMeterPerSec();
        double translationY = translationResult.translationYMeterPerSec();


        if (!Double.isFinite(omega)
            || !Double.isFinite(translationX)
            || !Double.isFinite(translationY)) {
            if (nowSec - lastWarningLogSec >= kWarningLogPeriodSec) {
                lastWarningLogSec = nowSec;
                System.out.println("[DriveControl] !!INVALID!!");
                System.out.printf ("               omega        = %.2f rad%n", omega);
                System.out.printf ("               translationX = %.2f m/s%n", translationX);
                System.out.printf ("               translationY = %.2f m/s%n", translationY);
            }
            headingControl.reset();
            translationControl.reset();
            swerve.driveFieldOriented(new ChassisSpeeds());
            return;
        }

        boolean headingAtSetpoint = headingResult.atSetpoint();
        boolean xAtSetpoint = translationResult.xAtSetpoint();
        boolean yAtSetpoint = translationResult.yAtSetpoint();

        if (nowSec - lastStatusLogSec >= kStatusLogPeriodSec) {
            lastStatusLogSec = nowSec;
            System.out.printf("[DriveControl] cur=(%.2f, %.2f, %.1f deg) tgt=(%.2f, %.2f, %.1f deg) cmd=(%.2f, %.2f, %.3f) at=(%b, %b, %b)%n",
                    currentXMeter, currentYMeter, Math.toDegrees(currentHeadingRad),
                    targetXMeter, targetYMeter, Math.toDegrees(targetHeadingRad),
                    translationX, translationY, omega,
                    xAtSetpoint, yAtSetpoint, headingAtSetpoint);
        }

        if (headingAtSetpoint) {
            omega = 0.0;
        }
        if (xAtSetpoint) {
            translationX = 0.0;
        }
        if (yAtSetpoint) {
            translationY = 0.0;
        }

        swerve.driveFieldOriented(new ChassisSpeeds(translationX, translationY, omega));
    }

    @Override
    public void end(boolean interrupted) {
        headingControl.reset();
        translationControl.reset();
        swerve.driveFieldOriented(new ChassisSpeeds());
    }
}
