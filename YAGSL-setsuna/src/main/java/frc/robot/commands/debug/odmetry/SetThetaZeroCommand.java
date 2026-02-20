package frc.robot.commands.debug.odmetry;

import static frc.robot.lib.constants.SemiAutoConstants.omegaMaximum;
import static frc.robot.lib.constants.PIDConstants.*;
import static frc.robot.lib.constants.LogConstants.*;

import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj2.command.Command;

import frc.robot.RobotState;
import frc.robot.subsystems.SwerveSubsystem;
import frc.robot.lib.util.OdomHeadingController;

public class SetThetaZeroCommand extends Command {
    
    private final SwerveSubsystem swerve;
    private final RobotState state;
    private final PIDController headingPid =
        new PIDController(kHeadingKp, kHeadingKi, kHeadingKd);
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

        // PID制御の到達判定を設定するメソッド
        headingPid.setTolerance(kHeadingToleranceRad, kHeadingVelocityToleranceRadPerSec);

        // I値の出力上限を設定するメソッド
        headingPid.setIntegratorRange(-kHeadingIntegralContributionLimit, kHeadingIntegralContributionLimit);

        headingControl = new OdomHeadingController(headingPid, omegaMaximum);
    }

    @Override
    public void initialize() {
        headingControl.reset();
        lastStatusLogSec = Double.NEGATIVE_INFINITY;
        System.out.println("[SetThetaZero] INITIALIZED");
    }

    @Override
    public void execute() {
        double nowSec = Timer.getFPGATimestamp();
        var latest = state.getLatestFieldToRobotOdom();
        if (latest == null) {
            if (nowSec - lastStatusLogSec >= kStatusLogPeriodSec) {
                lastStatusLogSec = nowSec;
                System.out.println("[SetThetaZero] !!ODOM POSE IS NULL!!");
                System.out.println("               skipping");

            }
            headingPid.reset();
            swerve.driveFieldOriented(new ChassisSpeeds());
            return;
        }

        double wrappedHeadingRad = latest.getValue().getRotation().getRadians();
        var maybeHeadingResult = headingControl.calculate(wrappedHeadingRad, 0.0);

        // 変化量が0の時停止
        if (maybeHeadingResult.isEmpty()) {
            if (nowSec - lastStatusLogSec >= kStatusLogPeriodSec) {
                lastStatusLogSec = nowSec;
                System.out.println("[SetThetaZero] !!INVALID!!");
                System.out.printf ("               heading=%.4f\n", wrappedHeadingRad);
                System.out.println("[SetThetaZero] ROBOT STOPPED");
            }
            headingControl.reset();
            swerve.driveFieldOriented(new ChassisSpeeds());
            return;
        }

        // 変化量が0でないとき、maybeHeadingResultを出力
        var headingResult = maybeHeadingResult.get();

        // 角速度の変化量を取得
        double omega = headingResult.omegaRadPerSec();

        // omegaが有限な値かチェックしている //
        if (!Double.isFinite(omega)) {

            // 0.2秒周期でログを出力するための機能
            // 最新のステータスログに対して、現在時間が0.2秒以上経っている時
            if (nowSec - lastStatusLogSec >= kStatusLogPeriodSec) {
                lastStatusLogSec = nowSec;
                System.out.println("[SetThetaZero] !!INVALID!!");
                System.out.printf ("               omega=%.4f\n", omega);
                System.out.println("[SetThetaZero] ROBOT STOPPED");
            }
            headingControl.reset();
            swerve.driveFieldOriented(new ChassisSpeeds());
            return;
        }

        boolean atSetpoint = headingResult.atSetpoint();

        if (nowSec - lastStatusLogSec >= kStatusLogPeriodSec) {
            lastStatusLogSec = nowSec;
            System.out.println("[SetThetaZero] periodic log");
            System.out.printf ("               headingWrap=%.4f rad (%.2f deg)\n", headingResult.wrappedCurrentHeadingRad(), Math.toDegrees(headingResult.wrappedCurrentHeadingRad()));
            System.out.printf ("               headingCont=%.4f\n", headingResult.continuousCurrentHeadingRad());
            System.out.printf ("               omega=%.4f\n", omega);
            System.out.printf ("               atSetpoint=%b\n", atSetpoint);
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