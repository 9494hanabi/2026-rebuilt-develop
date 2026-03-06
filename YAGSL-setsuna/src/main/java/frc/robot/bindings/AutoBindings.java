package frc.robot.bindings;

import com.pathplanner.lib.auto.NamedCommands;
import edu.wpi.first.wpilibj2.command.Commands;
import frc.robot.RobotState;
import frc.robot.commands.auto.AutoCommand;
import frc.robot.commands.auto.autovision.AutoVisionCommand;
import frc.robot.lib.constants.AutoVisionConstants;
import frc.robot.lib.constants.FieldConstants;
import frc.robot.lib.constants.PathPlannerConstants;
import frc.robot.lib.constants.commandconstants.ShootAngleCommandConstants;
import frc.robot.subsystems.SwerveSubsystem;
import frc.robot.subsystems.mechanism.ShootAngleSubsystems;
import frc.robot.subsystems.mechanism.ShooterSubsystem;
import frc.robot.subsystems.shooter.ShooterFeedSubsystem;
import frc.robot.subsystems.vision.PieceVisionSubsystem;

// === 担当者 ===
// ハルタ
//
// Autonomous関連のバインディングとNamedCommandsを管理するクラス
// このファイルは二年生専用です。他の人は編集しないでください。
/*
 * このファイルはPathPlannerで作るイベントマーカーの名前を一致させる辞書的な場所
 * これがあることでイベントを呼ぶことができる。PathPlannerはどこでイベントを発生させるか決めるだけのツール
 * だからイベントはこっちで作らないといけない。そのイベントの動きが反映されているのがAutoCommand.java
 */

public class AutoBindings {
  private final SwerveSubsystem drivebase;
  private final RobotState robotState;
  private final ShooterSubsystem shooter;
  private final ShootAngleSubsystems shootAngle;
  private final PieceVisionSubsystem pieceVisionSubsystem;
  private final ShooterFeedSubsystem shooterFeed;

  public AutoBindings(
      SwerveSubsystem drivebase,
      RobotState robotState,
      ShooterSubsystem shooter,
      ShootAngleSubsystems shootAngle,
      PieceVisionSubsystem piecevision,
      ShooterFeedSubsystem shooterFeed) {
    this.drivebase = drivebase;
    this.robotState = robotState;
    this.shooter = shooter;
    this.shootAngle = shootAngle;
    this.pieceVisionSubsystem = piecevision;
    this.shooterFeed = shooterFeed;
  }

  /**
   * Autonomous関連のNamedCommandsを登録
   * PathPlannerで使用するコマンドをここに追加
   */
  public void configure() {
    /*
     * ==== PathPlannerイベントマーカー用コマンド登録 ====
     */

    // markStart: イベント発火確認のために開始ログを出力する
    NamedCommands.registerCommand("markStart", Commands.print("[AUTO] marker start"));

    // markFinish: イベントが終わった時の確認用ログ
    NamedCommands.registerCommand("markFinish", Commands.print("[AUTO] marker Finish"));

    /*
     * ==== ログ/安全停止 ====
     */
    // logScoreStart: シーケンス開始ログ
    NamedCommands.registerCommand("logScoreStart", AutoCommand.logMarker("score-start"));

    // logScoreEnd: シーケンス終了ログ
    NamedCommands.registerCommand("logScoreEnd", AutoCommand.logMarker("score-end"));

    // safeStopAll: ドライブとシューターを同時停止し、Auto終了時の安全を確保する
    NamedCommands.registerCommand("safeStopAll", AutoCommand.safeStopAll(drivebase, shooter));

    // pathfindToFieldCenter: フィールド中心へ経路探索して移動（デバッグ用）
    NamedCommands.registerCommand(
        "pathfindToFieldCenter",
        AutoCommand.pathfindToPose(
            drivebase,
            FieldConstants.kInitialFieldToRobotPose,
            PathPlannerConstants.kDefaultPathfindingConstraints,
            0.0));

    // pathfindThenFollowNewPath: New Pathへ接続して追従（デバッグ用）
    NamedCommands.registerCommand(
        "pathfindThenFollowNewPath",
        AutoCommand.pathfindThenFollowPath(
            drivebase, "New Path", PathPlannerConstants.kDefaultPathfindingConstraints));

    /*
     * ==== AutoVision 関連マーカー登録 ====
     */
    NamedCommands.registerCommand(
        "visionScoreModeOn",
        AutoVisionCommand.enterScoreVisionMode(
            robotState,
            AutoVisionConstants.kVisionTableName,
            AutoVisionConstants.kAprilTagPipeline,
            AutoVisionConstants.kDefaultScoreTagId));

    NamedCommands.registerCommand(
        "visionScoreModeOff", AutoVisionCommand.exitScoreVisionMode(robotState));

    NamedCommands.registerCommand(
        "visionAlignTag1p0s",
        AutoVisionCommand.alignToTagFor(
            drivebase, robotState, AutoVisionConstants.kTagAlignTimeoutSec));

    // 以下piece intakeのコード
    // Fuel が見つからない時は fallback path の `back` へ移る。
    NamedCommands.registerCommand(
        "visionAcquirePiece",
        AutoVisionCommand.acquirePieceOrFallback(drivebase, pieceVisionSubsystem));

    // 以下piece visionのコード
    // piece 用 LL4 は front-center と分離して subsystem 経由で扱う。
    NamedCommands.registerCommand(
        "visionPieceModeOn", AutoVisionCommand.enablePieceDetectorMode(pieceVisionSubsystem));

    NamedCommands.registerCommand(
        "visionAprilTagModeOn",
        AutoVisionCommand.setPipeline(
            AutoVisionConstants.kVisionTableName, AutoVisionConstants.kAprilTagPipeline));

    // 以下L4角度制御のコード
    // L4start: L4角度へ移動開始（到達待ちはしない）
    NamedCommands.registerCommand(
        "shootAngleL4Start",
        AutoCommand.shootAngleSetTargetRot(shootAngle, ShootAngleCommandConstants.kL4Rot));

    // L4end: L4角度に到達するまで待機（timeoutなし）
    NamedCommands.registerCommand("shootAngleL4End", Commands.waitUntil(shootAngle::atTarget));

    // 以下Auto shootマーカー登録のコード
    // startshootcommand: 開始直後のショットを実行する
    NamedCommands.registerCommand(
        "startshootcommand", AutoCommand.startShootWithFeedCommand(shooter, shootAngle, shooterFeed));

    // outpostshootcommand: アウトポスト用ショットを実行する
    NamedCommands.registerCommand(
        "outpostshootcommand",
        AutoCommand.outpostShootWithFeedCommand(shooter, shootAngle, shooterFeed));
  }
}
