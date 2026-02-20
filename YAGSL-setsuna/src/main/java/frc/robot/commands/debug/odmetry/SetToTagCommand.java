package frc.robot.commands.debug.odmetry;

import static frc.robot.lib.constants.SemiAutoConstants.omegaMaximum;
import static frc.robot.lib.constants.SemiAutoConstants.velocityMaximum;
import static frc.robot.lib.constants.PIDConstants.*;
import static frc.robot.lib.constants.LogConstants.*;

import frc.robot.lib.constants.commandconstants.SetToTagCommandConstants;
import frc.robot.lib.constants.FieldConstants;
import frc.robot.lib.constants.VisionConstants;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Transform2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj2.command.Command;

import frc.robot.RobotState;
import frc.robot.subsystems.SwerveSubsystem;
import frc.robot.lib.limelight.LimelightConfig;
import frc.robot.lib.limelight.VisionTargetSelector;
import frc.robot.lib.util.OdomHeadingController;

public class SetToTagCommand extends Command {
    private static final double kSaturationEpsilon = 1e-9;
    private static final boolean kEnableVerboseStatusLog = false;
    private static final double kTagStandoffDistanceMeters = 0.50;
    private static final double kFusedOdomTranslationMismatchMeter = 0.75;
    private static final double kFieldBoundaryMarginMeter = 0.25;
    private static final double kFusedUseBoundaryMarginMeter = 1.0;
    private static final double kMaxFusedStepMetersPerSec = 4.0;
    private static final double kFusedStepSlackMeter = 0.20;

    private final SwerveSubsystem swerve;
    private final RobotState state;
    private final VisionTargetSelector targetSelector;

    private final PIDController translationPidX =
        new PIDController(kTranslationKp, kTranslationKi, kTranslationKd);
    private final PIDController translationPidY =
        new PIDController(kTranslationKp, kTranslationKi, kTranslationKd);
    private final PIDController headingPid =
        new PIDController(kHeadingKp, kHeadingKi, kHeadingKd);
    private final OdomHeadingController headingControl;

    private int lockedTagId = -1;
    private Pose2d target = null;
    private String lockedSourceTable = "";
    private double lockedSourceArea = Double.NaN;
    private double lockAcquiredTimestampSec = Double.NEGATIVE_INFINITY;
    private double lastStatusLogSec = Double.NEGATIVE_INFINITY;
    private double lastSearchLogSec = Double.NEGATIVE_INFINITY;
    private double lastWarningLogSec = Double.NEGATIVE_INFINITY;
    private Pose2d lastAcceptedFusedTranslationPose = null;
    private double lastAcceptedFusedTimestampSec = Double.NaN;

    public SetToTagCommand(
        SwerveSubsystem swerve,
        RobotState state
    ) {
        this.swerve = swerve;
        this.state = state;
        this.targetSelector = new VisionTargetSelector(getEnabledVisionTables());
        addRequirements(swerve);

        translationPidX.setTolerance(kTranslationToleranceMeters, kTranslationVelocityToleranceMPerSec);
        translationPidX.setIntegratorRange(-kTranslationIntegralContributionLimit, kTranslationIntegralContributionLimit);
        translationPidY.setTolerance(kTranslationToleranceMeters, kTranslationVelocityToleranceMPerSec);
        translationPidY.setIntegratorRange(-kTranslationIntegralContributionLimit, kTranslationIntegralContributionLimit);
        headingPid.disableContinuousInput();
        headingPid.setTolerance(kHeadingToleranceRad, kHeadingVelocityToleranceRadPerSec);
        headingPid.setIntegratorRange(-kTranslationIntegralContributionLimit, kTranslationIntegralContributionLimit);
        headingControl = new OdomHeadingController(headingPid, omegaMaximum);
    }

    @Override
    public void initialize() {
        lockedTagId = -1;
        target = null;
        lockedSourceTable = "";
        lockedSourceArea = Double.NaN;
        lockAcquiredTimestampSec = Double.NEGATIVE_INFINITY;
        state.clearExclusiveTag();
        translationPidX.reset();
        translationPidY.reset();
        headingControl.reset();
        lastStatusLogSec = Double.NEGATIVE_INFINITY;
        lastSearchLogSec = Double.NEGATIVE_INFINITY;
        lastWarningLogSec = Double.NEGATIVE_INFINITY;
        lastAcceptedFusedTranslationPose = null;
        lastAcceptedFusedTimestampSec = Double.NaN;
        System.out.println("[SetToTag] initialized, searching for tag...");
    }

