package frc.robot.commands.auto.autovision;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj2.command.Command;
import frc.robot.commands.auto.AutoTelemetry;
import frc.robot.lib.constants.AutoVisionConstants;
import frc.robot.lib.limelight.LimelightHelpers;
import frc.robot.subsystems.SwerveSubsystem;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import frc.robot.subsystems.vision.PieceVisionSubsystem;
import frc.robot.subsystems.vision.PieceVisionSubsystem.ClusterTarget;
import java.util.Optional;


/*
 * 担当
 * 晴太
 */

public class AutoIntakeAssistCommand extends Command {
    private final SwerveSubsystem drivebase;
  private final PieceVisionSubsystem pieceVisionSubsystem;
  private final double forwardSpeedMps;
  private final double turnGainRadPerSecPerDeg;
  private final double maxTurnRateRadPerSec;
  private final double centeringDeadbandDeg;
  private final double offCenterForwardScale;
  private final double lostTargetForwardScale;
  private final double lastSeenHoldSec;
  private final double collectStartAreaThreshold;
  private final double collectWindowSec;
  private final double noTargetFallbackDelaySec;

  private boolean finished;
  private boolean shouldRunFallback;
  private double collectStartTimestampSec;
  private double noTargetSinceTimestampSec;

  public AutoIntakeAssistCommand(
      SwerveSubsystem drivebase,
      PieceVisionSubsystem pieceVisionSubsystem,
      double forwardSpeedMps,
      double turnGainRadPerSecPerDeg,
      double maxTurnRateRadPerSec,
      double centeringDeadbandDeg,
      double offCenterForwardScale,
      double lostTargetForwardScale,
      double lastSeenHoldSec,
      double collectStartAreaThreshold,
      double collectWindowSec,
      double noTargetFallbackDelaySec) {
    this.drivebase = drivebase;
    this.pieceVisionSubsystem = pieceVisionSubsystem;
    this.forwardSpeedMps = forwardSpeedMps;
    this.turnGainRadPerSecPerDeg = turnGainRadPerSecPerDeg;
    this.maxTurnRateRadPerSec = maxTurnRateRadPerSec;
    this.centeringDeadbandDeg = centeringDeadbandDeg;
    this.offCenterForwardScale = offCenterForwardScale;
    this.lostTargetForwardScale = lostTargetForwardScale;
    this.lastSeenHoldSec = lastSeenHoldSec;
    this.collectStartAreaThreshold = collectStartAreaThreshold;
    this.collectWindowSec = collectWindowSec;
    this.noTargetFallbackDelaySec = noTargetFallbackDelaySec;
    addRequirements(drivebase, pieceVisionSubsystem);
  }

  @Override
  public void initialize() {
    finished = false;
    shouldRunFallback = false;
    collectStartTimestampSec = Double.NaN;
    noTargetSinceTimestampSec = Timer.getFPGATimestamp();

    pieceVisionSubsystem.clearLastSeen();
    pieceVisionSubsystem.setDetectorPipeline();

    AutoTelemetry.putState("PIECE_ASSIST_INIT");
    AutoTelemetry.putEvent("pieceAssistStart");
  }

