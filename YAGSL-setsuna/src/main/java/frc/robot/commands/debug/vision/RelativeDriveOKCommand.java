package frc.robot.commands.debug.vision;

import frc.robot.RobotState;
import frc.robot.subsystems.SwerveSubsystem;
import frc.robot.lib.util.Constants.FieldConstants;
import frc.robot.lib.util.Constants.VisionConstants;

import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.math.kinematics.ChassisSpeeds;

import static frc.robot.lib.util.Constants.SemiAutoConstants.angularGain;
import static frc.robot.lib.util.Constants.SemiAutoConstants.omegaMaximum;
import static frc.robot.lib.util.Constants.SemiAutoConstants.planeDeadbandMeter;
import static frc.robot.lib.util.Constants.SemiAutoConstants.thetaDeadbandRad;
import static frc.robot.lib.util.Constants.SemiAutoConstants.translationGain;
import static frc.robot.lib.util.Constants.SemiAutoConstants.velocityMaximum;

import edu.wpi.first.math.MathUtil;

public class RelativeDriveOKCommand extends Command{
    private static final double kTagLockHoldSeconds = 0.75;
    private static final double kTranslationKi = 0.0;
    private static final double kTranslationKd = 0.0;
    private static final double kAngularKi = 0.0;
    private static final double kAngularKd = 0.0;

    private final SwerveSubsystem swerve;
    private final RobotState state;
    private final DebugVisionTargetSelector targetSelector;

    // PID制御
    private final PIDController xController =
        new PIDController(translationGain, kTranslationKi, kTranslationKd);
    private final PIDController yController =
        new PIDController(translationGain, kTranslationKi, kTranslationKd);
    private final PIDController omegaController =
        new PIDController(angularGain, kAngularKi, kAngularKd);

    private int lockedTagId = -1;
    private String lockedTableName = null;
    private double lastLockSeenTimestampSec = Double.NEGATIVE_INFINITY;

    public RelativeDriveOKCommand(
            SwerveSubsystem swerve,
            RobotState state) {
        this.swerve = swerve;
        this.state = state;
        this.targetSelector =
            new DebugVisionTargetSelector(
                VisionConstants.kLimelightATableName,
                VisionConstants.kLimelightBTableName);
        addRequirements(swerve);
    }

    @Override
    public void initialize() {
        lockedTagId = -1;
        lockedTableName = null;
        lastLockSeenTimestampSec = Double.NEGATIVE_INFINITY;
        xController.reset();
        yController.reset();
        omegaController.reset();
        xController.setTolerance(planeDeadbandMeter);
        yController.setTolerance(planeDeadbandMeter);
        omegaController.setTolerance(thetaDeadbandRad);
    }

    private void lockTo(DebugVisionTargetSelector.TargetObservation observation, double nowSec) {
        lockedTagId = observation.tagId();
        lockedTableName = observation.tableName();
        lastLockSeenTimestampSec = nowSec;
    }

    private void clearLock() {
        lockedTagId = -1;
        lockedTableName = null;
        lastLockSeenTimestampSec = Double.NEGATIVE_INFINITY;
    }

    private void updateTagLock(double nowSec) {
        if (lockedTagId >= 0) {
            var sameTagObservation = targetSelector.observationForTag(lockedTagId);
            if (sameTagObservation.isPresent()) {
                lockTo(sameTagObservation.get(), nowSec);
                return;
            }

            if ((nowSec - lastLockSeenTimestampSec) <= kTagLockHoldSeconds) {
                return;
            }

            clearLock();
            return;
        }

        var bestObservation = targetSelector.selectBestObservation();
        bestObservation.ifPresent(observation -> lockTo(observation, nowSec));
    }

    @Override
    public void execute() {
        double nowSec = Timer.getFPGATimestamp();
        updateTagLock(nowSec);

        ChassisSpeeds cmd = new ChassisSpeeds();
        if (lockedTagId < 0 || lockedTableName == null) {
            swerve.setChassisSpeeds(cmd);
            xController.reset();
            yController.reset();
            omegaController.reset();
            return;
        }

        // いま見えている primary tag がロック中IDと一致するフレームだけ採用する。
        // 2タグ同時視認時のID飛びによる逆方向指令を防ぐ。
        if (!targetSelector.isTagVisible(lockedTableName, lockedTagId)) {
            swerve.setChassisSpeeds(cmd);
            return;
        }

        var latestFieldToRobot = state.getLatestFieldToRobot();
        if (latestFieldToRobot == null) {
            swerve.setChassisSpeeds(cmd);
            return;
        }
        var maybeFieldToTag = FieldConstants.kAprilTagLayout.getTagPose(lockedTagId);
        if (maybeFieldToTag.isEmpty()) {
            swerve.setChassisSpeeds(cmd);
            return;
        }

        var robotToTag = maybeFieldToTag.get().toPose2d().relativeTo(latestFieldToRobot.getValue());
        double targetX = robotToTag.getX();
        double targetY = robotToTag.getY();
        double angularErrorRad = Math.atan2(targetY, targetX);

        double vx = MathUtil.clamp(
            xController.calculate(-targetX, 0.0),
            -velocityMaximum,
            velocityMaximum);
        double vy = MathUtil.clamp(
            yController.calculate(-targetY, 0.0),
            -velocityMaximum,
            velocityMaximum);
        double omega = MathUtil.clamp(
            omegaController.calculate(angularErrorRad, 0.0),
            -omegaMaximum,
            omegaMaximum);

        if (Math.abs(targetX) < planeDeadbandMeter) {
            vx = 0.0;
            xController.reset();
        }
        if (Math.abs(targetY) < planeDeadbandMeter) {
            vy = 0.0;
            yController.reset();
        }
        if (Math.abs(angularErrorRad) < thetaDeadbandRad) {
            omega = 0.0;
            omegaController.reset();
        }

        cmd = new ChassisSpeeds(0, 0, 0.125);

        swerve.setChassisSpeeds(cmd);
    }

    @Override
    public void end(boolean interrupted) {
        swerve.setChassisSpeeds(new ChassisSpeeds());  // 停止
        xController.reset();
        yController.reset();
        omegaController.reset();
    }
}
