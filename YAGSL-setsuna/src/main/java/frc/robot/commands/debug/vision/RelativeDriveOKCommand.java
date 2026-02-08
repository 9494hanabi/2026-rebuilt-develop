package frc.robot.commands.debug.vision;

import frc.robot.subsystems.SwerveSubsystem;
import frc.robot.lib.limelight.LimelightHelpers;
import frc.robot.lib.util.Constants.VisionConstants;

import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.math.kinematics.ChassisSpeeds;

import static frc.robot.lib.util.Constants.SemiAutoConstants.angularGain;
import static frc.robot.lib.util.Constants.SemiAutoConstants.omegaMaximum;
import static frc.robot.lib.util.Constants.SemiAutoConstants.planeDeadbandMeter;
import static frc.robot.lib.util.Constants.SemiAutoConstants.thetaDeadbandRad;
import static frc.robot.lib.util.Constants.SemiAutoConstants.translationGain;
import static frc.robot.lib.util.Constants.SemiAutoConstants.velocityMaximum;

import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.MathUtil;

public class RelativeDriveOKCommand extends Command{
    private static final double kTagLockHoldSeconds = 0.25;

    private final SwerveSubsystem swerve;
    private final DebugVisionTargetSelector targetSelector;

    private int lockedTagId = -1;
    private String lockedTableName = null;
    private double lastLockSeenTimestampSec = Double.NEGATIVE_INFINITY;

    public RelativeDriveOKCommand(
            SwerveSubsystem swerve) {
        this.swerve = swerve;
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
        var sameTagObservation =
            lockedTagId < 0 ? java.util.Optional.<DebugVisionTargetSelector.TargetObservation>empty()
                            : targetSelector.observationForTag(lockedTagId);

        if (sameTagObservation.isPresent()) {
            lockTo(sameTagObservation.get(), nowSec);
            return;
        }

        if (lockedTagId >= 0 && (nowSec - lastLockSeenTimestampSec) <= kTagLockHoldSeconds) {
            return;
        }

        var bestObservation = targetSelector.selectBestObservation();
        if (bestObservation.isPresent()) {
            lockTo(bestObservation.get(), nowSec);
            return;
        }

        clearLock();
    }

    @Override
    public void execute() {
        double nowSec = Timer.getFPGATimestamp();
        updateTagLock(nowSec);

        ChassisSpeeds cmd = new ChassisSpeeds();
        if (lockedTagId < 0 || lockedTableName == null) {
            swerve.setChassisSpeeds(cmd);
            return;
        }

        // いま見えている primary tag がロック中IDと一致するフレームだけ採用する。
        // 2タグ同時視認時のID飛びによる逆方向指令を防ぐ。
        if (!targetSelector.isTagVisible(lockedTableName, lockedTagId)) {
            swerve.setChassisSpeeds(cmd);
            return;
        }

        Pose3d targetInBotSpace = LimelightHelpers.getTargetPose3d_RobotSpace(lockedTableName);

        double targetX = targetInBotSpace.getX();
        double targetY = targetInBotSpace.getY();
        double angularErrorRad = Math.atan2(targetY, targetX);

        double vx = MathUtil.clamp(
            translationGain * targetX,
            -velocityMaximum,
            velocityMaximum);
        double vy = MathUtil.clamp(
            translationGain * targetY,
            -velocityMaximum,
            velocityMaximum);
        double omega = MathUtil.clamp(
            angularGain * angularErrorRad,
            -omegaMaximum,
            omegaMaximum);

        if (Math.abs(targetX) < planeDeadbandMeter) {
            vx = 0.0;
        }
        if (Math.abs(targetY) < planeDeadbandMeter) {
            vy = 0.0;
        }
        if (Math.abs(angularErrorRad) < thetaDeadbandRad) {
            omega = 0.0;
        }

        cmd = new ChassisSpeeds(vx, vy, omega);

        swerve.setChassisSpeeds(cmd);
    }

    @Override
    public void end(boolean interrupted) {
        swerve.setChassisSpeeds(new ChassisSpeeds());  // 停止
    }
}
