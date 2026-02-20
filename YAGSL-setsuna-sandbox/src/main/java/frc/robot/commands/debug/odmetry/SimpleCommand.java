package frc.robot.commands.debug.odmetry;

import static frc.robot.lib.util.Constants.SemiAutoConstants.velocityMaximum;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.wpilibj2.command.Command;
import frc.robot.RobotState;
import frc.robot.lib.util.Constants.PIDConstants;
import frc.robot.subsystems.SwerveSubsystem;

public class SimpleCommand extends Command {
    private static final double kTargetXMeter = -0.5;
    private static final double kTargetYMeter = 0.0;
    private static final double kTranslationToleranceMeters = 0.03;
    private static final double kTranslationVelocityToleranceMPerSec = 0.1;
    private static final double kIntegralContributionLimit = 0.3;

    private final SwerveSubsystem swerve;
    private final RobotState state;
    private final PIDController translationPidX =
            new PIDController(
                    PIDConstants.kTranslationKp,
                    PIDConstants.kTranslationKi,
                    PIDConstants.kTranslationKd);
    private final PIDController translationPidY =
            new PIDController(
                    PIDConstants.kTranslationKp,
                    PIDConstants.kTranslationKi,
                    PIDConstants.kTranslationKd);

    public SimpleCommand(SwerveSubsystem swerve, RobotState state) {
        this.swerve = swerve;
        this.state = state;
        addRequirements(swerve);

        translationPidX.setTolerance(
                kTranslationToleranceMeters, kTranslationVelocityToleranceMPerSec);
        translationPidX.setIntegratorRange(-kIntegralContributionLimit, kIntegralContributionLimit);
        translationPidY.setTolerance(
                kTranslationToleranceMeters, kTranslationVelocityToleranceMPerSec);
        translationPidY.setIntegratorRange(-kIntegralContributionLimit, kIntegralContributionLimit);
    }

    @Override
    public void initialize() {
        translationPidX.reset();
        translationPidY.reset();
    }

    @Override
    public void execute() {
        var latest = state.getLatestFieldToRobot();
        if (latest == null) {
            swerve.driveFieldOriented(new ChassisSpeeds());
            translationPidX.reset();
            translationPidY.reset();
            return;
        }

        double currentXMeter = latest.getValue().getX();
        double currentYMeter = latest.getValue().getY();

        double translationX =
                MathUtil.clamp(
                        translationPidX.calculate(currentXMeter, kTargetXMeter),
                        -velocityMaximum,
                        velocityMaximum);
        double translationY =
                MathUtil.clamp(
                        translationPidY.calculate(currentYMeter, kTargetYMeter),
                        -velocityMaximum,
                        velocityMaximum);

        if (translationPidX.atSetpoint() && translationPidY.atSetpoint()) {
            translationX = 0.0;
            translationY = 0.0;
        }

        swerve.driveFieldOriented(new ChassisSpeeds(translationX, translationY, 0.0));
    }

    @Override
    public void end(boolean interrupted) {
        translationPidX.reset();
        translationPidY.reset();
        swerve.driveFieldOriented(new ChassisSpeeds());
    }
}
