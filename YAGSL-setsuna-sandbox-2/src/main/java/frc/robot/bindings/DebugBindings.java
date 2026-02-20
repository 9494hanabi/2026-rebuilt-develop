package frc.robot.bindings;

import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.button.CommandXboxController;
import frc.robot.RobotState;
import frc.robot.commands.debug.odmetry.SetDriveHorizonCommand;
import frc.robot.subsystems.SwerveSubsystem;
import frc.robot.subsystems.vision.VisionSubsystem;

// === 担当者 ===
// 誰でも（デバッグ用）
//
// デバッグ用のバインディングを管理するクラス
// テスト中のコードやデバッグ用のバインディングはここに書く
// 本番前に configure() の呼び出しをコメントアウトすればOK

public class DebugBindings {
    private final SwerveSubsystem drivebase;
    private final VisionSubsystem vision;
    private final RobotState robotState;
    private final CommandXboxController controller;

    public DebugBindings(
            SwerveSubsystem drivebase,
            VisionSubsystem vision,
            RobotState robotState,
            CommandXboxController controller) {
        this.drivebase = drivebase;
        this.vision = vision;
        this.robotState = robotState;
        this.controller = controller;
    }

    /**
     * デバッグ用のバインディングを設定
     * 本番前にこのメソッドの呼び出しをコメントアウトすること！
     */
    public void configure() {
        // ===============================================
        // デバッグ用のバインディングをここに追加
        // ===============================================

        // Startボタン: ジャイロをゼロリセット
        controller.start().onTrue(Commands.runOnce(() -> {
            drivebase.getSwerveDrive().zeroGyro();
            System.out.println("Debug: Gyro zeroed!");
        }));

        // Backボタン: 現在のポーズを表示
        controller.back().onTrue(Commands.runOnce(() -> {
            System.out.println("Debug: Current Pose = " + drivebase.getSwerveDrive().getPose());
        }));
    }
}
