package frc.robot.bindings;

import edu.wpi.first.wpilibj2.command.button.CommandXboxController;
import frc.robot.RobotState;
import frc.robot.commands.debug.odmetry.SetDriveHorizonCommand;
import frc.robot.commands.debug.odmetry.SimpleCommand;
import frc.robot.commands.debug.odmetry.SetThetaZeroCommand;
import frc.robot.commands.debug.odmetry.SetToTagCommand;
import frc.robot.commands.debug.odmetry.DriveToTag3FrontCommand;
import frc.robot.commands.debug.odmetry.DriveToTagFrontCommand;
import frc.robot.commands.debug.odmetry.SetZeroCommand;
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

    public void configure() {
        controller.b().whileTrue(
            new SetThetaZeroCommand(drivebase, robotState)
        );

        controller.a().whileTrue(
            new SetDriveHorizonCommand(drivebase)
        );

        // Y: 角度を0へ、
        controller.y().whileTrue(
            new SetToTagCommand(drivebase, robotState)
        );

        // X:AprilTagに絶対ドライブ
        controller.x().whileTrue(
            new SetZeroCommand(drivebase, robotState)
        );

        controller.povUp().whileTrue(
            new SimpleCommand(drivebase, robotState)
        );

        // POV Down: タグ正面へドライブ (ビジョン使用)
        controller.povDown().whileTrue(
            new DriveToTagFrontCommand(drivebase, robotState)
        );

        // POV Left: タグ3正面へドライブ (ハードコード座標、デバッグ用)
        controller.povLeft().whileTrue(
            new DriveToTag3FrontCommand(drivebase, robotState)
        );

    }
}
