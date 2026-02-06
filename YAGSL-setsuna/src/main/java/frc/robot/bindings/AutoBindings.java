package frc.robot.bindings;

import com.pathplanner.lib.auto.NamedCommands;
import edu.wpi.first.wpilibj2.command.Commands;
import frc.robot.subsystems.SwerveSubsystem;

// === 担当者 ===
// 二年生（Autonomous担当）
//
// Autonomous関連のバインディングとNamedCommandsを管理するクラス
// このファイルは二年生専用です。他の人は編集しないでください。

public class AutoBindings {
    private final SwerveSubsystem drivebase;

    public AutoBindings(SwerveSubsystem drivebase) {
        this.drivebase = drivebase;
    }

    /**
     * Autonomous関連のNamedCommandsを登録
     * PathPlannerで使用するコマンドをここに追加
     */
    public void configure() {
        // テスト用コマンド
        NamedCommands.registerCommand("test", Commands.print("Hello Hanabi"));

        // ここにAuto用のNamedCommandsを追加していく
        // 例:
        // NamedCommands.registerCommand("intake", new IntakeCommand(intake));
        // NamedCommands.registerCommand("shoot", new ShootCommand(shooter));
        // NamedCommands.registerCommand("alignToTarget", new AlignCommand(drivebase));
    }
}
