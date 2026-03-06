package frc.robot.commands.debug.mechanism;

import edu.wpi.first.wpilibj2.command.Command;

import frc.robot.subsystems.mechanism.ExtenderSubsystem;
import frc.robot.subsystems.mechanism.IntakerSubsystem;

public class IntakeCommand extends Command {

  private final IntakerSubsystem intaker;
  private final ExtenderSubsystem extender;

  public IntakeCommand(
      IntakerSubsystem intaker,
      ExtenderSubsystem extender
  ) {
    this.intaker = intaker;
    this.extender = extender;

    addRequirements(intaker, extender);
  }

  @Override
  public void initialize() {
    intaker.stop();
    System.out.println("[IntakeCommand] INITIALIZED");
  }

  @Override
  public void execute() {
    extender.SetIntakePosition();

    if (extender.extenderAtIntakePosition()) {
      intaker.intaking();
    } else {
      intaker.stop();
    }
  }

  @Override
  public void end(boolean interrupted) {
    intaker.stop();
    extender.SetDrivePosition();
  }
}
