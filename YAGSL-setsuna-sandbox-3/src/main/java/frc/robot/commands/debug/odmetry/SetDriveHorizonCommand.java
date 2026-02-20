package frc.robot.commands.debug.odmetry;

import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj2.command.Command;
import frc.robot.subsystems.SwerveSubsystem;

public class SetDriveHorizonCommand extends Command {
    // 実機で「前進」と一致する向きに合わせる補正。
    // 現在の機体座標系では+vxが体感後進になるため、Aボタン用は-vxを前進として扱う。
    private static final double kForwardSpeedMetersPerSec = -1.0;
    private static final double kStatusLogPeriodSec = 0.50;

    private final SwerveSubsystem swerve;
    private double lastStatusLogSec = Double.NEGATIVE_INFINITY;

    public SetDriveHorizonCommand(
        SwerveSubsystem swerve
    ) {
        this.swerve = swerve;
        addRequirements(swerve);
    }

    @Override
    public void initialize() {
        lastStatusLogSec = Double.NEGATIVE_INFINITY;
        System.out.printf(
                "[SetDriveHorizon] initialized: mode=robot-relative vx=%.2f%n",
                kForwardSpeedMetersPerSec);
    }

    @Override
    public void execute() {
        swerve.setChassisSpeeds(new ChassisSpeeds(kForwardSpeedMetersPerSec, 0.0, 0.0));

        double nowSec = Timer.getFPGATimestamp();
        if (nowSec - lastStatusLogSec >= kStatusLogPeriodSec) {
            lastStatusLogSec = nowSec;
            ChassisSpeeds measured = swerve.getSwerveDrive().getRobotVelocity();
            System.out.printf(
                    "[SetDriveHorizon] cmd=(%.2f, 0.00, 0.00) measured=(%.2f, %.2f, %.2f)%n",
                    kForwardSpeedMetersPerSec,
                    measured.vxMetersPerSecond,
                    measured.vyMetersPerSecond,
                    measured.omegaRadiansPerSecond);
        }
    }

    @Override
    public void end(boolean interrupted) {
        swerve.setChassisSpeeds(new ChassisSpeeds());
        System.out.println("[SetDriveHorizon] ended, interrupted=" + interrupted);
    }
}
