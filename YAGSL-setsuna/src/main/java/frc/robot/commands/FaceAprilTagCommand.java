
//
// FaceAprilTagCommand() -> FaceAprilTag()
//

package frc.robot.commands;

import frc.robot.subsystems.SwerveSubsystem;
import static frc.robot.lib.util.Constants.SemiAutoConstants.*;
import static frc.robot.lib.util.Constants.VisionConstants.kFaceAprilTagTargetOffset;
import frc.robot.lib.util.Constants.FieldConstants;
import frc.robot.lib.util.Constants.VisionConstants;
import frc.robot.RobotState;
import frc.robot.lib.time.RobotTime;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.wpilibj2.command.Command;
import org.littletonrobotics.junction.Logger;

import java.util.Optional;

// === 担当者 ===
// ひなた
//

public class FaceAprilTagCommand extends Command {
    private final SwerveSubsystem swerve;
    private final RobotState state;
    private final int targetTagId;

    private Optional<Pose2d> targetPose = Optional.empty();

    public FaceAprilTagCommand(
        SwerveSubsystem swerve,
        RobotState state
    ) {
        this(swerve, state, 1);
    }

    public FaceAprilTagCommand(
        SwerveSubsystem swerve,
        RobotState state,
        int targetTagId
    ) {
        this.swerve = swerve;
        this.state = state;
        this.targetTagId = targetTagId;
        addRequirements(swerve);
    }

    private boolean hasFreshFieldPose() {
        double lastTs = state.lastUsedMegatagTimestamp();
        if (lastTs <= 0.0) {
            return false;
        }
        double now = RobotTime.getTimestampSeconds();
        return (now - lastTs) <= VisionConstants.kFieldPoseValidTimeoutSec;
    }

    @Override
    public void initialize() {
        Logger.recordOutput("FaceAprilTag/Active", true);
        var tags = FieldConstants.kAprilTagLayout.getTags();
        double[] tagIds = new double[tags.size()];
        for (int i = 0; i < tags.size(); i++) {
            tagIds[i] = tags.get(i).ID;
        }
        Logger.recordOutput("FaceAprilTag/FieldLayoutTagCount", tagIds.length);
        Logger.recordOutput("FaceAprilTag/FieldLayoutTagIds", tagIds);
        Logger.recordOutput("FaceAprilTag/FieldLayoutFieldLength", FieldConstants.kAprilTagLayout.getFieldLength());
        Logger.recordOutput("FaceAprilTag/FieldLayoutFieldWidth", FieldConstants.kAprilTagLayout.getFieldWidth());

        Logger.recordOutput("FaceAprilTag/TargetTagId", targetTagId);
        var maybeFieldToTag = FieldConstants.kAprilTagLayout.getTagPose(targetTagId);
        Logger.recordOutput("FaceAprilTag/TagPosePresent", maybeFieldToTag.isPresent());
        if (maybeFieldToTag.isPresent()) {
            Pose2d fieldToTag = maybeFieldToTag.get().toPose2d();
            Pose2d fieldToTarget = fieldToTag.transformBy(kFaceAprilTagTargetOffset);
            targetPose = Optional.of(fieldToTarget);
            Logger.recordOutput("FaceAprilTag/FieldToTarget", fieldToTarget);
        } else {
            targetPose = Optional.empty();
        }
    }

    @Override
    public void execute() {
        // === 絶対座標でタグ1に向かう ===
        // 1) FieldLayout でタグ1の絶対座標を取得
        // 2) interestoffset と一致させたオフセット(kFaceAprilTagTargetOffset)を加えて目標座標を作る
        // 3) RobotState の融合姿勢との差分を「フィールド座標の誤差」として扱う
        // 4) 誤差から field-relative 速度を作り、ロボット座標系へ変換して出力
        if (targetPose.isEmpty()) {
            // まだタグの座標が確定していない場合は停止
            Logger.recordOutput("FaceAprilTag/HasTargetPose", false);
            swerve.setChassisSpeeds(new ChassisSpeeds(0, 0, 0));
            return;
        }
        Logger.recordOutput("FaceAprilTag/HasTargetPose", true);

        boolean hasPose = hasFreshFieldPose();
        Logger.recordOutput("FaceAprilTag/HasFreshFieldPose", hasPose);
        if (!hasPose) {
            swerve.setChassisSpeeds(new ChassisSpeeds(0, 0, 0));
            return;
        }

        var latestFieldToRobot = state.getLatestFieldToRobot();
        if (latestFieldToRobot == null) {
            Logger.recordOutput("FaceAprilTag/HasFieldPose", false);
            swerve.setChassisSpeeds(new ChassisSpeeds(0, 0, 0));
            return;
        }
        Logger.recordOutput("FaceAprilTag/HasFieldPose", true);

        Pose2d fieldToRobot = latestFieldToRobot.getValue();
        Pose2d fieldToTarget = targetPose.get();
        Logger.recordOutput("FaceAprilTag/FieldToRobot", fieldToRobot);

        // =========================
        // 1) 並進（フィールド座標系の目標へ）
        // =========================
        Translation2d fieldError = fieldToTarget.getTranslation().minus(fieldToRobot.getTranslation());
        double forwardErrorWithTargetMeter = fieldError.getX();
        double leftErrorWithTargetMeter = fieldError.getY();
        Logger.recordOutput("FaceAprilTag/FieldError", fieldError);

        double velocity_x = MathUtil.clamp(translationGain * forwardErrorWithTargetMeter, -velocityMaximum, velocityMaximum);
        double velocity_y = MathUtil.clamp(translationGain * leftErrorWithTargetMeter, -velocityMaximum, velocityMaximum);

        if (Math.abs(forwardErrorWithTargetMeter) < planeDeadbandMeter) velocity_x = 0.0;
        if (Math.abs(leftErrorWithTargetMeter) < planeDeadbandMeter) velocity_y = 0.0;

        // =========================
        // 2) 回転（タグ方向に正対）
        // =========================
        double angularErrorWithTargetRad = 0.0;
        if (fieldError.getNorm() >= planeDeadbandMeter) {
            Rotation2d desiredHeading = new Rotation2d(fieldError.getX(), fieldError.getY());
            angularErrorWithTargetRad = MathUtil.angleModulus(
                desiredHeading.minus(fieldToRobot.getRotation()).getRadians());
        }

        double omega = MathUtil.clamp(angularGain * angularErrorWithTargetRad, -omegaMaximum, omegaMaximum);

        if (Math.abs(angularErrorWithTargetRad) < thetaDeadbandRad) omega = 0.0;
        Logger.recordOutput("FaceAprilTag/AngularErrorRad", angularErrorWithTargetRad);
        Logger.recordOutput("FaceAprilTag/Omega", omega);
        Logger.recordOutput("FaceAprilTag/VelocityX", velocity_x);
        Logger.recordOutput("FaceAprilTag/VelocityY", velocity_y);

        ChassisSpeeds speeds = ChassisSpeeds.fromFieldRelativeSpeeds(
            velocity_x,
            velocity_y,
            omega,
            fieldToRobot.getRotation());

        swerve.setChassisSpeeds(speeds);
    }

    @Override
    public void end(boolean interrupted) {
        Logger.recordOutput("FaceAprilTag/Active", false);
        swerve.setChassisSpeeds(new ChassisSpeeds(0, 0, 0));
    }
}
