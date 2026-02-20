package frc.robot.commands.debug.odmetry;

import static frc.robot.lib.constants.LogConstants.*;

import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj2.command.Command;
import frc.robot.subsystems.SwerveSubsystem;
import frc.robot.RobotState;

public class SetDriveHorizonCommand extends Command {
    // 実機で「前進」と一致する向きに合わせる補正。
    // 現在の機体座標系では+vxが体感後進になるため、Aボタン用は-vxを前進として扱う。
    private static final double kForwardSpeedMetersPerSec = -1.0;

    private final SwerveSubsystem swerve;
    private final RobotState state;
    private double lastStatusLogSec = Double.NEGATIVE_INFINITY;

    public SetDriveHorizonCommand(
        SwerveSubsystem swerve,
        RobotState state
    ) {
        this.swerve = swerve;
        this.state = state;
        addRequirements(swerve);
    }

    @Override
    public void initialize() {
        lastStatusLogSec = Double.NEGATIVE_INFINITY;
        System.out.printf(
                "[SetDriveHorizon] INITIALIZED: mode=field-oriented vx=%.2f%n",
                kForwardSpeedMetersPerSec);
    }

    @Override
    public void execute() {
        swerve.driveFieldOriented(new ChassisSpeeds(kForwardSpeedMetersPerSec, 0, 0));

        double nowSec = Timer.getFPGATimestamp();

        var latest = state.getLatestFieldToRobot();
        var latestOdom = state.getLatestFieldToRobotOdom();

        double fusedXMeter = latest.getValue().getX();
        double fusedYMeter = latest.getValue().getY();
        double fusedHeadingRad = latest.getValue().getRotation().getRadians();
        
        double odomXMeter = latestOdom.getValue().getX();
        double odomYMeter = latestOdom.getValue().getY();
        double odomHeadingRad = latestOdom.getValue().getRotation().getRadians();

        if (nowSec - lastStatusLogSec >= kStatusLogPeriodSec) {
            lastStatusLogSec = nowSec;
            ChassisSpeeds measured = swerve.getSwerveDrive().getRobotVelocity();
            System.out.println("[SetDriveHorizon] periodic log");
            System.out.printf ("                  cmd=(%.2f, 0.00, 0.00)\n", kForwardSpeedMetersPerSec);
            System.out.printf ("                  measured=(%.2f, %.2f, %.2f)\n", measured.vxMetersPerSecond, measured.vyMetersPerSecond, measured.omegaRadiansPerSecond);
            System.out.printf ("                  fused=(%.2f, %.2f, %.2f)\n", fusedXMeter, fusedYMeter, fusedHeadingRad);
            System.out.printf ("                  odom=(%.2f, %.2f, %.2f)\n", odomXMeter, odomYMeter, odomHeadingRad);
        }
    }

    @Override
    public void end(boolean interrupted) {
        swerve.setChassisSpeeds(new ChassisSpeeds());
        System.out.println("[SetDriveHorizon] ended, interrupted=" + interrupted);
    }
}
