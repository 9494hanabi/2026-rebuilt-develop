package frc.robot.bindings;

import static frc.robot.lib.util.Constants.VisionConstants.kLimelightBTableName;

import edu.wpi.first.wpilibj2.command.button.CommandXboxController;
import frc.robot.RobotState;
import frc.robot.commands.debug.vision.AbsoluteDriveOKCommand;
import frc.robot.commands.debug.vision.ObservationOKCommand;
import frc.robot.commands.debug.vision.RelativeDriveOKCommand;
import frc.robot.commands.debug.odmetry.SetDriveHorizonCommand;
import frc.robot.commands.debug.odmetry.SetDriveToZeroCommand;
import frc.robot.commands.debug.odmetry.SetThetaZeroCommand;
import frc.robot.commands.debug.odmetry.SetToTagCommand;
import frc.robot.commands.debug.odmetry.SetZeroCommand;
import frc.robot.lib.util.Constants.VisionConstants;
import frc.robot.subsystems.SwerveSubsystem;
import swervelib.SwerveInputStream;

// === 担当者 ===
// ひなた
//
// ドライブ/ビジョン関連のバインディングを管理するクラス
// このファイルはひなた専用です。他の人は編集しないでください。

public class DriveBindings {
    private final SwerveSubsystem drivebase;
    private final RobotState robotState;
    private final CommandXboxController controller;
    private final SwerveInputStream driveAngularVelocity;

    public DriveBindings(
            SwerveSubsystem drivebase,
            RobotState robotState,
            CommandXboxController controller,
            SwerveInputStream driveAngularVelocity) {
        this.drivebase = drivebase;
        this.robotState = robotState;
        this.controller = controller;
        this.driveAngularVelocity = driveAngularVelocity;
    }

    /**
     * ドライブ/ビジョン関連のバインディングを設定
     * 本番時もこのメソッドを呼び出す
     */
    public void configure() {
        // B: AprilTagへ相対ドライブ
        controller.b().whileTrue(
            new RelativeDriveOKCommand(drivebase, robotState)
        );

        // A: タグが見えているなら後ろへドライブ
        controller.a().whileTrue(
            new ObservationOKCommand(drivebase)
        );

        // Y: 角度を0へ、
        controller.y().whileTrue(
            new SetThetaZeroCommand(drivebase, robotState)
        );

        // X:AprilTagに絶対ドライブ
        controller.x().whileTrue(
            new AbsoluteDriveOKCommand(drivebase, robotState)
        );

        // 上矢印（POV Up）: フィールド基準で水平移動
        controller.povUp().whileTrue(
            new SetDriveHorizonCommand(drivebase)
        );

        // 下矢印 : フィールド基準で0へドライブ
        controller.povDown().whileTrue(
            new SetDriveToZeroCommand(drivebase, robotState)
        );

        // 右矢印 : 角度も含めて0へ
        controller.povRight().whileTrue(
            new SetZeroCommand(drivebase, robotState)
        );

        // 左矢印 : タグを探索→ロック→頂点座標へドライブ
        controller.povLeft().whileTrue(
            new SetToTagCommand(drivebase, robotState)
        );
    }
}
