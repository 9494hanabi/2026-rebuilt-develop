package frc.robot.bindings;

import com.pathplanner.lib.auto.NamedCommands;
import edu.wpi.first.wpilibj2.command.Commands;
import frc.robot.commands.AutoCommand;
import frc.robot.subsystems.SwerveSubsystem;
import frc.robot.subsystems.TurretSubsystem;
import frc.robot.RobotState;

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
    private final TurretSubsystem turret;

    public AutoBindings(SwerveSubsystem drivebase, RobotState robotState, TurretSubsystem turret) {
        this.drivebase = drivebase;
        this.robotState = robotState;
        this.turret = turret;
    }

    /**
     * Autonomous関連のNamedCommandsを登録
     * PathPlannerで使用するコマンドをここに追加
     */
    public void configure() {
        /* 
        * ==== Vision/AprilTag 関連マーカー登録 ====
        * 方針:
        * 1) まずは時間ベースでSim検証
        * 2) 後で距離/状態ベース終了条件に置き換える
        * 3) マーカー名はPathPlanner側と完全一致させる
        */

        // llFaceTag1_1p0s: AprilTag ID=1に向けて、最大1.0秒だけ機首を合わせる
        NamedCommands.registerCommand(
            "llFaceTag1_1p0s",
            AutoCommand.faceTagFor(drivebase, robotState, 1, 1.0)
        );
        // llDriveOnTag0p8s: タグを検出している間だけ、最大0.8秒前進する
        NamedCommands.registerCommand(
            "llDriveOnTag0p8s",
            AutoCommand.driveOnTagFor(drivebase, robotState, 0.8)
        );
        // llAlignAndApproachTag1: 「タグ1へ向く→寄る」を1イベントで実行する
        NamedCommands.registerCommand(
            "llAlignAndApproachTag1",
            AutoCommand.alignAndApproachTag(drivebase, robotState, 1, 1.0, 0.8)
        );

        /* 
         * ==== タレット/得点関連マーカー登録 ====
         * 
        */
        // scorePrep: 得点前準備として、タグへ向いてからタレットを得点角に向ける
        NamedCommands.registerCommand(
            "scorePrep",
            Commands.sequence(
                AutoCommand.faceTagFor(drivebase, robotState, 1, 1.0),
                AutoCommand.turretToAngle(turret, 20.00, 1.2)
            )
        );
        // scoreExecuteMock: 射出機構未実装のため、ログ出力と短い待機で疑似射出を行う
        NamedCommands.registerCommand(
            "scoreExecuteMock",
            Commands.sequence(
                Commands.print("[AUTOモード] はっしゃ〜〜"),
                Commands.waitSeconds(0.20)
            )
        );
        // scoreReset: 得点後にタレットを格納位置へ戻す
        NamedCommands.registerCommand(
            "scoreReset",
            AutoCommand.turretStow(turret)
        );
        // scoreCycleBasic: 基本得点シーケンスを1イベントで実行する
        NamedCommands.registerCommand(
            "scoreCycleBasic",
            AutoCommand.scoreCycleBasic(drivebase, robotState, turret)
        );

        /*
         *==== PathPlannerイベントマーカー用コマンド登録 ====
         * 
        */
        // markStart: イベント発火確認のために開始ログを出力する
        NamedCommands.registerCommand(
            "markStart", 
            Commands.print("[AUTO] marker start")
        );
        // driveForward0p6s: テスト用に1.0m/sで0.6秒だけ前進する
        NamedCommands.registerCommand(
            "driveForward0p6s", 
            AutoCommand.driveForwardFor(drivebase, 1.0, 0.6)
        );
        // llDriveOnTag0p7s: テスト用にタグ検出前進を0.7秒だけ実行する
        NamedCommands.registerCommand(
            "llDriveOnTag0p7s", 
            AutoCommand.driveOnTagFor(drivebase, robotState, 0.7)
        );
        // stopDrive: 任意タイミングでドライブを明示停止する安全用コマンド
        NamedCommands.registerCommand(
            "stopDrive", 
            AutoCommand.stopDrive(drivebase)
        );

        /* 
         * ==== ログ/安全停止 ====
         * 
        */
        // logScoreStart: 得点シーケンス開始をログに残してデバッグしやすくする
        NamedCommands.registerCommand(
            "logScoreStart",
            AutoCommand.logMarker("score-start")
        );
        // logScoreEnd: 得点シーケンス終了をログに残してデバッグしやすくする
        NamedCommands.registerCommand(
            "logScoreEnd",
            AutoCommand.logMarker("score-end")
        );
        // safeStopAll: ドライブとタレットを同時に停止し、Auto終了時の安全を確保する
        NamedCommands.registerCommand(
            "safeStopAll",
            AutoCommand.safeStopAll(drivebase, turret)
        );
        // scoreCycleBasicSafe: 基本得点シーケンスにタイムアウトと安全停止を追加した運用版
        NamedCommands.registerCommand(
            "scoreCycleBasicSafe",
            AutoCommand.scoreCycleBasicSafe(drivebase, robotState, turret)
        );
    }
}
