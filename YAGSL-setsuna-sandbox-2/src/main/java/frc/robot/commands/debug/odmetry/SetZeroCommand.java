package frc.robot.commands.debug.odmetry;

import static frc.robot.lib.util.Constants.SemiAutoConstants.omegaMaximum;
import static frc.robot.lib.util.Constants.SemiAutoConstants.velocityMaximum;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj2.command.Command;

import frc.robot.RobotState;
import frc.robot.subsystems.SwerveSubsystem;
import frc.robot.lib.util.Constants.FieldConstants;
import frc.robot.lib.util.Constants.PIDConstants;
import frc.robot.lib.util.OdomHeadingController;

public class SetZeroCommand extends Command {
    private static final double kTranslationToleranceMeters = 0.05;
    private static final double kTranslationVelocityToleranceMPerSec = 0.1;
    private static final double kIntegralContributionLimit = 0.3;

    private static final double kHeadingToleranceRad = Math.toRadians(1.0);
    private static final double kHeadingVelocityToleranceRadPerSec = Math.toRadians(8.0);
    private static final double kStatusLogPeriodSec = 0.20;
    private static final double kFusedOdomTranslationMismatchMeter = 0.75;
    private static final double kFieldBoundaryMarginMeter = 0.25;
    private static final double kTargetXMeter = FieldConstants.kInitialFieldToRobotPose.getX();
    private static final double kTargetYMeter = FieldConstants.kInitialFieldToRobotPose.getY();
    private static final double kTargetHeadingRad = FieldConstants.kInitialFieldToRobotPose.getRotation().getRadians();

    private final SwerveSubsystem swerve;
    private final RobotState state;

    private final PIDController translationPidX =
        new PIDController(PIDConstants.kTranslationKp, PIDConstants.kTranslationKi, PIDConstants.kTranslationKd);
    private final PIDController translationPidY =
        new PIDController(PIDConstants.kTranslationKp, PIDConstants.kTranslationKi, PIDConstants.kTranslationKd);
    private final PIDController headingPid =
        new PIDController(PIDConstants.kHeadingKp, PIDConstants.kHeadingKi, PIDConstants.kHeadingKd);
    private final OdomHeadingController headingControl;
    private double lastStatusLogSec = Double.NEGATIVE_INFINITY;

    public SetZeroCommand(
        SwerveSubsystem swerve,
        RobotState state
    ) {
        this.swerve = swerve;
        this.state = state;
        addRequirements(swerve);

        translationPidX.setTolerance(kTranslationToleranceMeters, kTranslationVelocityToleranceMPerSec);
        translationPidX.setIntegratorRange(-kIntegralContributionLimit, kIntegralContributionLimit);
        translationPidY.setTolerance(kTranslationToleranceMeters, kTranslationVelocityToleranceMPerSec);
        translationPidY.setIntegratorRange(-kIntegralContributionLimit, kIntegralContributionLimit);
        headingPid.disableContinuousInput();
        headingPid.setTolerance(kHeadingToleranceRad, kHeadingVelocityToleranceRadPerSec);
        headingPid.setIntegratorRange(-kIntegralContributionLimit, kIntegralContributionLimit);
        headingControl = new OdomHeadingController(headingPid, omegaMaximum);
    }

    @Override
    public void initialize() {
        translationPidX.reset();
        translationPidY.reset();
        headingControl.reset();
        lastStatusLogSec = Double.NEGATIVE_INFINITY;
        System.out.printf(
                "[SetZero] initialized target=(%.2f, %.2f, %.2f deg)%n",
                kTargetXMeter,
                kTargetYMeter,
                Math.toDegrees(kTargetHeadingRad));
    }