    @Override
    public void execute() {
        double nowSec = Timer.getFPGATimestamp();

        // フェーズ1: タグ未ロック → 探索
        if (lockedTagId < 0) {
            var best = targetSelector.selectBestObservation();
            if (best.isEmpty()) {
                if (nowSec - lastSearchLogSec >= kSearchLogPeriodSec) {
                    lastSearchLogSec = nowSec;
                    System.out.println("[SetToTag] searching... (no valid observation)");
                }
                swerve.driveFieldOriented(new ChassisSpeeds());
                return;
            }

            var bestObservation = best.get();
            int tagId = bestObservation.tagId();
            Pose2d resolvedTarget = resolveTargetPose(tagId);
            if (resolvedTarget == null) {
                System.out.printf(
                        "[SetToTag] tagId=%d has no valid target, ignoring (src=%s ta=%.2f)%n",
                        tagId,
                        bestObservation.tableName(),
                        bestObservation.area());
                swerve.driveFieldOriented(new ChassisSpeeds());
                return;
            }

            lockedTagId = tagId;
            target = resolvedTarget;
            lockedSourceTable = bestObservation.tableName();
            lockedSourceArea = bestObservation.area();
            lockAcquiredTimestampSec = nowSec;
            state.setExclusiveTag(lockedTagId);
            translationPidX.reset();
            translationPidY.reset();
            headingControl.reset();
            System.out.printf(
                    "[SetToTag] locked tagId=%d src=%s ta=%.2f target=(%.2f, %.2f, %.2f deg)%n",
                    tagId,
                    lockedSourceTable,
                    lockedSourceArea,
                    target.getX(),
                    target.getY(),
                    target.getRotation().getDegrees());
        }

        // フェーズ2: タグロック済み → ドライブ
        var currentObservation = targetSelector.observationForTag(lockedTagId);
        if (currentObservation.isPresent()) {
            lockedSourceTable = currentObservation.get().tableName();
            lockedSourceArea = currentObservation.get().area();
        }

        var latest = state.getLatestFieldToRobot();
        var latestOdom = state.getLatestFieldToRobotOdom();
        if (latest == null || latestOdom == null) {
            if (nowSec - lastWarningLogSec >= kWarningLogPeriodSec) {
                lastWarningLogSec = nowSec;
                System.out.println("[SetToTag] pose is null, skipping");
            }
            translationPidX.reset();
            translationPidY.reset();
            headingControl.reset();
            swerve.driveFieldOriented(new ChassisSpeeds());
            return;
        }

        double fusedX = latest.getValue().getX();
        double fusedY = latest.getValue().getY();
        double fusedHeadingRad = latest.getValue().getRotation().getRadians();
        double odomX = latestOdom.getValue().getX();
        double odomY = latestOdom.getValue().getY();
        double odomHeadingRad = latestOdom.getValue().getRotation().getRadians();
        double fusedOdomTranslationDelta = Math.hypot(fusedX - odomX, fusedY - odomY);
        boolean fusedOdomTranslationConsistent =
                fusedOdomTranslationDelta <= kFusedOdomTranslationMismatchMeter;
        boolean fusedInField = isInsideFieldWithMargin(fusedX, fusedY);
        boolean fusedUseCandidateInField =
                isInsideFieldWithCustomMargin(
                        fusedX, fusedY, kFusedUseBoundaryMarginMeter);
        boolean fusedStepAccepted = false;
        if (fusedUseCandidateInField
                && Double.isFinite(fusedX)
                && Double.isFinite(fusedY)
                && Double.isFinite(nowSec)) {
            if (lastAcceptedFusedTranslationPose == null || !Double.isFinite(lastAcceptedFusedTimestampSec)) {
                fusedStepAccepted = true;
            } else {
                double dtSec = nowSec - lastAcceptedFusedTimestampSec;
                if (Double.isFinite(dtSec) && dtSec > 0.0) {
                    double maxAllowedStepMeter = kFusedStepSlackMeter + kMaxFusedStepMetersPerSec * dtSec;
                    double fusedStepMeter = Math.hypot(
                            fusedX - lastAcceptedFusedTranslationPose.getX(),
                            fusedY - lastAcceptedFusedTranslationPose.getY());
                    fusedStepAccepted = Double.isFinite(fusedStepMeter) && fusedStepMeter <= maxAllowedStepMeter;
                } else {
                    fusedStepAccepted = true;
                }
            }
        }

        boolean translationFromFused =
                fusedUseCandidateInField
                        && fusedStepAccepted
                        && fusedOdomTranslationConsistent;
        if (translationFromFused) {
            lastAcceptedFusedTranslationPose = latest.getValue();
            lastAcceptedFusedTimestampSec = nowSec;
        }

        // 平面位置は絶対座標を重視してfused優先、headingは安定性重視でodom姿勢を使う。
        double currentX = translationFromFused ? fusedX : odomX;
        double currentY = translationFromFused ? fusedY : odomY;
        double currentHeadingRad = odomHeadingRad;

        if (!Double.isFinite(currentX)
                || !Double.isFinite(currentY)
                || !Double.isFinite(fusedHeadingRad)
                || !Double.isFinite(odomX)
                || !Double.isFinite(odomY)
                || !Double.isFinite(currentHeadingRad)
                || target == null
                || !Double.isFinite(target.getX())
                || !Double.isFinite(target.getY())
                || !Double.isFinite(target.getRotation().getRadians())) {
            if (nowSec - lastWarningLogSec >= kWarningLogPeriodSec) {
                lastWarningLogSec = nowSec;
                System.out.printf(
                        "[SetToTag] invalid pose/target -> stop. current=(%s, %s, %s) target=%s%n",
                        currentX, currentY, currentHeadingRad, target);
            }
            translationPidX.reset();
            translationPidY.reset();
            headingControl.reset();
            swerve.driveFieldOriented(new ChassisSpeeds());
            return;
        }

        double targetX = target.getX();
        double targetY = target.getY();
        double targetHeadingRad = target.getRotation().getRadians();
        double errorX = targetX - currentX;
        double errorY = targetY - currentY;
        double errorNorm = Math.hypot(errorX, errorY);

        double translationX = MathUtil.clamp(
                        translationPidX.calculate(currentX, targetX),
                        -velocityMaximum, velocityMaximum);

        double translationY = MathUtil.clamp(
                        translationPidY.calculate(currentY, targetY),
                        -velocityMaximum, velocityMaximum);

        var maybeHeadingResult = headingControl.calculate(currentHeadingRad, targetHeadingRad);
        if (maybeHeadingResult.isEmpty()) {
            if (nowSec - lastWarningLogSec >= kWarningLogPeriodSec) {
                lastWarningLogSec = nowSec;
                System.out.println("[SetToTag] invalid heading control input -> stop");
            }
            translationPidX.reset();
            translationPidY.reset();
            headingControl.reset();
            swerve.driveFieldOriented(new ChassisSpeeds());
            return;
        }
        var headingResult = maybeHeadingResult.get();
        double omega = headingResult.omegaRadPerSec();

        boolean atTranslation = translationPidX.atSetpoint() && translationPidY.atSetpoint();
        boolean atHeading = headingResult.atSetpoint();
        if (atTranslation) {
            translationX = 0.0;
            translationY = 0.0;
        }

        if (!Double.isFinite(translationX) || !Double.isFinite(translationY) || !Double.isFinite(omega)) {
            if (nowSec - lastWarningLogSec >= kWarningLogPeriodSec) {
                lastWarningLogSec = nowSec;
                System.out.printf(
                        "[SetToTag] invalid command output -> stop. vx=%s vy=%s omega=%s%n",
                        translationX, translationY, omega);
            }
            translationPidX.reset();
            translationPidY.reset();
            headingControl.reset();
            swerve.driveFieldOriented(new ChassisSpeeds());
            return;
        }

        if (nowSec - lastStatusLogSec >= kStatusLogPeriodSec) {
            lastStatusLogSec = nowSec;
            double lockAgeSec = lockAcquiredTimestampSec > 0.0 ? (nowSec - lockAcquiredTimestampSec) : Double.NaN;
            double headingErrorRad = headingResult.headingErrorRad();
            boolean saturatedX = Math.abs(translationX) >= (velocityMaximum - kSaturationEpsilon);
            boolean saturatedY = Math.abs(translationY) >= (velocityMaximum - kSaturationEpsilon);
            boolean saturatedOmega = Math.abs(omega) >= (omegaMaximum - kSaturationEpsilon);
            ChassisSpeeds measuredRobotSpeeds = swerve.getSwerveDrive().getRobotVelocity();
            System.out.printf(
                    "[SetToTag] tag=%d age=%.2fs src=%s ta=%.2f "
                            + "err=(%.2f, %.2f | %.2f m, %.2f deg) "
                            + "poseSrc=%s fO=%.2f fOOk=%b inField=%b stepOk=%b "
                            + "cmd=(%.2f%s, %.2f%s, %.4f%s) "
                            + "meas=(%.2f, %.2f, %.3f) atTrans=%b atHead=%b%n",
                    lockedTagId,
                    lockAgeSec,
                    lockedSourceTable,
                    lockedSourceArea,
                    errorX,
                    errorY,
                    errorNorm,
                    Math.toDegrees(headingErrorRad),
                    translationFromFused ? "fused" : "odom",
                    fusedOdomTranslationDelta,
                    fusedOdomTranslationConsistent,
                    fusedInField,
                    fusedStepAccepted,
                    translationX,
                    saturatedX ? "*" : "",
                    translationY,
                    saturatedY ? "*" : "",
                    omega,
                    saturatedOmega ? "*" : "",
                    measuredRobotSpeeds.vxMetersPerSecond,
                    measuredRobotSpeeds.vyMetersPerSecond,
                    measuredRobotSpeeds.omegaRadiansPerSecond,
                    atTranslation,
                    atHeading);
            if (kEnableVerboseStatusLog) {
                System.out.printf(
                        "[SetToTag][detail] poseF=(%.2f, %.2f, %.2f deg) poseO=(%.2f, %.2f, %.2f deg)%n",
                        fusedX,
                        fusedY,
                        Math.toDegrees(fusedHeadingRad),
                        odomX,
                        odomY,
                        Math.toDegrees(odomHeadingRad));
                System.out.printf(
                        "[SetToTag][detail] target=(%.2f, %.2f, %.2f deg) "
                                + "heading=(wrapCur %.2f deg, wrapTgt %.2f deg, contCur %.2f deg, contTgt %.2f deg)%n",
                        targetX,
                        targetY,
                        Math.toDegrees(targetHeadingRad),
                        Math.toDegrees(headingResult.wrappedCurrentHeadingRad()),
                        Math.toDegrees(headingResult.wrappedTargetHeadingRad()),
                        Math.toDegrees(headingResult.continuousCurrentHeadingRad()),
                        Math.toDegrees(headingResult.continuousTargetHeadingRad()));
            }
        }
        
        swerve.driveFieldOriented(new ChassisSpeeds(translationX, translationY, omega));
    }

