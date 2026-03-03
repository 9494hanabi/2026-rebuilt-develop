package frc.robot.commands.auto.autovision;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj2.command.Command;
import frc.robot.commands.auto.AutoTelemetry;
import frc.robot.lib.limelight.LimelightHelpers;
import frc.robot.subsystems.SwerveSubsystem;

public class AutoIntakeAssistCommand extends Command {
  private final SwerveSubsystem drivebase;
  private final String limelightTable;
  private final int detectorPipeline;
  private final int restorePipeline;
  private final double forwardMps;
  private final double turnKpRadPerSecPerDeg;
  private final double maxOmegaRadPerSec;
  private final double centerDeadbandDeg;
  private final double nearStartAreaThreshold;
  private final double collectWindowSec;

  private boolean finished;
  private double nearStartTimestampSec;

  public AutoIntakeAssistCommand(
      SwerveSubsystem drivebase,
      String limelightTable,
      int detectorPipeline,
      int restorePipeline,
      double forwardMps,
      double turnKpRadPerSecPerDeg,
      double maxOmegaRadPerSec,
      double centerDeadbandDeg,
      double nearStartAreaThreshold,
      double collectWindowSec) {
    this.drivebase = drivebase;
    this.limelightTable = limelightTable;
    this.detectorPipeline = detectorPipeline;
    this.restorePipeline = restorePipeline;
    this.forwardMps = forwardMps;
    this.turnKpRadPerSecPerDeg = turnKpRadPerSecPerDeg;
    this.maxOmegaRadPerSec = maxOmegaRadPerSec;
    this.centerDeadbandDeg = centerDeadbandDeg;
    this.nearStartAreaThreshold = nearStartAreaThreshold;
    this.collectWindowSec = collectWindowSec;
    addRequirements(drivebase);
  }

  @Override
  public void initialize() {
    finished = false;
    nearStartTimestampSec = Double.NaN;
    LimelightHelpers.setPipelineIndex(limelightTable, detectorPipeline);
    System.out.printf(
        "[AutoVision] piece assist start table=%s pipeline=%d%n", limelightTable, detectorPipeline);

    AutoTelemetry.putState("PIECE_ASSIST_INIT");
    AutoTelemetry.putEvent("pieceAssistStart");
  }

  @Override
  public void execute() {
    boolean hasTarget = LimelightHelpers.getTV(limelightTable);
    double pipelineIndex = LimelightHelpers.getCurrentPipelineIndex(limelightTable);

    if (!hasTarget) {
      if (isCollectWindowActive()) {
        // 近距離到達後は、見失っても少し前進して回収を狙う
        drivebase.setChassisSpeeds(new ChassisSpeeds(forwardMps * 0.8, 0.0, 0.0));
        updateFinishByCollectWindow();
      } else {
        drivebase.setChassisSpeeds(new ChassisSpeeds());
      }

      AutoTelemetry.putPieceVision(
          limelightTable,
          false,
          Double.NaN,
          Double.NaN,
          pipelineIndex,
          isCollectWindowActive(),
          getNearWindowElapsedSec(),
          finished);
      return;
    }

    double txDeg = LimelightHelpers.getTX(limelightTable);
    double ta = LimelightHelpers.getTA(limelightTable);

    if (!Double.isFinite(txDeg) || !Double.isFinite(ta)) {
      drivebase.setChassisSpeeds(new ChassisSpeeds());
      AutoTelemetry.putPieceVision(
          limelightTable,
          true,
          txDeg,
          ta,
          pipelineIndex,
          isCollectWindowActive(),
          getNearWindowElapsedSec(),
          finished);
      return;
    }

    double omega =
        MathUtil.clamp(-txDeg * turnKpRadPerSecPerDeg, -maxOmegaRadPerSec, maxOmegaRadPerSec);

    double vx = Math.abs(txDeg) <= centerDeadbandDeg ? forwardMps : forwardMps * 0.35;
    drivebase.setChassisSpeeds(new ChassisSpeeds(vx, 0.0, omega));

    // 1m相当の近距離トリガー（まずはtaで近似）
    if (!isCollectWindowActive() && ta >= nearStartAreaThreshold) {
      nearStartTimestampSec = Timer.getFPGATimestamp();
      System.out.printf("[AutoVision] collect window start ta=%.3f%n", ta);
      AutoTelemetry.putEvent("pieceNearStart");
    }

    updateFinishByCollectWindow();

    AutoTelemetry.putPieceVision(
        limelightTable,
        true,
        txDeg,
        ta,
        pipelineIndex,
        isCollectWindowActive(),
        getNearWindowElapsedSec(),
        finished);
  }

  @Override
  public boolean isFinished() {
    return finished;
  }

  @Override
  public void end(boolean interrupted) {
    drivebase.setChassisSpeeds(new ChassisSpeeds());
    LimelightHelpers.setPipelineIndex(limelightTable, restorePipeline);
    System.out.printf(
        "[AutoVision] piece assist end interrupted=%b -> restore pipeline=%d%n",
        interrupted, restorePipeline);

    AutoTelemetry.putState(interrupted ? "PIECE_ASSIST_INTERRUPTED" : "PIECE_ASSIST_DONE");
    AutoTelemetry.putEvent("pieceAssistEnd");
  }

  private boolean isCollectWindowActive() {
    return Double.isFinite(nearStartTimestampSec);
  }

  private void updateFinishByCollectWindow() {
    if (!isCollectWindowActive()) {
      return;
    }
    if ((Timer.getFPGATimestamp() - nearStartTimestampSec) >= collectWindowSec) {
      finished = true;
    }
  }

  private double getNearWindowElapsedSec() {
    if (!isCollectWindowActive()) {
      return 0.0;
    }
    return Timer.getFPGATimestamp() - nearStartTimestampSec;
  }
}