    @Override
  public void execute() {
    Optional<ClusterTarget> currentCluster = pieceVisionSubsystem.getBestCluster();
    double pipelineIndex = pieceVisionSubsystem.getCurrentPipelineIndex();

    if (currentCluster.isPresent()) {
      ClusterTarget activeCluster = currentCluster.get();
      noTargetSinceTimestampSec = Double.NaN;

      double forwardScale =
          Math.abs(activeCluster.centerTxDeg()) <= centeringDeadbandDeg
              ? 1.0
              : offCenterForwardScale;

      driveTowardTx(activeCluster.centerTxDeg(), forwardScale);

      if (!isCollectWindowActive() && activeCluster.totalArea() >= collectStartAreaThreshold) {
        collectStartTimestampSec = Timer.getFPGATimestamp();
        AutoTelemetry.putEvent("pieceCollectWindowStart");
      }
    } else {
      startNoTargetTimerIfNeeded();

      if (isCollectWindowActive()) {
        // 以下collect windowのコード
        // 最後に少し押し込んで、センサー未着の期間でも回収成功率を上げる。
        driveStraight(lostTargetForwardScale);
      } else if (shouldUseLastSeenHold()) {
        double lastSeenTxDeg =
            pieceVisionSubsystem.getLastSeenCluster().map(ClusterTarget::centerTxDeg).orElse(0.0);
        driveTowardTx(lastSeenTxDeg, lostTargetForwardScale);
      } else {
        drivebase.setChassisSpeeds(new ChassisSpeeds());
      }

      if (!isCollectWindowActive() && hasExceededNoTargetFallbackDelay()) {
        shouldRunFallback = true;
        finished = true;
        AutoTelemetry.putEvent("pieceFallbackRequested");
      }
    }

    updateFinishByCollectWindow();

    AutoTelemetry.putPieceVision(
        AutoVisionConstants.kPieceCameraTableName,
        currentCluster.isPresent(),
        currentCluster.map(ClusterTarget::centerTxDeg).orElse(Double.NaN),
        currentCluster.map(ClusterTarget::totalArea).orElse(Double.NaN),
        pipelineIndex,
        isCollectWindowActive(),
        getCollectWindowElapsedSec(),
        finished);

    SmartDashboard.putBoolean("Auto/Vision/Piece/ShouldRunFallback", shouldRunFallback);
    SmartDashboard.putNumber(
        "Auto/Vision/Piece/NoTargetElapsedSec",
        getNoTargetElapsedSec());
  }

  @Override
  public boolean isFinished() {
    return finished;
  }

    @Override
  public void end(boolean interrupted) {
    drivebase.setChassisSpeeds(new ChassisSpeeds());
    pieceVisionSubsystem.setIdlePipeline();

    AutoTelemetry.putState(interrupted ? "PIECE_ASSIST_INTERRUPTED" : "PIECE_ASSIST_DONE");
    AutoTelemetry.putEvent("pieceAssistEnd");
  }

    public boolean shouldRunFallback() {
    return shouldRunFallback;
  }

  // 以下drive出力のコード
  // cluster の中心 tx に向けて旋回しながら前進する。
  private void driveTowardTx(double targetTxDeg, double forwardScale) {
    double omega =
        MathUtil.clamp(
            -targetTxDeg * turnGainRadPerSecPerDeg,
            -maxTurnRateRadPerSec,
            maxTurnRateRadPerSec);

    double vx = MathUtil.clamp(forwardSpeedMps * forwardScale, 0.0, forwardSpeedMps);
    drivebase.setChassisSpeeds(new ChassisSpeeds(vx, 0.0, omega));
  }

  private void driveStraight(double forwardScale) {
    double vx = MathUtil.clamp(forwardSpeedMps * forwardScale, 0.0, forwardSpeedMps);
    drivebase.setChassisSpeeds(new ChassisSpeeds(vx, 0.0, 0.0));
  }

  private boolean isCollectWindowActive() {
    return Double.isFinite(collectStartTimestampSec);
  }

  private void updateFinishByCollectWindow() {
    if (!isCollectWindowActive()) {
      return;
    }

    if ((Timer.getFPGATimestamp() - collectStartTimestampSec) >= collectWindowSec) {
      finished = true;
    }
  }

  private double getCollectWindowElapsedSec() {
    if (!isCollectWindowActive()) {
      return 0.0;
    }
    return Timer.getFPGATimestamp() - collectStartTimestampSec;
  }

  private void startNoTargetTimerIfNeeded() {
    if (!Double.isFinite(noTargetSinceTimestampSec)) {
      noTargetSinceTimestampSec = Timer.getFPGATimestamp();
    }
  }

  private boolean hasExceededNoTargetFallbackDelay() {
    return getNoTargetElapsedSec() >= noTargetFallbackDelaySec;
  }

  private double getNoTargetElapsedSec() {
    if (!Double.isFinite(noTargetSinceTimestampSec)) {
      return 0.0;
    }
    return Timer.getFPGATimestamp() - noTargetSinceTimestampSec;
  }

  private boolean shouldUseLastSeenHold() {
    return pieceVisionSubsystem.getLastSeenCluster().isPresent()
        && pieceVisionSubsystem.getLastSeenAgeSec() <= lastSeenHoldSec;
  }

}
