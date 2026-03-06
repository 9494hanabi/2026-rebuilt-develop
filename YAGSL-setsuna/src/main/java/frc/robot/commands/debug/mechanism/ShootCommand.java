package frc.robot.commands.debug.mechanism;

import java.util.function.Supplier;

import edu.wpi.first.wpilibj2.command.Command;

import frc.robot.subsystems.mechanism.CarryerSubsystem;
import frc.robot.subsystems.mechanism.ShooterSubsystem;
import frc.robot.subsystems.mechanism.ShootAngleSubsystems;

public class ShootCommand extends Command {

  private final CarryerSubsystem carryer;
  private final ShooterSubsystem shooter;
  private final ShootAngleSubsystems shootAngler;
  private final Supplier<ShootSetpoint> setpointSupplier;

  public ShootCommand(
      CarryerSubsystem carryer,
      ShooterSubsystem shooter,
      ShootAngleSubsystems shootAngler,
      Supplier<ShootSetpoint> setpointSupplier
  ) {
    this.carryer = carryer;
    this.shooter = shooter;
    this.shootAngler = shootAngler;
    this.setpointSupplier = setpointSupplier;

    addRequirements(carryer, shooter, shootAngler);
  }

  @Override
  public void initialize() {
    System.out.println("[ShootCommand] INITIALIZED");
  }

  @Override
  public void execute() {
    ShootSetpoint setpoint = setpointSupplier.get();
    if (setpoint == null) {
      carryer.stop();
      return;
    }

    shooter.setTargetRps(setpoint.rps());
    shootAngler.setTargetMotorRot(setpoint.angleRot());

    if (shooter.atSpeed() && shootAngler.atTarget()) {
      carryer.carrying();
    } else {
      carryer.stop();
    }
  }

  @Override
  public void end(boolean interrupted) {
    carryer.stop();
    shooter.stop();
    shootAngler.stop();
  }
}
