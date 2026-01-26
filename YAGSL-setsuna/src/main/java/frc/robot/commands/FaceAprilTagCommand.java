
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

import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableInstance;
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

    private final NetworkTable table =
    NetworkTableInstance.getDefault().getTable(VisionConstants.kFaceAprilTagTableName);

    private Optional<Pose2d> lastTargetPose = Optional.empty();

    public FaceAprilTagCommand(
        SwerveSubsystem swerve,
        RobotState state
    ) {
        this.swerve = swerve;
        this.state = state;
        addRequirements(swerve);
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
    }

    @Override
    public void execute() {
        // === 新方式: 絶対座標でタグに向かう ===
        // 1) 見えたタグID(tid)から FieldLayout で「タグの絶対座標」を取得
        // 2) interestoffset と一致させたオフセット(kFaceAprilTagTargetOffset)を加えて目標座標を作る
        // 3) RobotState の最新姿勢との差分を「フィールド座標の誤差」として扱う
        // 4) 誤差から field-relative 速度を作り、ロボット座標系へ変換して出力
        double tvRaw = table.getEntry("tv").getDouble(0);
        boolean tv = tvRaw == 1.0; // 1なら有効
        Logger.recordOutput("FaceAprilTag/tv", tv);
        Logger.recordOutput("FaceAprilTag/tvRaw", tvRaw);
        Logger.recordOutput("FaceAprilTag/pipeline", table.getEntry("pipeline").getDouble(-1));
        if (tv) {
            double tidRaw = table.getEntry("tid").getDouble(-1);
            int tagId = (int) Math.round(tidRaw);
            Logger.recordOutput("FaceAprilTag/tid", tagId);
            Logger.recordOutput("FaceAprilTag/tidRaw", tidRaw);
            if (tagId >= 0) {
                var maybeFieldToTag = FieldConstants.kAprilTagLayout.getTagPose(tagId);
                Logger.recordOutput("FaceAprilTag/TagPosePresent", maybeFieldToTag.isPresent());
                if (maybeFieldToTag.isPresent()) {
                    Pose2d fieldToTag = maybeFieldToTag.get().toPose2d();
                    Pose2d fieldToTarget = fieldToTag.transformBy(kFaceAprilTagTargetOffset);
                    lastTargetPose = Optional.of(fieldToTarget);
                    Logger.recordOutput("FaceAprilTag/FieldToTarget", fieldToTarget);
                }
            }
        }

        if (lastTargetPose.isEmpty()) {
            // まだタグの座標が確定していない場合は停止
            Logger.recordOutput("FaceAprilTag/HasTargetPose", false);
            swerve.setChassisSpeeds(new ChassisSpeeds(0, 0, 0));
            return;
        }
        Logger.recordOutput("FaceAprilTag/HasTargetPose", true);

        var latestFieldToRobot = state.getLatestFieldToRobot();
        if (latestFieldToRobot == null) {
            Logger.recordOutput("FaceAprilTag/HasFieldPose", false);
            swerve.setChassisSpeeds(new ChassisSpeeds(0, 0, 0));
            return;
        }
        Logger.recordOutput("FaceAprilTag/HasFieldPose", true);

        Pose2d fieldToRobot = latestFieldToRobot.getValue();
        Pose2d fieldToTarget = lastTargetPose.get();
        Logger.recordOutput("FaceAprilTag/FieldToRobot", fieldToRobot);

        // =========================
        // 1) 並進（フィールド座標系の目標へ）
        // =========================
        Translation2d fieldError = fieldToTarget.getTranslation().minus(fieldToRobot.getTranslation());
        double forwardErrorwithTargetMeter = fieldError.getX();
        double leftErrorwithTargetMeter = fieldError.getY();
        Logger.recordOutput("FaceAprilTag/FieldError", fieldError);

        double velocity_x = MathUtil.clamp(translationGain * forwardErrorwithTargetMeter, -velocityMaximum, velocityMaximum);
        double velocity_y = MathUtil.clamp(translationGain * leftErrorwithTargetMeter, -velocityMaximum, velocityMaximum);

        if (Math.abs(forwardErrorwithTargetMeter) < planeDeadbandMeter) velocity_x = 0.0;
        if (Math.abs(leftErrorwithTargetMeter) < planeDeadbandMeter) velocity_y = 0.0;

        // =========================
        // 2) 回転（タグ方向に正対）
        // =========================
        double angularErrorwithTargetRad = 0.0;
        if (fieldError.getNorm() >= planeDeadbandMeter) {
            Rotation2d desiredHeading = new Rotation2d(fieldError.getX(), fieldError.getY());
            angularErrorwithTargetRad = MathUtil.angleModulus(
                desiredHeading.minus(fieldToRobot.getRotation()).getRadians());
        }

        // ChassisSpeeds: omegaはrad/s  [oai_citation:8‡FIRST Robotics Competition Documentation](https://docs.wpilib.org/en/stable/docs/software/kinematics-and-odometry/intro-and-chassis-speeds.html?utm_source=chatgpt.com)
        double omega = MathUtil.clamp(angularGain * angularErrorwithTargetRad, -omegaMaximum, omegaMaximum);

        if (Math.abs(angularErrorwithTargetRad) < thetaDeadbandRad) omega = 0.0;
        Logger.recordOutput("FaceAprilTag/AngularErrorRad", angularErrorwithTargetRad);
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
