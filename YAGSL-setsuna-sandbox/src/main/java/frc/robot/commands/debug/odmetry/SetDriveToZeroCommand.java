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

public class SetDriveToZeroCommand extends Command {
    private static final double kTranslationToleranceMeters = 0.05;
    private static final double kTranslationVelocityToleranceMPerSec = 0.1;
    private static final double kIntegralContributionLimit = 0.3;

    private final SwerveSubsystem swerve;
    private final RobotState state;
    private final PIDController translationPidX =
        new PIDController(PIDConstants.kTranslationKp, PIDConstants.kTranslationKi, PIDConstants.kTranslationKd);
    private final PIDController translationPidY =
        new PIDController(PIDConstants.kTranslationKp, PIDConstants.kTranslationKi, PIDConstants.kTranslationKd);

    public SetDriveToZeroCommand(
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
    }

    @Override
    public void initialize() {
        translationPidX.reset();
        translationPidY.reset();
        System.out.println("[SetDriveToZero] initialized");
    }

    @Override
    public void execute() {
        var latest = state.getLatestFieldToRobot();
        if (latest == null) {
            System.out.println("[SetDriveToZero] pose is null, skipping");
            translationPidX.reset();
            translationPidY.reset();
            swerve.driveFieldOriented(new ChassisSpeeds());
            return;
        }

        double currentXMeter = latest.getValue().getX();
        double currentYMeter = latest.getValue().getY();

        double translationX = MathUtil.clamp(
                        translationPidX.calculate(currentXMeter, 0.0),
                        -velocityMaximum, velocityMaximum);

        double translationY = MathUtil.clamp(
                        translationPidY.calculate(currentYMeter, 0.0),
                        -velocityMaximum, velocityMaximum);

        boolean atSetpoint = translationPidX.atSetpoint() && translationPidY.atSetpoint();
        if (atSetpoint) {
            translationX = 0.0;
            translationY = 0.0;
        }

        System.out.printf("[SetDriveToZero] pos=(%.2f, %.2f) vel=(%.2f, %.2f) atSetpoint=%b%n",
                currentXMeter, currentYMeter, translationX, translationY, atSetpoint);

        swerve.driveFieldOriented(new ChassisSpeeds(translationX, translationY, 0.375));
    }

    @Override
    public void end(boolean interrupted) {
        translationPidX.reset();
        translationPidY.reset();
        swerve.driveFieldOriented(new ChassisSpeeds());
        System.out.println("[SetDriveToZero] ended, interrupted=" + interrupted);
    }
}