    @Override
    public void execute() {
        double nowSec = Timer.getFPGATimestamp();
        var latest = state.getLatestFieldToRobot();
        var latestOdom = state.getLatestFieldToRobotOdom();
        if (latest == null || latestOdom == null) {
            System.out.println("[SetZero] pose is null, skipping");
            translationPidX.reset();
            translationPidY.reset();
            headingControl.reset();
            swerve.driveFieldOriented(new ChassisSpeeds());
            return;
        }

        double currentXMeter = latest.getValue().getX();
        double currentYMeter = latest.getValue().getY();
        double fusedXMeter = currentXMeter;
        double fusedYMeter = currentYMeter;
        double odomXMeter = latestOdom.getValue().getX();
        double odomYMeter = latestOdom.getValue().getY();
        double currentHeadingRad = latestOdom.getValue().getRotation().getRadians();
        double fusedOdomDeltaMeter = Math.hypot(fusedXMeter - odomXMeter, fusedYMeter - odomYMeter);
        boolean fusedInField = isInsideFieldWithMargin(fusedXMeter, fusedYMeter);
        boolean translationFromFused =
                fusedInField
                        && Double.isFinite(fusedOdomDeltaMeter)
                        && fusedOdomDeltaMeter <= kFusedOdomTranslationMismatchMeter;
        if (!translationFromFused) {
            currentXMeter = odomXMeter;
            currentYMeter = odomYMeter;
        }
        if (!Double.isFinite(currentXMeter)
                || !Double.isFinite(currentYMeter)
                || !Double.isFinite(currentHeadingRad)) {
            translationPidX.reset();
            translationPidY.reset();
            headingControl.reset();
            swerve.driveFieldOriented(new ChassisSpeeds());
            return;
        }

        double translationX = MathUtil.clamp(
                        translationPidX.calculate(currentXMeter, kTargetXMeter),
                        -velocityMaximum, velocityMaximum);

        double translationY = MathUtil.clamp(
                        translationPidY.calculate(currentYMeter, kTargetYMeter),
                        -velocityMaximum, velocityMaximum);
                        
        var maybeHeadingResult = headingControl.calculate(currentHeadingRad, kTargetHeadingRad);
        if (maybeHeadingResult.isEmpty()) {
            translationPidX.reset();
            translationPidY.reset();
            headingControl.reset();
            swerve.driveFieldOriented(new ChassisSpeeds());
            return;
        }
        var headingResult = maybeHeadingResult.get();
        double omega = headingResult.omegaRadPerSec();

        boolean atTranslation = translationPidX.atSetpoint() && translationPidY.atSetpoint();
        boolean atHeading = headingResult.atSetpoint();
        if (atTranslation) {
            translationX = 0.0;
            translationY = 0.0;
        }

        if (nowSec - lastStatusLogSec >= kStatusLogPeriodSec) {
            lastStatusLogSec = nowSec;
            System.out.printf(
                    "[SetZero] pos=(%.2f, %.2f, %.2f deg) src=%s fO=%.2f fused=(%.2f, %.2f) odom=(%.2f, %.2f) "
                            + "vel=(%.2f, %.2f, %.2f) atTranslation=%b atHeading=%b%n",
                    currentXMeter,
                    currentYMeter,
                    Math.toDegrees(headingResult.wrappedCurrentHeadingRad()),
                    translationFromFused ? "fused" : "odom",
                    fusedOdomDeltaMeter,
                    fusedXMeter,
                    fusedYMeter,
                    odomXMeter,
                    odomYMeter,
                    translationX,
                    translationY,
                    omega,
                    atTranslation,
                    atHeading);
        }

        swerve.driveFieldOriented(new ChassisSpeeds(translationX, translationY, omega));
    }

    private static boolean isInsideFieldWithMargin(double xMeter, double yMeter) {
        return xMeter >= -kFieldBoundaryMarginMeter
                && xMeter <= FieldConstants.fieldLengthMeter + kFieldBoundaryMarginMeter
                && yMeter >= -kFieldBoundaryMarginMeter
                && yMeter <= FieldConstants.fieldWidthMeter + kFieldBoundaryMarginMeter;
    }

    @Override
    public void end(boolean interrupted) {
        translationPidX.reset();
        translationPidY.reset();
        headingControl.reset();
        swerve.driveFieldOriented(new ChassisSpeeds());
        System.out.println("[SetZero] ended, interrupted=" + interrupted);
    }
}
