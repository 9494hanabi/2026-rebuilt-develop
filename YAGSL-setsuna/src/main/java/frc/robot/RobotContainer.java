package frc.robot;

import frc.robot.subsystems.SwerveSubsystem;
import frc.robot.subsystems.ShooterSubsystem;
import frc.robot.subsystems.ShootAngleSubsystems;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;

import swervelib.SwerveInputStream;

// 254系
import frc.robot.subsystems.vision.VisionIOHardwareLimelight;
import frc.robot.subsystems.vision.VisionIODummy;
import frc.robot.subsystems.vision.VisionSubsystem;
import frc.robot.controllboard.DriverController;
import frc.robot.lib.constants.ControlConstants;
import frc.robot.lib.limelight.LimelightConfig;
// バインディング
import frc.robot.bindings.DriveBindings;
import frc.robot.bindings.DebugBindings;
import frc.robot.bindings.AutoBindings;


// === 担当者 ===
// 共通（このファイルはできるだけ編集しない）
// 晴太
// バインディングは各Bindingsクラスに分離されています：
// - DriveBindings.java: ひなた担当（ドライブ/ビジョン関連）
// - AutoBindings.java: 二年生担当（Autonomous関連）
// - DebugBindings.java: 誰でも（デバッグ用）

public class RobotContainer {
  private final RobotState robotState;
  private final SwerveSubsystem drivebase;
  private final ShooterSubsystem shooter;
  private final DriverController m_driverController =
      new DriverController(ControlConstants.kDriverControllerPort);
  private final VisionSubsystem visionSubsystem;

  private final SendableChooser<Command> autoChooser;

  private final SwerveInputStream driveAngularVelocity;
  private final SwerveInputStream driveDirectAngle;
  private final Command driveFieldOrientedDirectAngle;
  private final Command driveFieldOrientedAngularVelocity;

  // バインディングクラス
  private final DriveBindings driveBindings;
  private final AutoBindings autoBindings;
  private final DebugBindings debugBindings;
  private final ShootAngleSubsystems shootAngle;

  public RobotContainer() {
    robotState = new RobotState();
    drivebase = new SwerveSubsystem(robotState);
    shooter = new ShooterSubsystem();
    shootAngle = new ShootAngleSubsystems();

    robotState.setVisionEstimateConsumer(drivebase::addVisionMeasurement);
    visionSubsystem = new VisionSubsystem(
        LimelightConfig.getInstance().isAnyLimelightEnabled()
            ? new VisionIOHardwareLimelight(robotState)
            : new VisionIODummy(),
        robotState);

    DriverStation.silenceJoystickConnectionWarning(true);
    driveAngularVelocity = SwerveInputStream.of(
            drivebase.getSwerveDrive(),
            () -> m_driverController.getLeftY() * -1,
            () -> m_driverController.getLeftX() * -1)
        .withControllerRotationAxis(m_driverController::getRightX)
        .deadband(ControlConstants.kDeadband)
        .scaleTranslation(0.8)
        .allianceRelativeControl(true);

    driveDirectAngle = driveAngularVelocity
        .copy()
        .withControllerHeadingAxis(m_driverController::getRightX, m_driverController::getRightY)
        .headingWhile(true);

    driveFieldOrientedDirectAngle = drivebase.driveFieldOriented(driveDirectAngle);
    driveFieldOrientedAngularVelocity = drivebase.driveFieldOriented(driveAngularVelocity);

    // バインディングクラスの初期化
    driveBindings = new DriveBindings(drivebase, robotState, m_driverController, driveAngularVelocity);
    autoBindings = new AutoBindings(drivebase, robotState, shooter, shootAngle);
    debugBindings = new DebugBindings(drivebase, visionSubsystem, robotState, m_driverController, shooter, shootAngle);

    // バインディングの設定
    configureBindings();
    drivebase.setDefaultCommand(driveFieldOrientedAngularVelocity);
    autoChooser = drivebase.buildAutoChooser("New Auto");
    SmartDashboard.putData("Auto setting", autoChooser);
  }

  private void configureBindings() {
    // ドライブ/ビジョン関連のバインディング（ひなた担当）
    driveBindings.configure();

    // Autonomous関連のバインディング（二年生担当）
    autoBindings.configure();

    // デバッグ用のバインディング（本番前にコメントアウト）
    debugBindings.configure();
  }

  public Command getAutonomousCommand() {
    return autoChooser.getSelected();
  }
}
