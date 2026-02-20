package frc.robot.commands.debug.odmetry;

import static frc.robot.lib.util.Constants.SemiAutoConstants.omegaMaximum;
import static frc.robot.lib.util.Constants.SemiAutoConstants.velocityMaximum;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.wpilibj2.command.Command;

import frc.robot.RobotState;
import frc.robot.subsystems.SwerveSubsystem;
import frc.robot.lib.util.Constants.PIDConstants;

public class SetZeroCommand extends Command {
    private static final double kTranslationToleranceMeters = 0.05;
    private static final double kTranslationVelocityToleranceMPerSec = 0.1;
    private static final double kIntegralContributionLimit = 0.3;

    private static final double kHeadingToleranceRad = Math.toRadians(1.0);
    private static final double kHeadingVelocityToleranceRadPerSec = Math.toRadians(8.0);

    private final SwerveSubsystem swerve;
    private final RobotState state;

    private final PIDController translationPidX =
        new PIDController(PIDConstants.kTranslationKp, PIDConstants.kTranslationKi, PIDConstants.kTranslationKd);
    private final PIDController translationPidY =
        new PIDController(PIDConstants.kTranslationKp, PIDConstants.kTranslationKi, PIDConstants.kTranslationKd);
    private final PIDController headingPid =
        new PIDController(PIDConstants.kHeadingKp, PIDConstants.kHeadingKi, PIDConstants.kHeadingKd);

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
        headingPid.enableContinuousInput(-Math.PI, Math.PI);
        headingPid.setTolerance(kHeadingToleranceRad, kHeadingVelocityToleranceRadPerSec);
        headingPid.setIntegratorRange(-kIntegralContributionLimit, kIntegralContributionLimit);
    }

    @Override
    public void initialize() {
        translationPidX.reset();
        translationPidY.reset();
        headingPid.reset();
        System.out.println("[SetZero] initialized");
    }

    @Override
    public void execute() {
        var latest = state.getLatestFieldToRobot();
        if (latest == null) {
            System.out.println("[SetZero] pose is null, skipping");
            translationPidX.reset();
            translationPidY.reset();
            headingPid.reset();
            swerve.driveFieldOriented(new ChassisSpeeds());
            return;
        }

        double currentXMeter = latest.getValue().getX();
        double currentYMeter = latest.getValue().getY();
        double currentHeadingRad = latest.getValue().getRotation().getRadians();

        double translationX = MathUtil.clamp(
                        translationPidX.calculate(currentXMeter, 0.0),
                        -velocityMaximum, velocityMaximum);

        double translationY = MathUtil.clamp(
                        translationPidY.calculate(currentYMeter, 0.0),
                        -velocityMaximum, velocityMaximum);
                        
        double omega = MathUtil.clamp(
                        headingPid.calculate(currentHeadingRad, 0.0),
                        -omegaMaximum, omegaMaximum);

        boolean atTranslation = translationPidX.atSetpoint() && translationPidY.atSetpoint();
        boolean atHeading = headingPid.atSetpoint();
        if (atTranslation) {
            translationX = 0.0;
            translationY = 0.0;
        }
        if (atHeading) {
            omega = 0.0;
        }

        System.out.printf("[SetZero] pos=(%.2f, %.2f, %.2f) vel=(%.2f, %.2f, %.2f) atTranslation=%b atHeading=%b%n",
                currentXMeter, currentYMeter, currentHeadingRad, translationX, translationY, omega, atTranslation, atHeading);

        swerve.driveFieldOriented(new ChassisSpeeds(translationX, translationY, omega));
    }

    @Override
    public void end(boolean interrupted) {
        translationPidX.reset();
        translationPidY.reset();
        headingPid.reset();
        swerve.driveFieldOriented(new ChassisSpeeds());
        System.out.println("[SetZero] ended, interrupted=" + interrupted);
    }
}
