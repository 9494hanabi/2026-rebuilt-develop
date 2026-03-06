package frc.robot.commands.debug.odmetry;

import static frc.robot.lib.constants.LogConstants.kStatusLogPeriodSec;
import static frc.robot.lib.constants.PIDConstants.kHeadingIntegralContributionLimit;
import static frc.robot.lib.constants.PIDConstants.kHeadingKd;
import static frc.robot.lib.constants.PIDConstants.kHeadingKi;
import static frc.robot.lib.constants.PIDConstants.kHeadingKp;
import static frc.robot.lib.constants.PIDConstants.kHeadingToleranceRad;
import static frc.robot.lib.constants.PIDConstants.kHeadingVelocityToleranceRadPerSec;
import static frc.robot.lib.constants.SemiAutoConstants.omegaMaximum;

import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj2.command.Command;
import frc.robot.RobotState;
import frc.robot.lib.constants.FieldConstants;
import frc.robot.lib.util.OdomHeadingController;
import frc.robot.subsystems.SwerveSubsystem;
import java.util.function.Supplier;

// ジョイスティックの並進操作を有効のまま、フィールド中心を向き続けるコマンド。
public class FaceFieldCenterWhileDriveCommand extends Command {

  private static final Translation2d FIELD_CENTER =
      new Translation2d(FieldConstants.fieldLengthMeter / 2.0, FieldConstants.fieldWidthMeter / 2.0);

  // この機体では姿勢基準が前後反転しているため、中心方向に対して180deg補正して前面を向ける。
  private static final Rotation2d kRobotFrontHeadingOffset = Rotation2d.fromDegrees(180.0);

  private final SwerveSubsystem swerve;
  private final RobotState state;
  private final Supplier<ChassisSpeeds> driveSpeedsSupplier;
  private final PIDController headingPid = new PIDController(kHeadingKp, kHeadingKi, kHeadingKd);
  private final OdomHeadingController headingControl;
  private double lastStatusLogSec = Double.NEGATIVE_INFINITY;

  public FaceFieldCenterWhileDriveCommand(
      SwerveSubsystem swerve, RobotState state, Supplier<ChassisSpeeds> driveSpeedsSupplier) {
    this.swerve = swerve;
    this.state = state;
    this.driveSpeedsSupplier = driveSpeedsSupplier;
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
    System.out.println("[FaceFieldCenterWhileDrive] INITIALIZED");
  }

  @Override
  public void execute() {
    double nowSec = Timer.getFPGATimestamp();
    ChassisSpeeds requested = driveSpeedsSupplier.get();

    if (requested == null
        || !Double.isFinite(requested.vxMetersPerSecond)
        || !Double.isFinite(requested.vyMetersPerSecond)
        || !Double.isFinite(requested.omegaRadiansPerSecond)) {
      requested = new ChassisSpeeds();
    }

    double vx = requested.vxMetersPerSecond;
    double vy = requested.vyMetersPerSecond;

    var latest = state.getLatestFieldToRobotOdom();
    if (latest == null) {
      if (nowSec - lastStatusLogSec >= kStatusLogPeriodSec) {
        lastStatusLogSec = nowSec;
        System.out.println("[FaceFieldCenterWhileDrive] !!ODOM POSE IS NULL!!");
      }
      headingPid.reset();
      swerve.driveFieldOriented(new ChassisSpeeds(vx, vy, requested.omegaRadiansPerSecond));
      return;
    }

    Pose2d robotPose = latest.getValue();
    Translation2d robotToCenter = FIELD_CENTER.minus(robotPose.getTranslation());
    if (robotToCenter.getNorm() < 0.05) {
      swerve.driveFieldOriented(new ChassisSpeeds(vx, vy, 0.0));
      return;
    }

    double targetHeadingRad = robotToCenter.getAngle().plus(kRobotFrontHeadingOffset).getRadians();
    double currentHeadingRad = robotPose.getRotation().getRadians();
    var maybeResult = headingControl.calculate(currentHeadingRad, targetHeadingRad);
    if (maybeResult.isEmpty()) {
      if (nowSec - lastStatusLogSec >= kStatusLogPeriodSec) {
        lastStatusLogSec = nowSec;
        System.out.println("[FaceFieldCenterWhileDrive] !!INVALID!!");
      }
      headingControl.reset();
      swerve.driveFieldOriented(new ChassisSpeeds(vx, vy, requested.omegaRadiansPerSecond));
      return;
    }

    var result = maybeResult.get();
    double omega = result.omegaRadPerSec();
    if (!Double.isFinite(omega)) {
      if (nowSec - lastStatusLogSec >= kStatusLogPeriodSec) {
        lastStatusLogSec = nowSec;
        System.out.printf("[FaceFieldCenterWhileDrive] !!INVALID omega=%.4f!!%n", omega);
      }
      headingControl.reset();
      swerve.driveFieldOriented(new ChassisSpeeds(vx, vy, requested.omegaRadiansPerSecond));
      return;
    }

    if (nowSec - lastStatusLogSec >= kStatusLogPeriodSec) {
      lastStatusLogSec = nowSec;
      System.out.printf(
          "[FaceFieldCenterWhileDrive] target=%.2fdeg current=%.2fdeg omega=%.4f atSetpoint=%b%n",
          Math.toDegrees(targetHeadingRad),
          Math.toDegrees(result.wrappedCurrentHeadingRad()),
          omega,
          result.atSetpoint());
    }

    swerve.driveFieldOriented(new ChassisSpeeds(vx, vy, omega));
  }

  @Override
  public void end(boolean interrupted) {
    headingControl.reset();
    swerve.driveFieldOriented(new ChassisSpeeds());
    System.out.println("[FaceFieldCenterWhileDrive] ended, interrupted=" + interrupted);
  }
}
