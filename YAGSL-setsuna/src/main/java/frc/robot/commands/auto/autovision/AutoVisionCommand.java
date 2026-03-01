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
   * フェルール回収補助をAuto向けにラップ。
   * AutoIntakeAssistCommand本体にパラメータを渡し、
   * timeout後に確実に停止させる。
   */
  public static Command acquirePieceWithTimeout(
      SwerveSubsystem drivebase,
      String tableName,
      int detectorPipeline,
      int restorePipeline,
      double forwardMps,
      double turnKpRadPerSecPerDeg,
      double maxOmegaRadPerSec,
      double centerDeadbandDeg,
      double nearStartAreaThreshold,
      double collectWindowSec,
      double timeoutSec) {
    return new AutoIntakeAssistCommand(
            drivebase,
            tableName,
            detectorPipeline,
            restorePipeline,
            forwardMps,
            turnKpRadPerSecPerDeg,
            maxOmegaRadPerSec,
            centerDeadbandDeg,
            nearStartAreaThreshold,
            collectWindowSec)
        .withTimeout(timeoutSec)
        .andThen(AutoCommand.stopDrive(drivebase));
  }
}
