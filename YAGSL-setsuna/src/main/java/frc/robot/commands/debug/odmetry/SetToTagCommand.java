package frc.robot.commands.debug.odmetry;

import static frc.robot.lib.util.Constants.SemiAutoConstants.omegaMaximum;
import static frc.robot.lib.util.Constants.SemiAutoConstants.velocityMaximum;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.wpilibj2.command.Command;

import frc.robot.RobotState;
import frc.robot.subsystems.SwerveSubsystem;
import frc.robot.lib.limelight.VisionTargetSelector;
import frc.robot.lib.util.Constants.CommandConstants;
import frc.robot.lib.util.Constants.PIDConstants;
import frc.robot.lib.util.Constants.VisionConstants;

public class SetToTagCommand extends Command {
    private static final double kTranslationToleranceMeters = 0.05;
    private static final double kTranslationVelocityToleranceMPerSec = 0.1;
    private static final double kIntegralContributionLimit = 0.3;

    private static final double kHeadingToleranceRad = Math.toRadians(1.0);
    private static final double kHeadingVelocityToleranceRadPerSec = Math.toRadians(8.0);

    private final SwerveSubsystem swerve;
    private final RobotState state;
    private final VisionTargetSelector targetSelector;

    private final PIDController translationPidX =
        new PIDController(PIDConstants.kTranslationKp, PIDConstants.kTranslationKi, PIDConstants.kTranslationKd);
    private final PIDController translationPidY =
        new PIDController(PIDConstants.kTranslationKp, PIDConstants.kTranslationKi, PIDConstants.kTranslationKd);
    private final PIDController headingPid =
        new PIDController(PIDConstants.kHeadingKp, PIDConstants.kHeadingKi, PIDConstants.kHeadingKd);

    private int lockedTagId = -1;
    private Pose2d target = null;

    public SetToTagCommand(
        SwerveSubsystem swerve,
        RobotState state
    ) {
        this.swerve = swerve;
        this.state = state;
        this.targetSelector = new VisionTargetSelector(
            VisionConstants.kLimelightATableName,
            VisionConstants.kLimelightBTableName);
        addRequirements(swerve);

        translationPidX.setTolerance(kTranslationToleranceMeters, kTranslationVelocityToleranceMPerSec);
        translationPidX.setIntegratorRange(-kIntegralContributionLimit, kIntegralContributionLimit);
        translationPidY.setTolerance(kTranslationToleranceMeters, kTranslationVelocityToleranceMPerSec);
        translationPidY.setIntegratorRange(-kIntegralContributionLimit, kIntegralContributionLimit);
        headingPid.enableContinuousInput(-Math.PI, Math.PI);
        headingPid.setTolerance(kHeadingToleranceRad, kHeadingVelocityToleranceRadPerSec);
        headingPid.setIntegratorRange(-kIntegralContributionLimit, kIntegralContributionLimit);
    }

    @Override
    public void initialize() {
        lockedTagId = -1;
        target = null;
        translationPidX.reset();
        translationPidY.reset();
        headingPid.reset();
        System.out.println("[SetToTag] initialized, searching for tag...");
    }

    @Override
    public void execute() {
        // フェーズ1: タグ未ロック → 探索
        if (lockedTagId < 0) {
            var best = targetSelector.selectBestObservation();
            if (best.isEmpty()) {
                System.out.println("[SetToTag] searching...");
                swerve.driveFieldOriented(new ChassisSpeeds());
                return;
            }

            int tagId = best.get().tagId();
            Pose2d vertex = CommandConstants.tagToVertexMap.get(tagId);
            if (vertex == null) {
                System.out.printf("[SetToTag] tagId=%d not in tagToVertexMap, ignoring%n", tagId);
                swerve.driveFieldOriented(new ChassisSpeeds());
                return;
            }

            lockedTagId = tagId;
            target = vertex;
            translationPidX.reset();
            translationPidY.reset();
            headingPid.reset();
            System.out.printf("[SetToTag] locked tagId=%d target=(%.2f, %.2f, %.2f deg)%n",
                    tagId, target.getX(), target.getY(), target.getRotation().getDegrees());
        }

        // フェーズ2: タグロック済み → ドライブ
        var latest = state.getLatestFieldToRobot();
        if (latest == null) {
            System.out.println("[SetToTag] pose is null, skipping");
            translationPidX.reset();
            translationPidY.reset();
            headingPid.reset();
            swerve.driveFieldOriented(new ChassisSpeeds());
            return;
        }

        double currentX = latest.getValue().getX();
        double currentY = latest.getValue().getY();
        double currentHeadingRad = latest.getValue().getRotation().getRadians();

        double translationX = MathUtil.clamp(
                        translationPidX.calculate(currentX, target.getX()),
                        -velocityMaximum, velocityMaximum);

        double translationY = MathUtil.clamp(
                        translationPidY.calculate(currentY, target.getY()),
                        -velocityMaximum, velocityMaximum);

        double omega = MathUtil.clamp(
                        headingPid.calculate(currentHeadingRad, target.getRotation().getRadians()),
                        -omegaMaximum, omegaMaximum);

        boolean atTranslation = translationPidX.atSetpoint() && translationPidY.atSetpoint();
        boolean atHeading = headingPid.atSetpoint();
        if (atTranslation) {
            translationX = 0.0;
            translationY = 0.0;
        }
        if (atHeading) {
            omega = 0.0;
        }

        System.out.printf("[SetToTag] tag=%d pos=(%.2f, %.2f, %.2f deg) vel=(%.2f, %.2f, %.4f) atTrans=%b atHead=%b%n",
                lockedTagId, currentX, currentY, Math.toDegrees(currentHeadingRad),
                translationX, translationY, omega, atTranslation, atHeading);

        swerve.driveFieldOriented(new ChassisSpeeds(translationX, translationY, omega));
    }

    @Override
    public void end(boolean interrupted) {
        translationPidX.reset();
        translationPidY.reset();
        headingPid.reset();
        swerve.driveFieldOriented(new ChassisSpeeds());
        System.out.printf("[SetToTag] ended lockedTag=%d, interrupted=%b%n", lockedTagId, interrupted);
    }
}
