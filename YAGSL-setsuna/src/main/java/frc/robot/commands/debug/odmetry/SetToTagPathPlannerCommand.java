package frc.robot.commands.debug.odmetry;

import static frc.robot.lib.constants.LogConstants.kSearchLogPeriodSec;
import static frc.robot.lib.constants.LogConstants.kStatusLogPeriodSec;
import static frc.robot.lib.constants.LogConstants.kWarningLogPeriodSec;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.CommandScheduler;
import frc.robot.RobotState;
import frc.robot.lib.constants.PathPlannerConstants;
import frc.robot.lib.constants.commandconstants.SetToTagCommandConstants;
import frc.robot.lib.limelight.LimelightConfig;
import frc.robot.lib.limelight.VisionTargetSelector;
import frc.robot.subsystems.SwerveSubsystem;

public class SetToTagPathPlannerCommand extends Command {
  private final SwerveSubsystem swerve;
  private final RobotState state;
  private final VisionTargetSelector targetSelector;

  private int lockedTagId = -1;
  private Pose2d targetPose = null;
  private String lockedSourceTable = "";
  private double lockedSourceArea = Double.NaN;
  private double lockAcquiredTimestampSec = Double.NEGATIVE_INFINITY;
  private double lastStatusLogSec = Double.NEGATIVE_INFINITY;
  private double lastSearchLogSec = Double.NEGATIVE_INFINITY;
  private double lastWarningLogSec = Double.NEGATIVE_INFINITY;
  private Command activePathfindCommand = null;
  private boolean pathStarted = false;
  private boolean pathCompleted = false;

  public SetToTagPathPlannerCommand(SwerveSubsystem swerve, RobotState state) {
    this.swerve = swerve;
    this.state = state;
    this.targetSelector = new VisionTargetSelector(getEnabledVisionTables());
  }

  @Override
  public void initialize() {
    lockedTagId = -1;
    targetPose = null;
    lockedSourceTable = "";
    lockedSourceArea = Double.NaN;
    lockAcquiredTimestampSec = Double.NEGATIVE_INFINITY;
    lastStatusLogSec = Double.NEGATIVE_INFINITY;
    lastSearchLogSec = Double.NEGATIVE_INFINITY;
    lastWarningLogSec = Double.NEGATIVE_INFINITY;
    pathStarted = false;
    pathCompleted = false;

    if (activePathfindCommand != null && activePathfindCommand.isScheduled()) {
      activePathfindCommand.cancel();
    }
    activePathfindCommand = null;

    state.clearExclusiveTag();
    System.out.println("[SetToTagPathPlanner] INITIALIZED: searching for tag...");
  }

  @Override
  public void execute() {
    double nowSec = Timer.getFPGATimestamp();

    if (!pathStarted) {
      var best = targetSelector.selectBestObservation();
      if (best.isEmpty()) {
        if (nowSec - lastSearchLogSec >= kSearchLogPeriodSec) {
          lastSearchLogSec = nowSec;
          System.out.println("[SetToTagPathPlanner] searching... (no valid observation)");
        }
        return;
      }

      var bestObservation = best.get();
      int tagId = bestObservation.tagId();
      Pose2d resolvedTarget = resolveTargetPose(tagId);
      if (resolvedTarget == null) {
        if (nowSec - lastWarningLogSec >= kWarningLogPeriodSec) {
          lastWarningLogSec = nowSec;
          System.out.printf(
              "[SetToTagPathPlanner] !!TAG ID=%d HAS NO VALID TARGET IGNORING!!%n",
              tagId);
          System.out.printf(
              "                         (src=%s ta=%.2f)%n",
              bestObservation.tableName(),
              bestObservation.area());
        }
        return;
      }

      lockedTagId = tagId;
      targetPose = resolvedTarget;
      lockedSourceTable = bestObservation.tableName();
      lockedSourceArea = bestObservation.area();
      lockAcquiredTimestampSec = nowSec;
      state.setExclusiveTag(lockedTagId);

      activePathfindCommand =
          swerve.pathfindToPose(
              targetPose,
              PathPlannerConstants.kDefaultPathfindingConstraints,
              0.0);
      CommandScheduler.getInstance().schedule(activePathfindCommand);
      pathStarted = true;
      pathCompleted = false;

      System.out.println("[SetToTagPathPlanner] TAG LOCKED / PATHFIND STARTED");
      System.out.printf(
          "                       tagId=%d src=%s ta=%.2f target=%s%n",
          lockedTagId,
          lockedSourceTable,
          lockedSourceArea,
          formatPose(targetPose));
      return;
    }

    if (lockedTagId >= 0) {
      var currentObservation = targetSelector.observationForTag(lockedTagId);
      if (currentObservation.isPresent()) {
        lockedSourceTable = currentObservation.get().tableName();
        lockedSourceArea = currentObservation.get().area();
      }
    }

    if (activePathfindCommand == null) {
      if (nowSec - lastWarningLogSec >= kWarningLogPeriodSec) {
        lastWarningLogSec = nowSec;
        System.out.println("[SetToTagPathPlanner] !!PATH COMMAND IS NULL!!");
      }
      return;
    }

    boolean pathRunning = activePathfindCommand.isScheduled();
    if (!pathRunning && !pathCompleted) {
      pathCompleted = true;
      swerve.driveFieldOriented(new ChassisSpeeds());
      System.out.println("[SetToTagPathPlanner] PATH COMPLETED");
    }

    if (nowSec - lastStatusLogSec >= kStatusLogPeriodSec) {
      lastStatusLogSec = nowSec;
      double lockAgeSec =
          lockAcquiredTimestampSec > 0.0 ? (nowSec - lockAcquiredTimestampSec) : Double.NaN;
      Pose2d currentPose = swerve.getSwerveDrive().getPose();
      System.out.printf(
          "[SetToTagPathPlanner] tag=%d age=%.2fs src=%s ta=%.2f running=%b completed=%b "
              + "current=%s target=%s%n",
          lockedTagId,
          lockAgeSec,
          lockedSourceTable,
          lockedSourceArea,
          pathRunning,
          pathCompleted,
          formatPose(currentPose),
          formatPose(targetPose));
    }
  }

  @Override
  public void end(boolean interrupted) {
    if (activePathfindCommand != null && activePathfindCommand.isScheduled()) {
      activePathfindCommand.cancel();
    }
    state.clearExclusiveTag();
    swerve.driveFieldOriented(new ChassisSpeeds());
    System.out.printf(
        "[SetToTagPathPlanner] ended lockedTag=%d src=%s ta=%.2f interrupted=%b%n",
        lockedTagId,
        lockedSourceTable,
        lockedSourceArea,
        interrupted);
  }

  @Override
  public boolean isFinished() {
    return false;
  }

  private static String[] getEnabledVisionTables() {
    return LimelightConfig.getInstance().getEnabledTableNames();
  }

  private static Pose2d resolveTargetPose(int tagId) {
    Pose2d pose = SetToTagCommandConstants.tagToVertexMap.get(tagId);
    if (isFinitePose(pose)) {
      return pose;
    }
    return null;
  }

  private static boolean isFinitePose(Pose2d pose) {
    return pose != null
        && Double.isFinite(pose.getX())
        && Double.isFinite(pose.getY())
        && Double.isFinite(pose.getRotation().getRadians());
  }

  private static String formatPose(Pose2d pose) {
    if (pose == null) {
      return "null";
    }
    return String.format(
        "(%.2f, %.2f, %.1f deg)",
        pose.getX(),
        pose.getY(),
        pose.getRotation().getDegrees());
  }
}
