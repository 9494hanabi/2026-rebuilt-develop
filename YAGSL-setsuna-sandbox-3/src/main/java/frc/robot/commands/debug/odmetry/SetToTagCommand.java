package frc.robot.commands.debug.odmetry;

import static frc.robot.lib.util.Constants.SemiAutoConstants.omegaMaximum;
import static frc.robot.lib.util.Constants.SemiAutoConstants.velocityMaximum;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Transform2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.wpilibj2.command.Command;
import frc.robot.RobotState;
import frc.robot.lib.limelight.LimelightConfig;
import frc.robot.lib.limelight.VisionTargetSelector;
import frc.robot.lib.util.Constants.CommandConstants;
import frc.robot.lib.util.Constants.FieldConstants;
import frc.robot.lib.util.Constants.PIDConstants;
import frc.robot.lib.util.Constants.VisionConstants;
import frc.robot.lib.util.OdomHeadingController;
import frc.robot.lib.util.PoseSelectionUtil;
import frc.robot.subsystems.SwerveSubsystem;

public class SetToTagCommand extends Command {
    private static final double kTranslationToleranceMeters = 0.05;
    private static final double kTranslationVelocityToleranceMPerSec = 0.1;
    private static final double kIntegralContributionLimit = 0.3;

    private static final double kHeadingToleranceRad = Math.toRadians(1.0);
    private static final double kHeadingVelocityToleranceRadPerSec = Math.toRadians(8.0);
    private static final double kTagStandoffDistanceMeters = 0.50;

    private final SwerveSubsystem swerve;
    private final RobotState state;
    private final VisionTargetSelector targetSelector;

    private final PIDController translationPidX =
            new PIDController(PIDConstants.kTranslationKp, PIDConstants.kTranslationKi, PIDConstants.kTranslationKd);
    private final PIDController translationPidY =
            new PIDController(PIDConstants.kTranslationKp, PIDConstants.kTranslationKi, PIDConstants.kTranslationKd);
    private final PIDController headingPid =
            new PIDController(PIDConstants.kHeadingKp, PIDConstants.kHeadingKi, PIDConstants.kHeadingKd);
    private final OdomHeadingController headingControl;

    private int lockedTagId = -1;
    private Pose2d target = null;

    public SetToTagCommand(SwerveSubsystem swerve, RobotState state) {
        this.swerve = swerve;
        this.state = state;
        this.targetSelector = new VisionTargetSelector(getEnabledVisionTables());
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
        lockedTagId = -1;
        target = null;
        resetControllers();
    }

    @Override
    public void execute() {
        if (lockedTagId < 0) {
            var best = targetSelector.selectBestObservation();
            if (best.isEmpty()) {
                swerve.driveFieldOriented(new ChassisSpeeds());
                return;
            }

            int tagId = best.get().tagId();
            Pose2d resolvedTarget = resolveTargetPose(tagId);
            if (!PoseSelectionUtil.isFinitePose(resolvedTarget)) {
                swerve.driveFieldOriented(new ChassisSpeeds());
                return;
            }

            lockedTagId = tagId;
            target = resolvedTarget;
            resetControllers(); 
        }

        var maybePose = PoseSelectionUtil.selectFinitePose(
                state.getLatestFieldToRobot(),
                state.getLatestFieldToRobotOdom());
        if (maybePose.isEmpty() || !PoseSelectionUtil.isFinitePose(target)) {
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

        if (!Double.isFinite(currentX)
                || !Double.isFinite(currentY)
                || !Double.isFinite(currentHeadingRad)) {
            stopAndReset();
            return;
        }

        double targetX = target.getX();
        double targetY = target.getY();
        double targetHeadingRad = target.getRotation().getRadians();

        double translationX = MathUtil.clamp(
                translationPidX.calculate(currentX, targetX),
                -velocityMaximum,
                velocityMaximum);
        double translationY = MathUtil.clamp(
                translationPidY.calculate(currentY, targetY),
                -velocityMaximum,
                velocityMaximum);

        var maybeHeadingResult = headingControl.calculate(currentHeadingRad, targetHeadingRad);
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

    private static String[] getEnabledVisionTables() {
        var enabled = LimelightConfig.getInstance().getEnabledLimelights().stream()
                .map(entry -> entry.table)
                .filter(table -> table != null && !table.isBlank())
                .distinct()
                .toList();
        if (!enabled.isEmpty()) {
            return enabled.toArray(String[]::new);
        }
        return new String[] {VisionConstants.kLimelightATableName, VisionConstants.kLimelightBTableName};
    }

    private static Pose2d resolveTargetPose(int tagId) {
        var maybeTagPose3d = FieldConstants.kAprilTagLayout.getTagPose(tagId);
        if (maybeTagPose3d.isPresent()) {
            Pose2d tagPose = maybeTagPose3d.get().toPose2d();
            Transform2d frontOffset = new Transform2d(
                    new Translation2d(kTagStandoffDistanceMeters, 0.0),
                    Rotation2d.kPi);
            Pose2d standoffTarget = tagPose.transformBy(frontOffset);
            if (PoseSelectionUtil.isFinitePose(standoffTarget)) {
                return standoffTarget;
            }
        }

        Pose2d legacyTarget = CommandConstants.tagToVertexMap.get(tagId);
        if (PoseSelectionUtil.isFinitePose(legacyTarget)) {
            return legacyTarget;
        }
        return null;
    }
}
