package frc.robot.commands.debug.odmetry;

import static frc.robot.lib.util.Constants.SemiAutoConstants.omegaMaximum;
import static frc.robot.lib.util.Constants.SemiAutoConstants.velocityMaximum;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.wpilibj2.command.Command;
import frc.robot.RobotState;
import frc.robot.lib.util.Constants.FieldConstants;
import frc.robot.lib.util.Constants.PIDConstants;
import frc.robot.lib.util.OdomHeadingController;
import frc.robot.lib.util.PoseSelectionUtil;
import frc.robot.subsystems.SwerveSubsystem;

public class SetZeroCommand extends Command {
    private static final double kTranslationToleranceMeters = 0.05;
    private static final double kTranslationVelocityToleranceMPerSec = 0.1;
    private static final double kIntegralContributionLimit = 0.3;

    private static final double kHeadingToleranceRad = Math.toRadians(1.0);
    private static final double kHeadingVelocityToleranceRadPerSec = Math.toRadians(8.0);
    private static final double kTargetXMeter = FieldConstants.kInitialFieldToRobotPose.getX();
    private static final double kTargetYMeter = FieldConstants.kInitialFieldToRobotPose.getY();
    private static final double kTargetHeadingRad =
            FieldConstants.kInitialFieldToRobotPose.getRotation().getRadians();

    private final SwerveSubsystem swerve;
    private final RobotState state;

    private final PIDController translationPidX =
            new PIDController(PIDConstants.kTranslationKp, PIDConstants.kTranslationKi, PIDConstants.kTranslationKd);
    private final PIDController translationPidY =
            new PIDController(PIDConstants.kTranslationKp, PIDConstants.kTranslationKi, PIDConstants.kTranslationKd);
    private final PIDController headingPid =
            new PIDController(PIDConstants.kHeadingKp, PIDConstants.kHeadingKi, PIDConstants.kHeadingKd);
    private final OdomHeadingController headingControl;

    public SetZeroCommand(SwerveSubsystem swerve, RobotState state) {
        this.swerve = swerve;
        this.state = state;
        addRequirements(swerve);

        translationPidX.setTolerance(kTranslationToleranceMeters, kTranslationVelocityToleranceMPerSec);
        translationPidX.setIntegratorRange(-kIntegralContributionLimit, kIntegralContributionLimit);
        translationPidY.setTolerance(kTranslationToleranceMeters, kTranslationVelocityToleranceMPerSec);
        translationPidY.setIntegratorRange(-kIntegralContributionLimit, kIntegralContributionLimit);
        headingPid.disableContinuousInput();
        headingPid.setTolerance(kHeadingToleranceRad, kHeadingVelocityToleranceRadPerSec);
        headingPid.setIntegratorRange(-kIntegralContributionLimit, kIntegralContributionLimit);
        headingControl = new OdomHeadingController(headingPid, omegaMaximum);
    }

    @Override
    public void initialize() {
        resetControllers();
    }

    @Override
    public void execute() {
        var maybePose = PoseSelectionUtil.selectFinitePose(
                state.getLatestFieldToRobot(),
                state.getLatestFieldToRobotOdom());
        if (maybePose.isEmpty()) {
            stopAndReset();
            return;
        }

        double currentX = maybePose.get().getX();
        double currentY = maybePose.get().getY();
        double currentHeadingRad = maybePose.get().getRotation().getRadians();

        var latestOdom = state.getLatestFieldToRobotOdom();
        if (latestOdom != null && PoseSelectionUtil.isFinitePose(latestOdom.getValue())) {
            currentHeadingRad = latestOdom.getValue().getRotation().getRadians();
        }

        if (!Double.isFinite(currentX) || !Double.isFinite(currentY) || !Double.isFinite(currentHeadingRad)) {
            stopAndReset();
            return;
        }

        double translationX = MathUtil.clamp(
                translationPidX.calculate(currentX, kTargetXMeter),
                -velocityMaximum,
                velocityMaximum);
        double translationY = MathUtil.clamp(
                translationPidY.calculate(currentY, kTargetYMeter),
                -velocityMaximum,
                velocityMaximum);

        var maybeHeadingResult = headingControl.calculate(currentHeadingRad, kTargetHeadingRad);
        if (maybeHeadingResult.isEmpty()) {
            stopAndReset();
            return;
        }

        double omega = maybeHeadingResult.get().omegaRadPerSec();
        if (!Double.isFinite(translationX) || !Double.isFinite(translationY) || !Double.isFinite(omega)) {
            stopAndReset();
            return;
        }

        if (translationPidX.atSetpoint() && translationPidY.atSetpoint()) {
            translationX = 0.0;
            translationY = 0.0;
        }

        swerve.driveFieldOriented(new ChassisSpeeds(translationX, translationY, omega));
    }

    @Override
    public void end(boolean interrupted) {
        stopAndReset();
    }

    private void stopAndReset() {
        resetControllers();
        swerve.driveFieldOriented(new ChassisSpeeds());
    }

    private void resetControllers() {
        translationPidX.reset();
        translationPidY.reset();
        headingControl.reset();
    }
}
