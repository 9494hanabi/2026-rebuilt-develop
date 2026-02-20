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
import frc.robot.subsystems.SwerveSubsystem;
import frc.robot.lib.util.Constants.FieldConstants;
import frc.robot.lib.util.Constants.PIDConstants;

/**
 * デバッグ用: タグ3の正面0.5mへドライブするコマンド。
 * タグ座標は FieldConstants.kAprilTagLayout から取得し、目標をログに出力する。
 */
public class DriveToTag3FrontCommand extends Command {
    private static final int kTargetTagId = 3;
    private static final double kStandoffDistance = 0.5;

    private static final double kTranslationToleranceMeters = 0.05;
    private static final double kTranslationVelocityToleranceMPerSec = 0.1;
    private static final double kIntegralContributionLimit = 0.3;

    private static final double kHeadingToleranceRad = Math.toRadians(1.0);
    private static final double kHeadingVelocityToleranceRadPerSec = Math.toRadians(8.0);

    private final SwerveSubsystem swerve;
    private final RobotState state;

    private final PIDController translationPidX =
        new PIDController(PIDConstants.kTranslationKp, PIDConstants.kTranslationKi, PIDConstants.kTranslationKd);
    private final PIDController translationPidY =
        new PIDController(PIDConstants.kTranslationKp, PIDConstants.kTranslationKi, PIDConstants.kTranslationKd);
    private final PIDController headingPid =
        new PIDController(PIDConstants.kHeadingKp, PIDConstants.kHeadingKi, PIDConstants.kHeadingKd);

    private Pose2d target = null;

    public DriveToTag3FrontCommand(SwerveSubsystem swerve, RobotState state) {
        this.swerve = swerve;
        this.state = state;
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
        translationPidX.reset();
        translationPidY.reset();
        headingPid.reset();

        // タグ3の絶対座標を取得
        var maybeTagPose3d = FieldConstants.kAprilTagLayout.getTagPose(kTargetTagId);
        if (maybeTagPose3d.isEmpty()) {
            System.out.printf("[DriveToTag3Front] ERROR: tagId=%d not found in layout!%n", kTargetTagId);
            target = null;
            return;
        }

        Pose2d tagPose = maybeTagPose3d.get().toPose2d();
        System.out.printf("[DriveToTag3Front] tagPose=(%.3f, %.3f, %.1f deg)%n",
                tagPose.getX(), tagPose.getY(), tagPose.getRotation().getDegrees());

        // タグのローカル座標系で kStandoffDistance 前方 + 180°回転(タグを向く)
        Transform2d frontOffset = new Transform2d(
            new Translation2d(kStandoffDistance, 0), Rotation2d.kPi);
        target = tagPose.transformBy(frontOffset);

        System.out.printf("[DriveToTag3Front] target=(%.3f, %.3f, %.1f deg) standoff=%.2fm%n",
                target.getX(), target.getY(), target.getRotation().getDegrees(), kStandoffDistance);
    }

    @Override
    public void execute() {
        if (target == null) {
            swerve.driveFieldOriented(new ChassisSpeeds());
            return;
        }

        var latest = state.getLatestFieldToRobot();
        if (latest == null) {
            System.out.println("[DriveToTag3Front] pose is null, skipping");
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

        System.out.printf("[DriveToTag3Front] pos=(%.2f, %.2f, %.1f deg) err=(%.2f, %.2f) atTrans=%b atHead=%b%n",
                currentX, currentY, Math.toDegrees(currentHeadingRad),
                target.getX() - currentX, target.getY() - currentY,
                atTranslation, atHeading);

        swerve.driveFieldOriented(new ChassisSpeeds(translationX, translationY, omega));
    }

    @Override
    public void end(boolean interrupted) {
        translationPidX.reset();
        translationPidY.reset();
        headingPid.reset();
        swerve.driveFieldOriented(new ChassisSpeeds());
        System.out.printf("[DriveToTag3Front] ended, interrupted=%b%n", interrupted);
    }
}
