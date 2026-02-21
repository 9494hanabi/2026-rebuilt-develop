package frc.robot.bindings;

import edu.wpi.first.wpilibj2.command.Commands;
import frc.robot.RobotState;
import frc.robot.controllboard.DriverController;
import frc.robot.commands.debug.odmetry.SetDriveHorizonCommand;
import frc.robot.commands.debug.odmetry.SetThetaZeroCommand;
import frc.robot.commands.debug.odmetry.SetToTagCommand;
import frc.robot.commands.debug.odmetry.SetZeroCommand;
import frc.robot.commands.debug.safety.ClearOdometryAndLockStopCommand;
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
    private final DriverController controller;
    private final SwerveInputStream driveAngularVelocity;

    public DriveBindings(
            SwerveSubsystem drivebase,
            RobotState robotState,
            DriverController controller,
            SwerveInputStream driveAngularVelocity) {
        this.drivebase = drivebase;
        this.robotState = robotState;
        this.controller = controller;
        this.driveAngularVelocity = driveAngularVelocity;
    }

    public void configure() {
        controller.a().onTrue(Commands.print("[DriveBindings] A pressed -> SetDriveHorizonCommand"));
        controller.x().onTrue(Commands.print("[DriveBindings] X pressed -> SetZeroCommand"));

        controller.b().whileTrue(
            new SetThetaZeroCommand(drivebase, robotState)
        );

        controller.a().whileTrue(
            new SetDriveHorizonCommand(drivebase, robotState)
        );

        controller.y().whileTrue(
            new SetToTagCommand(drivebase, robotState)
        );

        controller.x().whileTrue(
            new SetZeroCommand(drivebase, robotState)
        );

        // 強制停止コマンド
        controller.leftTrigger().onTrue(
            new ClearOdometryAndLockStopCommand(drivebase)
        );
    }
}
