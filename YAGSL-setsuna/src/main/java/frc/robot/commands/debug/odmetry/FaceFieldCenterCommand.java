package frc.robot.commands.debug.odmetry;

import static frc.robot.lib.constants.SemiAutoConstants.omegaMaximum;
import static frc.robot.lib.constants.PIDConstants.*;
import static frc.robot.lib.constants.LogConstants.*;

import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj2.command.Command;

import frc.robot.RobotState;
import frc.robot.subsystems.SwerveSubsystem;
import frc.robot.lib.constants.FieldConstants;
import frc.robot.lib.util.OdomHeadingController;

// フィールドの中心を常に向き続けるコマンド。
// ロボットの現在位置からフィールド中心への角度を目標ヨーとして追従する。
public class FaceFieldCenterCommand extends Command {

    private static final Translation2d FIELD_CENTER =
            new Translation2d(
                    FieldConstants.fieldLengthMeter / 2.0,
                    FieldConstants.fieldWidthMeter / 2.0);
    private static final Rotation2d ROBOT_FRONT_HEADING_OFFSET = Rotation2d.fromDegrees(180.0);

    private final SwerveSubsystem swerve;
    private final RobotState state;
    private final PIDController headingPid =
            new PIDController(kHeadingKp, kHeadingKi, kHeadingKd);
    private final OdomHeadingController headingControl;
    private double lastStatusLogSec = Double.NEGATIVE_INFINITY;

    public FaceFieldCenterCommand(
            SwerveSubsystem swerve,
            RobotState state) {
        this.swerve = swerve;
        this.state = state;
        addRequirements(swerve);

        headingPid.disableContinuousInput();
        headingPid.setTolerance(kHeadingToleranceRad, kHeadingVelocityToleranceRadPerSec);
        headingPid.setIntegratorRange(
                -kHeadingIntegralContributionLimit, kHeadingIntegralContributionLimit);

        headingControl = new OdomHeadingController(headingPid, omegaMaximum);
    }

    @Override
    public void initialize() {
        headingControl.reset();
        lastStatusLogSec = Double.NEGATIVE_INFINITY;
        System.out.println("[FaceFieldCenter] INITIALIZED");
    }

    @Override
    public void execute() {
        double nowSec = Timer.getFPGATimestamp();
        var latest = state.getLatestFieldToRobotOdom();
        if (latest == null) {
            if (nowSec - lastStatusLogSec >= kStatusLogPeriodSec) {
                lastStatusLogSec = nowSec;
                System.out.println("[FaceFieldCenter] !!ODOM POSE IS NULL!!");
            }
            headingPid.reset();
            swerve.driveFieldOriented(new ChassisSpeeds());
            return;
        }

        Pose2d robotPose = latest.getValue();
        Translation2d robotToCenter =
                FIELD_CENTER.minus(robotPose.getTranslation());

        // ロボット位置がフィールド中心とほぼ一致する場合は回転しない
        if (robotToCenter.getNorm() < 0.05) {
            swerve.driveFieldOriented(new ChassisSpeeds());
            return;
        }

        // フィールド中心への方位角 + 180deg補正でロボット前面を向ける
        double targetHeadingRad = robotToCenter.getAngle().plus(ROBOT_FRONT_HEADING_OFFSET).getRadians();
        double currentHeadingRad = robotPose.getRotation().getRadians();

        var maybeResult = headingControl.calculate(currentHeadingRad, targetHeadingRad);

        if (maybeResult.isEmpty()) {
            if (nowSec - lastStatusLogSec >= kStatusLogPeriodSec) {
                lastStatusLogSec = nowSec;
                System.out.println("[FaceFieldCenter] !!INVALID!!");
            }
            headingControl.reset();
            swerve.driveFieldOriented(new ChassisSpeeds());
            return;
        }

        var result = maybeResult.get();
        double omega = result.omegaRadPerSec();

        if (!Double.isFinite(omega)) {
            if (nowSec - lastStatusLogSec >= kStatusLogPeriodSec) {
                lastStatusLogSec = nowSec;
                System.out.printf("[FaceFieldCenter] !!INVALID omega=%.4f!!%n", omega);
            }
            headingControl.reset();
            swerve.driveFieldOriented(new ChassisSpeeds());
            return;
        }

        if (nowSec - lastStatusLogSec >= kStatusLogPeriodSec) {
            lastStatusLogSec = nowSec;
            System.out.printf("[FaceFieldCenter] target=%.2fdeg current=%.2fdeg omega=%.4f atSetpoint=%b%n",
                    Math.toDegrees(targetHeadingRad),
                    Math.toDegrees(result.wrappedCurrentHeadingRad()),
                    omega,
                    result.atSetpoint());
        }

        swerve.driveFieldOriented(new ChassisSpeeds(0.0, 0.0, omega));
    }

    @Override
    public void end(boolean interrupted) {
        headingControl.reset();
        swerve.driveFieldOriented(new ChassisSpeeds());
        System.out.println("[FaceFieldCenter] ended, interrupted=" + interrupted);
    }
}
