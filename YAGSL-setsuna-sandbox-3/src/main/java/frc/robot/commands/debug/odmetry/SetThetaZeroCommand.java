package frc.robot.commands.debug.odmetry;

import static frc.robot.lib.util.Constants.SemiAutoConstants.omegaMaximum;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj2.command.Command;
import frc.robot.RobotState;
import frc.robot.subsystems.SwerveSubsystem;
import frc.robot.lib.util.OdomHeadingController;
import frc.robot.lib.util.Constants.PIDConstants;

public class SetThetaZeroCommand extends Command {

    private static final double kHeadingToleranceRad = Math.toRadians(1.0);
    private static final double kHeadingVelocityToleranceRadPerSec = Math.toRadians(8.0);
    private static final double kIntegralContributionLimit = 0.3;
    private static final double kStatusLogPeriodSec = 0.20;

    private final SwerveSubsystem swerve;
    private final RobotState state;
    private final PIDController headingPid =
        new PIDController(PIDConstants.kHeadingKp, PIDConstants.kHeadingKi, PIDConstants.kHeadingKd);
    private final OdomHeadingController headingControl;
    private double lastStatusLogSec = Double.NEGATIVE_INFINITY;
    
    public SetThetaZeroCommand(
        SwerveSubsystem swerve,
        RobotState state
    ) {
        this.swerve = swerve;
        this.state = state;
        addRequirements(swerve);

        headingPid.disableContinuousInput();
        headingPid.setTolerance(kHeadingToleranceRad, kHeadingVelocityToleranceRadPerSec);
        headingPid.setIntegratorRange(-kIntegralContributionLimit, kIntegralContributionLimit);
        headingControl = new OdomHeadingController(headingPid, omegaMaximum);
    }
    @Override
    public void initialize() {
        headingControl.reset();
        lastStatusLogSec = Double.NEGATIVE_INFINITY;
        System.out.println("[SetThetaZero] initialized");
    }

    @Override
    public void execute() {
        double nowSec = Timer.getFPGATimestamp();
        var latest = state.getLatestFieldToRobotOdom();
        if (latest == null) {
            if (nowSec - lastStatusLogSec >= kStatusLogPeriodSec) {
                lastStatusLogSec = nowSec;
                System.out.println("[SetThetaZero] odom pose is null, skipping");
            }
            headingPid.reset();
            swerve.driveFieldOriented(new ChassisSpeeds());
            return;
        }

        double wrappedHeadingRad = latest.getValue().getRotation().getRadians();
        var maybeHeadingResult = headingControl.calculate(wrappedHeadingRad, 0.0);
        if (maybeHeadingResult.isEmpty()) {
            if (nowSec - lastStatusLogSec >= kStatusLogPeriodSec) {
                lastStatusLogSec = nowSec;
                System.out.printf("[SetThetaZero] invalid heading=%.4f, stop%n", wrappedHeadingRad);
            }
            headingControl.reset();
            swerve.driveFieldOriented(new ChassisSpeeds());
            return;
        }
        var headingResult = maybeHeadingResult.get();
        double omega = headingResult.omegaRadPerSec();
        if (!Double.isFinite(omega)) {
            if (nowSec - lastStatusLogSec >= kStatusLogPeriodSec) {
                lastStatusLogSec = nowSec;
                System.out.printf("[SetThetaZero] invalid omega=%.4f, stop%n", omega);
            }
            headingControl.reset();
            swerve.driveFieldOriented(new ChassisSpeeds());
            return;
        }

        boolean atSetpoint = headingResult.atSetpoint();

        if (nowSec - lastStatusLogSec >= kStatusLogPeriodSec) {
            lastStatusLogSec = nowSec;
            System.out.printf("[SetThetaZero] headingWrap=%.4f rad (%.2f deg) headingCont=%.4f omega=%.4f atSetpoint=%b%n",
                    headingResult.wrappedCurrentHeadingRad(),
                    Math.toDegrees(headingResult.wrappedCurrentHeadingRad()),
                    headingResult.continuousCurrentHeadingRad(),
                    omega,
                    atSetpoint);
        }

        swerve.driveFieldOriented(new ChassisSpeeds(0.0, 0.0, omega));
        
    }

    @Override
    public void end(boolean interrupted) {
        headingControl.reset();
        swerve.driveFieldOriented(new ChassisSpeeds());
        System.out.println("[SetThetaZero] ended, interrupted=" + interrupted);
    }
}
