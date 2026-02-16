package frc.robot.commands.debug.odmetry;

import static frc.robot.lib.util.Constants.SemiAutoConstants.omegaMaximum;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.wpilibj2.command.Command;
import frc.robot.RobotState;
import frc.robot.subsystems.SwerveSubsystem;
import frc.robot.lib.util.Constants.PIDConstants;

public class SetThetaZeroCommand extends Command {
    private static final double kHeadingToleranceRad = Math.toRadians(1.0);
    private static final double kHeadingVelocityToleranceRadPerSec = Math.toRadians(8.0);
    private static final double kIntegralContributionLimit = 0.3;

    private final SwerveSubsystem swerve;
    private final RobotState state;
    private final PIDController headingPid =
        new PIDController(PIDConstants.kHeadingKp, PIDConstants.kHeadingKi, PIDConstants.kHeadingKd);
    
    public SetThetaZeroCommand(
        SwerveSubsystem swerve,
        RobotState state
    ) {
        this.swerve = swerve;
        this.state = state;
        addRequirements(swerve);

        headingPid.enableContinuousInput(-Math.PI, Math.PI);
        headingPid.setTolerance(kHeadingToleranceRad, kHeadingVelocityToleranceRadPerSec);
        headingPid.setIntegratorRange(-kIntegralContributionLimit, kIntegralContributionLimit);
    }

    @Override
    public void initialize() {
        headingPid.reset();
        System.out.println("[SetThetaZero] initialized");
    }

    @Override
    public void execute() {
        var latest = state.getLatestFieldToRobot();
        if (latest == null) {
            System.out.println("[SetThetaZero] pose is null, skipping");
            headingPid.reset();
            swerve.setChassisSpeeds(new ChassisSpeeds());
            return;
        }

        double currentHeadingRad = latest.getValue().getRotation().getRadians();
        double omega = MathUtil.clamp(
                        headingPid.calculate(currentHeadingRad, 0.0),
                        -omegaMaximum, omegaMaximum);

        boolean atSetpoint = headingPid.atSetpoint();
        if (atSetpoint) {
            omega = 0.0;
        }

        System.out.printf("[SetThetaZero] heading=%.4f rad (%.2f deg) omega=%.4f atSetpoint=%b%n",
                currentHeadingRad, Math.toDegrees(currentHeadingRad), omega, atSetpoint);

        swerve.setChassisSpeeds(new ChassisSpeeds(0.0, 0.0, omega));
    }

    @Override
    public void end(boolean interrupted) {
        headingPid.reset();
        swerve.setChassisSpeeds(new ChassisSpeeds());
        System.out.println("[SetThetaZero] ended, interrupted=" + interrupted);
    }
}
