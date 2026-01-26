package frc.robot;

// import frc.robot.commands.Autos;
// import frc.robot.commands.ExampleCommand;
import frc.robot.commands.FaceAprilTagCommand;

import frc.robot.subsystems.SwerveSubsystem;


import frc.robot.lib.util.Constants.OperatorConstants;

import com.pathplanner.lib.auto.NamedCommands;

import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.button.CommandXboxController;

import swervelib.SwerveInputStream;

// 254系
import frc.robot.subsystems.vision.VisionIOHardwareLimelight;
import frc.robot.subsystems.vision.VisionSubsystem;


// === 担当者 ===
// ひなた
//

public class RobotContainer {
  private final RobotState robotState;
  private final SwerveSubsystem drivebase;
  private final CommandXboxController m_driverController =
      new CommandXboxController(OperatorConstants.kDriverControllerPort);
<<<<<<< HEAD

  //AutoSetting
  //これを書き換えて選択できるようにしていく Robot.java と連携
  // private final Command m_simpleAuto = new DriveDistance(
  //       AutoConstants.kAutoDriveDistanceInches,
  //       AutoConstants.kAutoDriveSpeed,m_robotDrive);
  
=======
  
  private final VisionSubsystem visionSubsystem;

  // //AutoSetting
  // //これを書き換えて選択できるようにしていく Robot.java と連携
  // private final Command m_simpleAuto = new DriveDistance(
  //       AutoConstants.kAutoDriveDistanceInches,
  //       AutoConstants.kAutoDriveSpeed,m_robotDrive);
  
>>>>>>> origin/develop
  // private final Command m_complexAuto = new ComplexAuto(m_robotDrive, m_hatchSubsystem);

  //自律コマンド用の SendableChooser スマートダッシュボードで選択できるようにしているクラス
  SendableChooser<Command> m_chooser = new SendableChooser<>();



  private final SwerveInputStream driveAngularVelocity;
  private final SwerveInputStream driveDriectAngle;
  private final Command driveFieldorientedDriectAngle;
  private final Command driveFieldOrentedAngularVelocity;

  public RobotContainer() {
    robotState = new RobotState();
    drivebase = new SwerveSubsystem(robotState);
    robotState.setVisionEstimateConsumer(drivebase::addVisionMeasurement);
    visionSubsystem = new VisionSubsystem(new VisionIOHardwareLimelight(robotState), robotState);

    DriverStation.silenceJoystickConnectionWarning(true);
    driveAngularVelocity = SwerveInputStream.of(
            drivebase.getSwerveDrive(),
            () -> m_driverController.getLeftY() * -1,
            () -> m_driverController.getLeftX() * -1)
        .withControllerRotationAxis(m_driverController::getRightX)
        .deadband(OperatorConstants.DEADBAND)
        .scaleTranslation(0.8)
        .allianceRelativeControl(true);

    driveDriectAngle = driveAngularVelocity
        .copy()
        .withControllerHeadingAxis(m_driverController::getRightX, m_driverController::getRightY)
        .headingWhile(true);

    driveFieldorientedDriectAngle = drivebase.driveFieldOriented(driveDriectAngle);
    driveFieldOrentedAngularVelocity = drivebase.driveFieldOriented(driveAngularVelocity);

    configureBindings();
    drivebase.setDefaultCommand(driveFieldOrentedAngularVelocity);
    NamedCommands.registerCommand("test", Commands.print("Hello Hanabi"));

<<<<<<< HEAD
    // Autoダッシュボード設定
    // Add commands to the autonomous command chooser
=======
    // // Autoダッシュボード設定
    // // Add commands to the autonomous command chooser
>>>>>>> origin/develop
    // m_chooser.setDefaultOption("Simple Auto", m_simpleAuto);
    // m_chooser.addOption("Complex Auto", m_complexAuto);
  }

  private void configureBindings() {
    m_driverController.b().whileTrue(
      new FaceAprilTagCommand(drivebase)
    );
  }
   public Command getAutonomousCommand() {
    // ダッシュボードで選択したコマンドを実行する役割
    return m_chooser.getSelected();

    // // ここに.autoの名前を入力する
    // return drivebase.getAutonomousCommand("New Auto");
  }
}
