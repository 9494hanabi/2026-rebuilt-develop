package frc.robot;

// import frc.robot.commands.Autos;
// import frc.robot.commands.ExampleCommand;
import frc.robot.commands.FaceAprilTagCommand;

import frc.robot.subsystems.SwerveSubsystem;
import frc.robot.subsystems.vision.VisionSubsystem;

import frc.robot.lib.util.Constants.OperatorConstants;

import com.pathplanner.lib.auto.NamedCommands;

import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.button.CommandXboxController;
import edu.wpi.first.wpilibj2.command.button.Trigger;

import swervelib.SwerveInputStream;

// === 担当者 ===
// はるた
//

public class RobotContainer {
  private final SwerveSubsystem drivebase = new SwerveSubsystem();
  private final CommandXboxController m_driverController =
      new CommandXboxController(OperatorConstants.kDriverControllerPort);

  public RobotContainer() {
    DriverStation.silenceJoystickConnectionWarning(true);
    configureBindings();
    drivebase.setDefaultCommand(driveFieldOrentedAngularVelocity);
    NamedCommands.registerCommand("test", Commands.print("Hello Hanabi"));
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
    // ここに.autoの名前を入力する
    return drivebase.getAutonomousCommand("New Auto");
  }
}
