package frc.robot;

import frc.robot.commands.FaceAprilTagCommand;
import frc.robot.commands.DriveOnTagCommand;
import frc.robot.commands.DriveWhileFieldPoseValidCommand;

import frc.robot.subsystems.SwerveSubsystem;


import frc.robot.lib.util.Constants.OperatorConstants;
import frc.robot.lib.util.Constants.VisionConstants;

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
  private final VisionSubsystem visionSubsystem;

  SendableChooser<Command> m_chooser = new SendableChooser<>();



  private final SwerveInputStream driveAngularVelocity;
  private final SwerveInputStream driveDirectAngle;
  private final Command driveFieldOrientedDirectAngle;
  private final Command driveFieldOrientedAngularVelocity;

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

    driveDirectAngle = driveAngularVelocity
        .copy()
        .withControllerHeadingAxis(m_driverController::getRightX, m_driverController::getRightY)
        .headingWhile(true);

    driveFieldOrientedDirectAngle = drivebase.driveFieldOriented(driveDirectAngle);
    driveFieldOrientedAngularVelocity = drivebase.driveFieldOriented(driveAngularVelocity);

    configureBindings();
    drivebase.setDefaultCommand(driveFieldOrientedAngularVelocity);
    NamedCommands.registerCommand("test", Commands.print("Hello Hanabi"));
  }

  private void configureBindings() {
    m_driverController.b().whileTrue(
      new FaceAprilTagCommand(drivebase, robotState)
    );
    m_driverController.a().whileTrue(
      new DriveOnTagCommand(drivebase)
    );
    m_driverController.y().whileTrue(
      new DriveOnTagCommand(drivebase, VisionConstants.kLimelightATableName)
    );
    m_driverController.x().whileTrue(
      new DriveWhileFieldPoseValidCommand(drivebase, robotState, driveAngularVelocity)
    );
  }

  public Command getAutonomousCommand() {
    return m_chooser.getSelected();
  }
}
