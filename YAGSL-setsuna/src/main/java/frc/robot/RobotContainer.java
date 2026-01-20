package frc.robot;

// import frc.robot.commands.Autos;
// import frc.robot.commands.ExampleCommand;
import frc.robot.commands.FaceAprilTagCommand;
import frc.robot.lib.util.Constants.OperatorConstants;
import frc.robot.subsystems.SwerveSubsystem;
import frc.robot.subsystems.vision.VisionSubsystem;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.button.CommandXboxController;
import edu.wpi.first.wpilibj2.command.button.Trigger;

import swervelib.SwerveInputStream;
public class RobotContainer {
  private final SwerveSubsystem drivebase = new SwerveSubsystem();
  private final CommandXboxController m_driverController =
      new CommandXboxController(OperatorConstants.kDriverControllerPort);
  private final VisionSubsystem vision =
      new VisionSubsystem(drivebase.getSwerveDrive());

  public RobotContainer() {
    configureBindings();
    drivebase.setDefaultCommand(driveFieldOrentedAngularVelocity);
  }

  SwerveInputStream driveAngularVelocity = SwerveInputStream.of(drivebase.getSwerveDrive(),
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
}
