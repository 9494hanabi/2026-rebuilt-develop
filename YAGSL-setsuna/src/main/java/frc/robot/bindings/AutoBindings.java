package frc.robot.bindings;

import com.pathplanner.lib.auto.NamedCommands;
import edu.wpi.first.wpilibj2.command.Commands;
import frc.robot.RobotState;
import frc.robot.commands.AutoCommand;
import frc.robot.subsystems.ShooterSubsystem;
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
    // 仮値: 実機完成後に距離別に最適化する
    private static final double kShooterTestTargetRps = 80.0;
    private static final double kShooterReadyTimeoutSec = 1.0;
    private static final double kShooterShootWindowSec = 2.0;


    public AutoBindings(SwerveSubsystem drivebase, RobotState robotState, ShooterSubsystem shooter) {
        this.drivebase = drivebase;
        this.robotState = robotState;
        this.shooter = shooter;
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
    }
}