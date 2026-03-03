package frc.robot.commands.auto.autovision;

import static frc.robot.lib.constants.PIDConstants.kHeadingKd;
import static frc.robot.lib.constants.PIDConstants.kHeadingKi;
import static frc.robot.lib.constants.PIDConstants.kHeadingKp;
import static frc.robot.lib.constants.PIDConstants.kHeadingToleranceRad;
import static frc.robot.lib.constants.PIDConstants.kHeadingVelocityToleranceRadPerSec;
import static frc.robot.lib.constants.PIDConstants.kHeadingIntegralContributionLimit;
import static frc.robot.lib.constants.PIDConstants.kTranslationKd;
import static frc.robot.lib.constants.PIDConstants.kTranslationKi;
import static frc.robot.lib.constants.PIDConstants.kTranslationKp;
import static frc.robot.lib.constants.PIDConstants.kTranslationToleranceMeters;
import static frc.robot.lib.constants.PIDConstants.kTranslationVelocityToleranceMPerSec;
import static frc.robot.lib.constants.PIDConstants.kTranslationIntegralContributionLimit;
import static frc.robot.lib.constants.SemiAutoConstants.omegaMaximum;
import static frc.robot.lib.constants.SemiAutoConstants.velocityMaximum;

import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj2.command.Command;
import frc.robot.RobotState;
import frc.robot.lib.constants.AutoVisionConstants;
import frc.robot.lib.constants.commandconstants.SetToTagCommandConstants;
import frc.robot.lib.util.OdomHeadingController;
import frc.robot.lib.util.OdomTranslationController;
import frc.robot.subsystems.SwerveSubsystem;

/**
 * Auto専用の固定タグAlignコマンド。
 * SetToTagCommandConstantsの目標Poseを使い、到達判定で終了する。
 */
public class AutoTagAlignCommand extends Command {
  private final SwerveSubsystem swerve;
  private final RobotState state;
  private final int tagId;

  private final PIDController translationPidX =
      new PIDController(kTranslationKp, kTranslationKi, kTranslationKd);
  private final PIDController translationPidY =
      new PIDController(kTranslationKp, kTranslationKi, kTranslationKd);
  private final PIDController headingPid =
      new PIDController(kHeadingKp, kHeadingKi, kHeadingKd);

  private final OdomTranslationController translationControl;
  private final OdomHeadingController headingControl;

  private Pose2d targetPose;
  private boolean finished;
  private double atGoalSinceSec = Double.NaN;

  public AutoTagAlignCommand(SwerveSubsystem swerve, RobotState state, int tagId) {
    this.swerve = swerve;
    this.state = state;
    this.tagId = tagId;
    addRequirements(swerve);

    translationPidX.setTolerance(kTranslationToleranceMeters, kTranslationVelocityToleranceMPerSec);
    translationPidY.setTolerance(kTranslationToleranceMeters, kTranslationVelocityToleranceMPerSec);
    translationPidX.setIntegratorRange(
        -kTranslationIntegralContributionLimit, kTranslationIntegralContributionLimit);
    translationPidY.setIntegratorRange(
        -kTranslationIntegralContributionLimit, kTranslationIntegralContributionLimit);

    headingPid.disableContinuousInput();
    headingPid.setTolerance(kHeadingToleranceRad, kHeadingVelocityToleranceRadPerSec);
    headingPid.setIntegratorRange(
        -kHeadingIntegralContributionLimit, kHeadingIntegralContributionLimit);

    translationControl = new OdomTranslationController(translationPidX, translationPidY, velocityMaximum);
    headingControl = new OdomHeadingController(headingPid, omegaMaximum);
  }

  @Override
  public void initialize() {
    finished = false;
    atGoalSinceSec = Double.NaN;
    translationControl.reset();
    headingControl.reset();

    state.setExclusiveTag(tagId);

    Pose2d baseTarget = SetToTagCommandConstants.tagToVertexMap.get(tagId);
    if (baseTarget == null) {
      finished = true;
      System.out.printf("[AutoTagAlign] target not found for tag=%d%n", tagId);
      return;
    }

    targetPose =
        new Pose2d(
            baseTarget.getTranslation(),
            baseTarget
                .getRotation()
                .plus(Rotation2d.fromDegrees(AutoVisionConstants.kTagAlignHeadingOffsetDeg)));

    System.out.printf(
        "[AutoTagAlign] init tag=%d target=(%.2f, %.2f, %.1fdeg)%n",
        tagId, targetPose.getX(), targetPose.getY(), targetPose.getRotation().getDegrees());
  }

  @Override
  public void execute() {
    if (finished || targetPose == null) {
      swerve.driveFieldOriented(new ChassisSpeeds());
      return;
    }

    var latest = state.getLatestFieldToRobotOdom();
    if (latest == null || latest.getValue() == null) {
      swerve.driveFieldOriented(new ChassisSpeeds());
      return;
    }

    Pose2d current = latest.getValue();
    if (!isFinitePose(current)) {
      swerve.driveFieldOriented(new ChassisSpeeds());
      return;
    }

    var maybeTrans =
        translationControl.calculate(
            current.getX(), current.getY(), targetPose.getX(), targetPose.getY());
    var maybeHead =
        headingControl.calculate(
            current.getRotation().getRadians(), targetPose.getRotation().getRadians());

    if (maybeTrans.isEmpty() || maybeHead.isEmpty()) {
      swerve.driveFieldOriented(new ChassisSpeeds());
      return;
    }

    var trans = maybeTrans.get();
    var head = maybeHead.get();

    double vx = trans.translationXMeterPerSec();
    double vy = trans.translationYMeterPerSec();
    double omega = head.omegaRadPerSec();

    boolean atTranslation = trans.xAtSetpoint() && trans.yAtSetpoint();
    boolean atHeading = head.atSetpoint();

    if (atTranslation && atHeading) {
      double nowSec = Timer.getFPGATimestamp();
      if (!Double.isFinite(atGoalSinceSec)) {
        atGoalSinceSec = nowSec;
      } else if ((nowSec - atGoalSinceSec) >= AutoVisionConstants.kTagAlignSettleSec) {
        finished = true;
      }
      vx = 0.0;
      vy = 0.0;
      omega = 0.0;
    } else {
      atGoalSinceSec = Double.NaN;
    }

    swerve.driveFieldOriented(new ChassisSpeeds(vx, vy, omega));
  }

  @Override
  public boolean isFinished() {
    return finished;
  }

  @Override
  public void end(boolean interrupted) {
    swerve.driveFieldOriented(new ChassisSpeeds());
    System.out.printf("[AutoTagAlign] end interrupted=%b%n", interrupted);
  }

  private static boolean isFinitePose(Pose2d pose) {
    return pose != null
        && Double.isFinite(pose.getX())
        && Double.isFinite(pose.getY())
        && Double.isFinite(pose.getRotation().getRadians());
  }
}
