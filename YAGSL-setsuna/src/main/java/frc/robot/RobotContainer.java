package frc.robot;

// import frc.robot.commands.Autos;
// import frc.robot.commands.ExampleCommand;
import frc.robot.commands.FaceAprilTagCommand;

import frc.robot.subsystems.SwerveSubsystem;


import frc.robot.lib.util.Constants.OperatorConstants;

import com.pathplanner.lib.auto.NamedCommands;

import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.button.CommandXboxController;
import edu.wpi.first.wpilibj2.command.button.Trigger;

import swervelib.SwerveInputStream;

// 254系
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import frc.robot.subsystems.vision.VisionFieldPoseEstimate;
import frc.robot.subsystems.vision.VisionIO;
import frc.robot.subsystems.vision.VisionIOHardwareLimelight;
import frc.robot.subsystems.vision.VisionSubsystem;

// === 担当者 ===
// ひなた
//

public class RobotContainer {
  // 254要素
  private final AtomicReference<SwerveSubsystem> swerveRef = new AtomicReference();

  private final Consumer<VisionFieldPoseEstimate> visionEstimateConsumer =
    (VisionFieldPoseEstimate estimate) -> {
      var swerve = swerveRef.get();
      if (swerve != null) {
        swerve.addVisionMeasurement(estimate);
      }
    };
  
  private final RobotState robotState = new RobotState(visionEstimateConsumer);

  private final SwerveSubsystem drivebase = new SwerveSubsystem();
  private final CommandXboxController m_driverController =
      new CommandXboxController(OperatorConstants.kDriverControllerPort);
  
  private final VisionSubsystem visionSubsystem = 
      new VisionSubsystem(new VisionIOHardwareLimelight(robotState), robotState);

  //AutoSetting
  //これを書き換えて選択できるようにしていく Robot.java と連携
  private final Command m_simpleAuto = new DriveDistance(
        AutoConstants.kAutoDriveDistanceInches,
        AutoConstants.kAutoDriveSpeed,m_robotDrive);
  
  private final Command m_complexAuto = new ComplexAuto(m_robotDrive, m_hatchSubsystem);

  //自律コマンド用の SendableChooser スマートダッシュボードで選択できるようにしているクラス
  SendableChooser<Command> m_chooser = new SendableChooser<>();



  public RobotContainer() {
    swerveRef.set(drivebase);
    DriverStation.silenceJoystickConnectionWarning(true);
    configureBindings();
    drivebase.setDefaultCommand(driveFieldOrentedAngularVelocity);
    NamedCommands.registerCommand("test", Commands.print("Hello Hanabi"));

    // Autoダッシュボード設定
    // Add commands to the autonomous command chooser
    m_chooser.setDefaultOption("Simple Auto", m_simpleAuto);
    m_chooser.addOption("Complex Auto", m_complexAuto);
  }

  SwerveInputStream driveAngularVelocity = SwerveInputStream.of(drivebase.getSwerveDrive(), //ここで、左スティックの割り当てをしている。下にある値を変えると遅くなったりする（絶対値１が最大）。上下、左右の入力
                                                                () -> m_driverController.getLeftY() * -1,
                                                                () -> m_driverController.getLeftX() * -1)
                                                                .withControllerRotationAxis(m_driverController::getRightX)
                                                                .deadband(OperatorConstants.DEADBAND)
                                                                .scaleTranslation(0.8) // 速度のスケーリング係数
                                                                .allianceRelativeControl(true);

  SwerveInputStream driveDriectAngle = driveAngularVelocity.copy().withControllerHeadingAxis(m_driverController::getRightX,
                                                                                             m_driverController::getRightY)
                                                                                           .headingWhile(true);

  Command driveFieldorientedDriectAngle = drivebase.driveFieldOriented(driveDriectAngle);

  Command driveFieldOrentedAngularVelocity = drivebase.driveFieldOriented(driveAngularVelocity);

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
