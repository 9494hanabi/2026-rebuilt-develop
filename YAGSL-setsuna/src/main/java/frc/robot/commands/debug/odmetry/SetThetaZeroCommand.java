package frc.robot.commands.debug.odmetry;

import static frc.robot.lib.util.Constants.SemiAutoConstants.omegaMaximum;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.wpilibj2.command.Command;
import frc.robot.RobotState;
import frc.robot.subsystems.SwerveSubsystem;

public class SetThetaZeroCommand extends Command {
    private static final double kHeadingKp = 0.1;
    private static final double kHeadingKi = 0.1;
    private static final double kHeadingKd = 0.1;
    private static final double kHeadingToleranceRad = Math.toRadians(1.0);
    private static final double kHeadingVelocityToleranceRadPerSec = Math.toRadians(8.0);
    private static final double kIntegralContributionLimit = 0.3;

    private final SwerveSubsystem swerve;
    private final RobotState state;
    private final PIDController headingPid =
        new PIDController(kHeadingKp, kHeadingKi, kHeadingKd);
    
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
    }

    @Override
    public void execute() {
        var latest = state.getLatestFieldToRobot();
        if (latest == null) {
            headingPid.reset();
            swerve.setChassisSpeeds(new ChassisSpeeds());
            return;
        }

        double currentHeadingRad = latest.getValue().getRotation().getRadians();
        double omega = MathUtil.clamp(
                        headingPid.calculate(currentHeadingRad, 0.0),
                        -omegaMaximum, omegaMaximum);

        if (headingPid.atSetpoint()) {
            omega = 0.0;
        }
        swerve.setChassisSpeeds(new ChassisSpeeds(0.0, 0.0, omega));
    }

    @Override
    public void end(boolean interrupted) {
        headingPid.reset();
        swerve.setChassisSpeeds(new ChassisSpeeds());
    }
}
