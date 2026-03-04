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
import frc.robot.subsystems.mechanism.ShootAngleSubsystems;
import frc.robot.subsystems.mechanism.ShooterSubsystem;
import frc.robot.subsystems.SwerveSubsystem;

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
    // 仮値: 実機完成後に距離別に最適化する
    private static final double kShooterTestTargetRps = ShooterSubsystem.kNominalShotRps;
    private static final double kShooterReadyTimeoutSec = 1.0;
    private static final double kShooterShootWindowSec = 2.0;

    // Angle調整のtimeout
    private static final double kShootAngleReadyTimeoutSec = 0.5;

    public AutoBindings(
        SwerveSubsystem drivebase,
        RobotState robotState,
        ShooterSubsystem shooter,
        ShootAngleSubsystems shootAngle) {
        this.drivebase = drivebase;
        this.robotState = robotState;
        this.shooter = shooter;
        this.shootAngle = shootAngle;
    }

    /**
     * Autonomous関連のNamedCommandsを登録
     * PathPlannerで使用するコマンドをここに追加
     */
    public void configure() {
        /*
         * ==== Shooter 関連マーカー登録 ====
         */
        // shooterSetTarget80Rps: 目標80RPSへスピンアップ開始
        NamedCommands.registerCommand(
            "shooterSetTarget80Rps",
            AutoCommand.shooterSetTargetRps(shooter, kShooterTestTargetRps)
        );

        // shooterWaitReady1p0s: Ready成立まで最大1.0秒待機
        NamedCommands.registerCommand(
            "shooterWaitReady1p0s",
            AutoCommand.shooterWaitReady(shooter, kShooterReadyTimeoutSec)
        );

        // shooterSpinUpAndWait80Rps: スピンアップ＋Ready待ちを1コマンド化
        NamedCommands.registerCommand(
            "shooterSpinUpAndWait80Rps",
            AutoCommand.shooterSpinUpAndWaitReady(shooter, kShooterTestTargetRps, kShooterReadyTimeoutSec)
        );

        // shooterStop: シューター停止
        NamedCommands.registerCommand(
            "shooterStop",
            AutoCommand.shooterStop(shooter)
        );

                // shooterShoot2p0s: スピンアップ→Ready待ち→2.0秒シュート→停止（ログ付き）
        NamedCommands.registerCommand(
            "shooterShoot2p0s",
            AutoCommand.shooterShootAtRpsForWithLogs(
                shooter,
                kShooterTestTargetRps,
                kShooterReadyTimeoutSec,
                kShooterShootWindowSec)
        );

        // shooterZoneSpin: PathPlannerのZoned Event中だけシューターを回す
        NamedCommands.registerCommand(
            "shooterZoneSpin",
            AutoCommand.shooterZoneSpin(shooter, kShooterTestTargetRps)
        );

        NamedCommands.registerCommand(
            "shootAngleSetTag7",
            AutoCommand.shootAngleSetTargetRot(shootAngle, ShootAngleCommandConstants.kAutoTag7Rot));

        NamedCommands.registerCommand(
            "shootAngleWaitReady0p5s",
            AutoCommand.shootAngleWaitAtTarget(shootAngle, kShootAngleReadyTimeoutSec));

        NamedCommands.registerCommand(
            "shootAngleSetAndWaitTag7",
            AutoCommand.shootAngleSetAndWaitRot(
                shootAngle,
                ShootAngleCommandConstants.kAutoTag7Rot,
                kShootAngleReadyTimeoutSec));



        /*
         * ==== PathPlannerイベントマーカー用コマンド登録 ====
         */

        // markStart: イベント発火確認のために開始ログを出力する
        NamedCommands.registerCommand(
            "markStart",
            Commands.print("[AUTO] marker start")
        );

        // markFinish: イベントが終わった時の確認用ログ
        NamedCommands.registerCommand(
            "markFinish",
            Commands.print("[AUTO] marker Finish")            
        );

        /*
         * ==== ログ/安全停止 ====
         */
        // logScoreStart: シーケンス開始ログ
        NamedCommands.registerCommand(
            "logScoreStart",
            AutoCommand.logMarker("score-start")
        );

        // logScoreEnd: シーケンス終了ログ
        NamedCommands.registerCommand(
            "logScoreEnd",
            AutoCommand.logMarker("score-end")
        );

        // safeStopAll: ドライブとシューターを同時停止し、Auto終了時の安全を確保する
        NamedCommands.registerCommand(
            "safeStopAll",
            AutoCommand.safeStopAll(drivebase, shooter)
        );

        // pathfindToFieldCenter: フィールド中心へ経路探索して移動（デバッグ用）
        NamedCommands.registerCommand(
            "pathfindToFieldCenter",
            AutoCommand.pathfindToPose(
                drivebase,
                FieldConstants.kInitialFieldToRobotPose,
                PathPlannerConstants.kDefaultPathfindingConstraints,
                0.0)
        );

        // pathfindThenFollowNewPath: New Pathへ接続して追従（デバッグ用）
        NamedCommands.registerCommand(
            "pathfindThenFollowNewPath",
            AutoCommand.pathfindThenFollowPath(
                drivebase,
                "New Path",
                PathPlannerConstants.kDefaultPathfindingConstraints)
        );

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
            "visionScoreModeOff",
            AutoVisionCommand.exitScoreVisionMode(robotState));

        NamedCommands.registerCommand(
            "visionAlignTag1p0s",
            AutoVisionCommand.alignToTagFor(
                drivebase,
                robotState,
                AutoVisionConstants.kTagAlignTimeoutSec));

        NamedCommands.registerCommand(
            "visionPieceModeOn",
            AutoVisionCommand.setPipeline(
                AutoVisionConstants.kVisionTableName,
                AutoVisionConstants.kPieceDetectorPipeline));

        NamedCommands.registerCommand(
            "visionAcquirePiece",
            AutoVisionCommand.acquirePieceWithTimeout(
                drivebase,
                AutoVisionConstants.kVisionTableName,
                AutoVisionConstants.kPieceDetectorPipeline,
                AutoVisionConstants.kAprilTagPipeline,
                AutoVisionConstants.kPieceForwardMps,
                AutoVisionConstants.kPieceTurnKpRadPerSecPerDeg,
                AutoVisionConstants.kPieceMaxOmegaRadPerSec,
                AutoVisionConstants.kPieceCenterDeadbandDeg,
                AutoVisionConstants.kPieceNearStartAreaThreshold,
                AutoVisionConstants.kPieceCollectWindowSec,
                AutoVisionConstants.kPieceAcquireTimeoutSec));


        NamedCommands.registerCommand(
            "visionAprilTagModeOn",
            AutoVisionCommand.setPipeline(
                AutoVisionConstants.kVisionTableName,
                AutoVisionConstants.kAprilTagPipeline));
    }
}
