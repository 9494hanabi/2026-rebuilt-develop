package frc.robot.bindings;

import static frc.robot.lib.util.Constants.VisionConstants.kLimelightBTableName;

import edu.wpi.first.wpilibj2.command.button.CommandXboxController;
import frc.robot.RobotState;
import frc.robot.commands.debug.vision.AbsoluteDriveOKCommand;
import frc.robot.commands.debug.vision.ObservationOKCommand;
import frc.robot.commands.debug.vision.RelativeDriveOKCommand;
import frc.robot.commands.DriveOnTagCommand;

import frc.robot.commands.DriveWhileFieldPoseValidCommand;
import frc.robot.commands.FaceAprilTagCommand;
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
            new RelativeDriveOKCommand(drivebase)
        );

        // A: タグが見えているなら後ろへドライブ
        controller.a().whileTrue(
            new ObservationOKCommand(drivebase)
        );

        // Y: タグに向かってドライブ（Limelight A）
        controller.y().whileTrue(
            new DriveOnTagCommand(drivebase, robotState, VisionConstants.kLimelightATableName)
        );

        // X:AprilTagに絶対ドライブ
        controller.x().whileTrue(
            new AbsoluteDriveOKCommand(drivebase, robotState)
        );
    }
}
