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
        // ---- Limelight Auto用コマンド登録 ----
        /*
         * 今は秒で指定しているけど、これをどのくらいの距離感になったらとかもできる。
         * simで確認したいから秒数にしています。
         * ---以下例プログラム--- AutoCommandに追加してね。
         * // 例: 「タグに0.8mまで近づいたら終了」。ただし3秒で強制終了も付ける。
         *    public static Command driveOnTagUntilDistance(
         *    SwerveSubsystem drivebase,
         *    RobotState state,
         *    int tagId,
         *    double targetDistanceMeters,
         *    double maxSeconds
         *    ) {
         *        return new DriveOnTagCommand(drivebase)
         *        .until(() -> isTagDistanceWithin(state, tagId, targetDistanceMeters))
         *        .withTimeout(maxSeconds)
         *        .andThen(stopDrive(drivebase));
         *    }
         */
        // tagI = 1へ1.0秒だけ向く
        NamedCommands.registerCommand(
            "llFaceTag1_1p0s",
            AutoCommand.faceTagFor(drivebase, robotState, 1, 1.0)
        );
        // タグが見えている間だけ0.8秒前進
        NamedCommands.registerCommand(
            "llDriveOnTag0p8s",
            AutoCommand.driveOnTagFor(drivebase, 0.8)
        );
        // 「向く ->　進む」をまとめて一つのイベントとして実行
        NamedCommands.registerCommand(
            "llAlignAndApproachTag1",
            AutoCommand.alignAndApproachTag(drivebase, robotState, 1, 1.0, 0.8)
        );

        // ---- タレットのコマンド登録 ----
        NamedCommands.registerCommand(
            "scorePrep",
            Commands.sequence(
                AutoCommand.faceTagFor(drivebase, robotState, 1, 1.0),
                AutoCommand.turretToAngle(turret, 20.00, 1.2)
            )
        );

        NamedCommands.registerCommand(
            "scoreExecuteMock",
            Commands.sequence(
                Commands.print("[AUTOモード] はっしゃ〜〜"),
                Commands.waitSeconds(0.20)
            )
        );

        NamedCommands.registerCommand(
            "scoreReset",
            AutoCommand.turretStow(turret)
        );

        NamedCommands.registerCommand(
            "scoreCycleBasic",
            AutoCommand.scoreCycleBasic(drivebase, robotState, turret)
        );

        // ---- PathPlannerイベントマーカー用コマンド登録 ----
        // .auto側のイベント名と、ここで登録する名前を完全一致させること。
        NamedCommands.registerCommand(
            "markStart", 
            Commands.print("[AUTO] marker start")
        );

        // テスト用: 1.0 m/sで0.6秒だけ前進
        NamedCommands.registerCommand(
            "driveForward0p6s", 
            AutoCommand.driveForwardFor(drivebase, 1.0, 0.6)
        );
        // テスト用: Limelightタグ検出時に0.7秒だけ前進（終了時は自動停止）
        NamedCommands.registerCommand(
            "llDriveOnTag0p7s", 
            AutoCommand.driveOnTagFor(drivebase, 0.7)
        );
        // 明示停止用マーカー（任意タイミングでブレーキ）
        NamedCommands.registerCommand(
            "stopDrive", 
            AutoCommand.stopDrive(drivebase)
        );

        // ---- ログ/安全停止 ----
        NamedCommands.registerCommand(
            "logScoreStart",
            AutoCommand.logMarker("score-start")
        );

        NamedCommands.registerCommand(
            "logScoreEnd",
            AutoCommand.logMarker("score-end")
        );

        NamedCommands.registerCommand(
            "safeStopAll",
            AutoCommand.safeStopAll(drivebase, turret)
        );

        NamedCommands.registerCommand(
            "scoreCycleBasicSafe",
            AutoCommand.scoreCycleBasicSafe(drivebase, robotState, turret)
        );
    }
}
