package frc.robot.commands.debug.vision;

import static frc.robot.lib.util.Constants.SemiAutoConstants.angularGain;
import static frc.robot.lib.util.Constants.SemiAutoConstants.omegaMaximum;
import static frc.robot.lib.util.Constants.SemiAutoConstants.planeDeadbandMeter;
import static frc.robot.lib.util.Constants.SemiAutoConstants.thetaDeadbandRad;
import static frc.robot.lib.util.Constants.SemiAutoConstants.translationGain;
import static frc.robot.lib.util.Constants.SemiAutoConstants.velocityMaximum;
import static frc.robot.lib.util.Constants.VisionConstants.kFaceAprilTagTargetOffset;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj2.command.Command;
import frc.robot.RobotState;
import frc.robot.lib.util.Constants.FieldConstants;
import frc.robot.lib.util.Constants.VisionConstants;
import frc.robot.subsystems.SwerveSubsystem;
import java.util.Optional;

/**
 * 観測したAprilTagの絶対座標からinterestPointを作り、その点へ追従するデバッグコマンド。
 *
 * 動作概要:
 * 1) Limelightで見えているタグIDを取得
 * 2) FieldLayoutからタグの絶対座標を取り、interestPointオフセットを適用
 * 3) RobotStateの現在姿勢との差分から目標シャーシ速度を計算
 * 4) field-relative速度をrobot-relativeへ変換してSwerveへ出力
 */
public class AbsoluteDriveOKCommand extends Command {
  private static final double kTagLockHoldSeconds = 0.30;

  private final SwerveSubsystem swerve;
  private final RobotState state;
  private final DebugVisionTargetSelector targetSelector;

  private Optional<Pose2d> targetPose = Optional.empty();
  private Optional<Rotation2d> targetHeading = Optional.empty();
  private int lockedTagId = -1;
  private double lastLockSeenTimestampSec = Double.NEGATIVE_INFINITY;

  public AbsoluteDriveOKCommand(SwerveSubsystem swerve, RobotState state) {
    this(swerve, state, VisionConstants.kLimelightATableName, VisionConstants.kLimelightBTableName);
  }

  public AbsoluteDriveOKCommand(
      SwerveSubsystem swerve, RobotState state, String limelightTableNameA, String limelightTableNameB) {
    this.swerve = swerve;
    this.state = state;
    this.targetSelector = new DebugVisionTargetSelector(limelightTableNameA, limelightTableNameB);
    addRequirements(swerve);
  }

  private void clearLock() {
    lockedTagId = -1;
    lastLockSeenTimestampSec = Double.NEGATIVE_INFINITY;
    targetPose = Optional.empty();
    targetHeading = Optional.empty();
  }

  private void updateTargetFromTagId(int detectedTagId) {
    var maybeFieldToTag = FieldConstants.kAprilTagLayout.getTagPose(detectedTagId);
    if (maybeFieldToTag.isPresent()) {
      Pose2d fieldToTag = maybeFieldToTag.get().toPose2d();
      // interestPointはVisionConstants.kFaceAprilTagTargetOffset側で定義された値を使う。
      Pose2d fieldToTarget = fieldToTag.transformBy(kFaceAprilTagTargetOffset);
      targetPose = Optional.of(fieldToTarget);
      // タグの正面を向くため、タグ姿勢のYawに180度を加えた向きを目標方位にする。
      targetHeading = Optional.of(fieldToTag.getRotation().rotateBy(Rotation2d.kPi));
    } else {
      targetPose = Optional.empty();
      targetHeading = Optional.empty();
    }
  }

  private void lockTo(DebugVisionTargetSelector.TargetObservation observation, double nowSec) {
    int detectedTagId = observation.tagId();
    if (detectedTagId != lockedTagId) {
      lockedTagId = detectedTagId;
      updateTargetFromTagId(detectedTagId);
    }
    lastLockSeenTimestampSec = nowSec;
  }

  private void updateTagLock(double nowSec) {
    var sameTagObservation =
        lockedTagId < 0
            ? Optional.<DebugVisionTargetSelector.TargetObservation>empty()
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
  public void initialize() {
    // コマンド開始時に状態を初期化。
    clearLock();
  }

  @Override
  public void execute() {
    updateTagLock(Timer.getFPGATimestamp());

    if (lockedTagId < 0) {
      swerve.setChassisSpeeds(new ChassisSpeeds());
      return;
    }

    if (targetPose.isEmpty() || targetHeading.isEmpty()) {
      // 目標点が無い場合は安全停止。
      swerve.setChassisSpeeds(new ChassisSpeeds());
      return;
    }

    var latestFieldToRobot = state.getLatestFieldToRobot();
    if (latestFieldToRobot == null) {
      swerve.setChassisSpeeds(new ChassisSpeeds());
      return;
    }

    Pose2d fieldToRobot = latestFieldToRobot.getValue();
    // 3) フィールド座標系で誤差を計算する。
    Translation2d fieldError = targetPose.get().getTranslation().minus(fieldToRobot.getTranslation());

    double velocityX =
        MathUtil.clamp(translationGain * fieldError.getX(), -velocityMaximum, velocityMaximum);
    double velocityY =
        MathUtil.clamp(translationGain * fieldError.getY(), -velocityMaximum, velocityMaximum);

    if (Math.abs(fieldError.getX()) < planeDeadbandMeter) {
      velocityX = 0.0;
    }
    if (Math.abs(fieldError.getY()) < planeDeadbandMeter) {
      velocityY = 0.0;
    }

    // 進行方向ではなく、常にタグ正面を向く角度誤差を使う。
    double angularErrorRad =
        MathUtil.angleModulus(targetHeading.get().minus(fieldToRobot.getRotation()).getRadians());

    double omega = MathUtil.clamp(angularGain * angularErrorRad, -omegaMaximum, omegaMaximum);
    if (Math.abs(angularErrorRad) < thetaDeadbandRad) {
      omega = 0.0;
    }

    ChassisSpeeds cmd =
        // 4) field-relative指令をrobot-relativeへ変換して出力する。
        ChassisSpeeds.fromFieldRelativeSpeeds(
            velocityX, velocityY, omega, fieldToRobot.getRotation());
    swerve.setChassisSpeeds(cmd);
  }

  @Override
  public void end(boolean interrupted) {
    swerve.setChassisSpeeds(new ChassisSpeeds());
  }
}
