package frc.robot.bindings;

import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.button.Trigger;
import frc.robot.commands.debug.mechanism.ShootSetpoint;
import frc.robot.lib.util.TrajectoryCalculationHelper;
import frc.robot.subsystems.mechanism.ShooterSubsystem;
import frc.robot.subsystems.mechanism.ShootAngleSubsystems;

public class TrajectoryBindings {
    private final DriverController controller;
    private final ShooterSubsystem shooter;
    private final ShootAngleSubsystems shootAngle;

    public TrajectoryBindings(
            DriverController controller,
            ShooterSubsystem shooter,
            ShootAngleSubsystems shootAngle) {
        this.controller = controller;
        this.shooter = shooter;
        this.shootAngle = shootAngle;
    }

    public void configure() {
        bindDistance(controller.a(),         1.0);
        bindDistance(controller.b(),         2.0);
        bindDistance(controller.x(),         3.0);
        bindDistance(controller.y(),         4.0);
        bindDistance(controller.povUp(),     5.0);
        bindDistance(controller.povRight(),  6.0);
        bindDistance(controller.povDown(),   7.0);
        bindDistance(controller.povLeft(),   8.0);
    }

    private void bindDistance(Trigger trigger, double distanceMeters) {
        trigger.whileTrue(
            Commands.run(() -> {
                ShootSetpoint setpoint = TrajectoryCalculationHelper.calculateOptimalSetpoint(distanceMeters, 1.15);
                if (setpoint == null) {
                    return;
                }
                shooter.setTargetRps(setpoint.rps());
                shootAngle.setTargetMotorRot(setpoint.angleRot());
            }, shooter, shootAngle)
            .beforeStarting(() ->
                System.out.printf("[Trajectory] %.0fm -> %s%n",
                    distanceMeters,
                    TrajectoryCalculationHelper.calculateOptimalSetpoint(distanceMeters, 1.15)))
            .finallyDo(() -> {
                shooter.stop();
                shootAngle.stop();
            })
        );
    }
}
