package frc.robot;

import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.button.CommandXboxController;
import frc.robot.bindings.DriveBindings;
import frc.robot.commands.debug.odmetry.ClearOdometryAndLockStopCommand;
import frc.robot.lib.limelight.LimelightConfig;
import frc.robot.lib.util.Constants.OperatorConstants;
import frc.robot.subsystems.SwerveSubsystem;
import frc.robot.subsystems.vision.VisionIODummy;
import frc.robot.subsystems.vision.VisionIOHardwareLimelight;
import frc.robot.subsystems.vision.VisionSubsystem;
import swervelib.SwerveInputStream;

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
    private final Command startupOdometryLockCommand;

    private final DriveBindings driveBindings;

    public RobotContainer() {
        robotState = new RobotState();
        drivebase = new SwerveSubsystem(robotState);
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
                .deadband(OperatorConstants.DEADBAND)
                .scaleTranslation(0.8)
                .allianceRelativeControl(true);

        driveDirectAngle = driveAngularVelocity
                .copy()
                .withControllerHeadingAxis(m_driverController::getRightX, m_driverController::getRightY)
                .headingWhile(true);

        driveFieldOrientedDirectAngle = drivebase.driveFieldOriented(driveDirectAngle);
        driveFieldOrientedAngularVelocity = drivebase.driveFieldOriented(driveAngularVelocity);

        driveBindings = new DriveBindings(drivebase, robotState, m_driverController, driveAngularVelocity);
        driveBindings.configure();

        drivebase.setDefaultCommand(driveFieldOrientedAngularVelocity);
        startupOdometryLockCommand = new ClearOdometryAndLockStopCommand(drivebase);

        m_chooser.setDefaultOption("Do Nothing", Commands.none());
    }

    public Command getAutonomousCommand() {
        return m_chooser.getSelected();
    }

    public Command getStartupOdometryLockCommand() {
        return startupOdometryLockCommand;
    }
}
