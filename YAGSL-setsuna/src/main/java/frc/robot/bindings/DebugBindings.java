package frc.robot.bindings;

import edu.wpi.first.wpilibj2.command.Commands;
import frc.robot.RobotState;
import frc.robot.lib.constants.commandconstants.ShootAngleCommandConstants;
import frc.robot.subsystems.mechanism.ShooterSubsystem;
import frc.robot.subsystems.mechanism.ShootAngleSubsystems;


// === 担当者 ===
// 誰でも（デバッグ用）
//  一旦晴太
// デバッグ用のバインディングを管理するクラス
// テスト中のコードやデバッグ用のバインディングはここに書く
// 本番前に configure() の呼び出しをコメントアウトすればOK
import frc.robot.subsystems.SwerveSubsystem;
import frc.robot.subsystems.vision.VisionSubsystem;

public class DebugBindings {
    private final SwerveSubsystem drivebase;
    private final VisionSubsystem vision;
    private final RobotState robotState;
    private final DriverController controller;
    private final ShooterSubsystem shooter;
    private static final double kDebugShooterRps = ShooterSubsystem.kNominalShotRps;

    private final ShootAngleSubsystems shootAngle;
    private int shootAnglePresetIndex = ShootAngleCommandConstants.kDefaultPresetIndex;


    public DebugBindings(
            SwerveSubsystem drivebase,
            VisionSubsystem vision,
            RobotState robotState,
            DriverController controller,
            ShooterSubsystem shooter,
            ShootAngleSubsystems shootAngle) {
        this.drivebase = drivebase;
        this.vision = vision;
        this.robotState = robotState;
        this.controller = controller;
        this.shooter = shooter;
        this.shootAngle = shootAngle;
    }


    /**
     * デバッグ用のバインディングを設定
     * 本番前にこのメソッドの呼び出しをコメントアウトすること！
     */
    public void configure() {
        // ===============================================
        // デバッグ用のバインディングをここに追加
        // ===============================================

        // ライトバンパー（押し込めるやつの一個前）：クラーケンが回る
        controller.rightBumper().onTrue(Commands.runOnce(() -> {
            shooter.setTargetRps(kDebugShooterRps);
            System.out.printf("[Debug Shooter] START target=%.1f RPS%n", kDebugShooterRps);
        }, shooter));

        // ライトバンパー：離したらクラーケンが止まる
        controller.rightBumper().onFalse(Commands.runOnce(() -> {
            shooter.stop();
            System.out.printf("[Debug Shooter] STOP actual=%.2f RPS%n", shooter.getVelocityRps());
        }, shooter));

        // ライトトリガー：押したら状況を見ることができる
        controller.rightTrigger().onTrue(Commands.runOnce(() -> {
            System.out.printf(
            "[Debug Shooter] STATUS target=%.2f actual=%.2f error=%.2f atSpeed=%b%n",
            shooter.getTargetRps(),
            shooter.getVelocityRps(),
            shooter.getVelocityErrorRps(),
            shooter.atSpeed());
        }, shooter));

        controller.povUp().onTrue(Commands.runOnce(() -> {
            shootAnglePresetIndex =
                Math.min(shootAnglePresetIndex + 1, ShootAngleCommandConstants.kPresetRot.length - 1);
            double targetRot = ShootAngleCommandConstants.kPresetRot[shootAnglePresetIndex];
            shootAngle.setTargetMotorRot(targetRot);
            System.out.printf("[ShootAngle] DPadUp -> L%d (%.6f rot)%n", shootAnglePresetIndex + 1, targetRot);
        }, shootAngle));

        controller.povDown().onTrue(Commands.runOnce(() -> {
            shootAnglePresetIndex = Math.max(shootAnglePresetIndex - 1, 0);
            double targetRot = ShootAngleCommandConstants.kPresetRot[shootAnglePresetIndex];
            shootAngle.setTargetMotorRot(targetRot);
            System.out.printf("[ShootAngle] DPadDown -> L%d (%.6f rot)%n", shootAnglePresetIndex + 1, targetRot);
        }, shootAngle));

        // Backボタン: 現在のポーズを表示
        controller.back().onTrue(Commands.runOnce(() -> {
            System.out.println("Debug: Current Pose = " + drivebase.getSwerveDrive().getPose());
        }));
    }
}
