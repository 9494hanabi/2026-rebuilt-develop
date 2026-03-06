package frc.robot.commands.auto.autovision;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import frc.robot.RobotState;
import frc.robot.commands.auto.AutoCommand;
import frc.robot.lib.limelight.LimelightHelpers;
import frc.robot.subsystems.SwerveSubsystem;
import frc.robot.lib.constants.AutoVisionConstants;
import frc.robot.lib.constants.commandconstants.SetToTagCommandConstants;
import frc.robot.commands.auto.AutoTelemetry;
import frc.robot.subsystems.vision.PieceVisionSubsystem;



/*担当
 * 晴太
 */
/**
 * Autoで使うVision関連コマンドの入口。
 * ここは「小さいコマンドを組み合わせる窓口」で、重い制御本体は持たない。
 */
public final class AutoVisionCommand {
  private AutoVisionCommand() {
    throw new UnsupportedOperationException("This is a utility class!");
  }

  /**
   * Limelightのpipelineを切り替えるコマンド。
   * 例: AprilTag用(1) <-> Piece検出用(2)
   */
  public static Command setPipeline(String tableName, int pipeline) {
    return Commands.runOnce(() -> {
      LimelightHelpers.setPipelineIndex(tableName, pipeline);
      AutoTelemetry.putSelectedPipeline(pipeline);
      AutoTelemetry.putEvent("setPipeline");
      System.out.printf("[AutoVision] set pipeline table=%s pipeline=%d%n", tableName, pipeline);
    });
  }

  /**
   * Visionで使う対象タグを固定する。
   * VisionSubsystem側で「このタグを含まない推定を捨てる」ための指定になる。
   * 今はID7だけ対象にしているみたいな感じ
   */
  public static Command enableExclusiveTag(RobotState state, int tagId) {
    return Commands.runOnce(() -> {
      state.setExclusiveTag(tagId);
      System.out.printf("[AutoVision] exclusiveTag ON id=%d%n", tagId);
      AutoTelemetry.putExclusiveTag(tagId);
    });
  }

  /**
   * 対象タグの固定を解除する。
   * Auto終了時や区間終了時に呼んで、設定持ち越し事故を防ぐ。
   */
  public static Command clearExclusiveTag(RobotState state) {
    return Commands.runOnce(() -> {
      state.clearExclusiveTag();
      System.out.println("[AutoVision] exclusiveTag OFF");
      AutoTelemetry.putExclusiveTag(-1);
    });
  }

  /**
   * スコア区間に入るときの定型処理:
   * 1) AprilTag pipelineへ切替
   * 2) 対象タグを固定
   */
  public static Command enterScoreVisionMode(
      RobotState state,
      String tableName,
      int aprilTagPipeline,
      int tagId) {
    return Commands.sequence(
        setPipeline(tableName, aprilTagPipeline),
        enableExclusiveTag(state, tagId));
  }

  /**
   * スコア区間を抜けるときの定型処理:
   * タグ固定を解除
   */
  public static Command exitScoreVisionMode(RobotState state) {
    return clearExclusiveTag(state);
  }

  /**
   * 既存のSetToTagCommandをAuto向けにラップ。
   * deadlineで抜けるようにして、止まり続ける事故を防ぐ。
   */
  public static Command alignToTagFor(
      SwerveSubsystem drivebase,
      RobotState state,
      double timeoutSec) {
    return new AutoTagAlignCommand(drivebase, state, AutoVisionConstants.kDefaultScoreTagId)
        .withTimeout(timeoutSec)
        .andThen(AutoCommand.stopDrive(drivebase));
  }

    /**
   * piece detector 用 Limelight を detector pipeline に切り替える。
   * piece 用カメラは pose vision と分離して扱う。
   */
  public static Command enablePieceDetectorMode(PieceVisionSubsystem pieceVisionSubsystem) {
    return Commands.runOnce(() -> {
      pieceVisionSubsystem.setDetectorPipeline();
      AutoTelemetry.putSelectedPipeline(AutoVisionConstants.kPieceDetectorPipelineIndex);
      AutoTelemetry.putEvent("pieceDetectorModeOn");
    }, pieceVisionSubsystem);
  }

    /**
   * Fuel 回収を行い、見つからない場合は fallback path へ移る。
   * 「見る」は PieceVisionSubsystem、「動く」は AutoIntakeAssistCommand に分ける。
   */
  public static Command acquirePieceOrFallback(
      SwerveSubsystem drivebase,
      PieceVisionSubsystem pieceVisionSubsystem) {
    AutoIntakeAssistCommand acquirePieceCommand =
        new AutoIntakeAssistCommand(
            drivebase,
            pieceVisionSubsystem,
            AutoVisionConstants.kPieceForwardSpeedMps,
            AutoVisionConstants.kPieceTurnGainRadPerSecPerDeg,
            AutoVisionConstants.kPieceMaxTurnRateRadPerSec,
            AutoVisionConstants.kPieceCenteringDeadbandDeg,
            AutoVisionConstants.kPieceOffCenterForwardScale,
            AutoVisionConstants.kPieceLostTargetForwardScale,
            AutoVisionConstants.kPieceLastSeenHoldSec,
            AutoVisionConstants.kPieceCollectStartAreaThreshold,
            AutoVisionConstants.kPieceCollectWindowSec,
            AutoVisionConstants.kPieceNoTargetFallbackDelaySec);

    return Commands.sequence(
        acquirePieceCommand.withTimeout(AutoVisionConstants.kPieceAcquireTimeoutSec),
        Commands.either(
            AutoCommand.followPath(drivebase, AutoVisionConstants.kPieceFallbackPathName),
            AutoCommand.stopDrive(drivebase),
            acquirePieceCommand::shouldRunFallback));
  }
}
