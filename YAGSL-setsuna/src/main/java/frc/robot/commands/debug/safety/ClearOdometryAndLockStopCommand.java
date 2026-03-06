package frc.robot.commands.debug.safety;

import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj2.command.Command;
import frc.robot.lib.constants.FieldConstants;
import frc.robot.subsystems.SwerveSubsystem;

/**
 * 開始時にオドメトリ履歴を完全に消去し、指定時間は全操作を受け付けず停止を維持する。
 */

// === 担当者 ===
// ひなた
//

public class ClearOdometryAndLockStopCommand extends Command {
    private static final double kDefaultLockSeconds = 2.5;

    private final SwerveSubsystem swerve;
    private final Timer timer = new Timer();
    private final double lockSeconds;

    public ClearOdometryAndLockStopCommand(SwerveSubsystem swerve) {
        this(swerve, kDefaultLockSeconds);
    }

    public ClearOdometryAndLockStopCommand(SwerveSubsystem swerve, double lockSeconds) {
        this.swerve = swerve;
        this.lockSeconds = lockSeconds;
        addRequirements(swerve);
    }

    @Override
    public void initialize() {
        timer.restart();
        swerve.resetAllOdometry(FieldConstants.kInitialFieldToRobotPose);
        swerve.setChassisSpeeds(new ChassisSpeeds());
        System.out.printf("[ClearOdometryAndLockStop] initialized: lock=%.2fs%n", lockSeconds);
    }

    @Override
    public void execute() {
        swerve.setChassisSpeeds(new ChassisSpeeds());
    }

    @Override
    public boolean isFinished() {
        return timer.hasElapsed(lockSeconds);
    }

    @Override
    public void end(boolean interrupted) {
        timer.stop();
        swerve.setChassisSpeeds(new ChassisSpeeds());
        System.out.println("[ClearOdometryAndLockStop] ended, interrupted=" + interrupted);
    }

    @Override
    public InterruptionBehavior getInterruptionBehavior() {
        return InterruptionBehavior.kCancelIncoming;
    }

    @Override
    public boolean runsWhenDisabled() {
        return true;
    }
}