    @Override
    public void end(boolean interrupted) {
        translationPidX.reset();
        translationPidY.reset();
        headingControl.reset();
        state.clearExclusiveTag();
        swerve.driveFieldOriented(new ChassisSpeeds());
        System.out.printf(
                "[SetToTag] ended lockedTag=%d src=%s ta=%.2f lockAge=%.2fs interrupted=%b%n",
                    lockedTagId,
                lockedSourceTable,
                    lockedSourceArea,
                lockAcquiredTimestampSec > 0.0 ? (Timer.getFPGATimestamp() - lockAcquiredTimestampSec) : Double.NaN,
                interrupted);
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
            if (isFinitePose(standoffTarget)) {
                return standoffTarget;
            }
        }

        Pose2d legacyTarget = SetToTagCommandConstants.tagToVertexMap.get(tagId);
        if (isFinitePose(legacyTarget)) {
            return legacyTarget;
        }
        return null;
    }

    private static boolean isFinitePose(Pose2d pose) {
        return pose != null
                && Double.isFinite(pose.getX())
                && Double.isFinite(pose.getY())
                && Double.isFinite(pose.getRotation().getRadians());
    }

    private static boolean isInsideFieldWithMargin(double xMeter, double yMeter) {
        return xMeter >= -kFieldBoundaryMarginMeter
                && xMeter <= FieldConstants.fieldLengthMeter + kFieldBoundaryMarginMeter
                && yMeter >= -kFieldBoundaryMarginMeter
                && yMeter <= FieldConstants.fieldWidthMeter + kFieldBoundaryMarginMeter;
    }

    private static boolean isInsideFieldWithCustomMargin(
            double xMeter,
            double yMeter,
            double marginMeter) {
        return xMeter >= -marginMeter
                && xMeter <= FieldConstants.fieldLengthMeter + marginMeter
                && yMeter >= -marginMeter
                && yMeter <= FieldConstants.fieldWidthMeter + marginMeter;
    }
}
