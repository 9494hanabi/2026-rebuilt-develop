package frc.robot.commands.debug.odmetry;

import static frc.robot.lib.util.Constants.SemiAutoConstants.omegaMaximum;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.wpilibj2.command.Command;
import frc.robot.RobotState;
import frc.robot.subsystems.SwerveSubsystem;

public class SetThetaZeroCommand extends Command {

    private final SwerveSubsystem swerve;
    private final RobotState state;
    private final PIDController headingPid = new PIDController(0.001, 0.001, 0.001);
    
    public SetThetaZeroCommand(
        SwerveSubsystem swerve,
        RobotState state
    ) {
        this.swerve = swerve;
        this.state = state;
        addRequirements(swerve);

        headingPid.enableContinuousInput(-Math.PI, Math.PI);
    }

    @Override
    public void execute() {
        ChassisSpeeds cmd;
        double currentHeadingRad = 0;
        var latest = state.getLatestFieldToRobot();
        if (latest!=null) {
            currentHeadingRad = latest.getValue().getRotation().getRadians();
        }

        double omega = MathUtil.clamp(
                        headingPid.calculate(currentHeadingRad, 0),
                        -omegaMaximum, omegaMaximum);
        
        cmd = new ChassisSpeeds(0, 0, omega);
        swerve.setChassisSpeeds(cmd);
    }

    @Override
    public void end(boolean interrupted) {
        swerve.setChassisSpeeds(new ChassisSpeeds());
    }
}
