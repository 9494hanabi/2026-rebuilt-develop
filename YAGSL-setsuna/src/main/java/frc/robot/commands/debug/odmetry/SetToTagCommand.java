package frc.robot.commands.debug.odmetry;

import static frc.robot.lib.constants.SemiAutoConstants.omegaMaximum;
import static frc.robot.lib.constants.SemiAutoConstants.velocityMaximum;
import static frc.robot.lib.constants.PIDConstants.*;
import static frc.robot.lib.constants.LogConstants.*;
import frc.robot.lib.constants.commandconstants.SetToTagCommandConstants;
import frc.robot.lib.constants.FieldConstants;
import frc.robot.lib.constants.VisionConstants;

import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj2.command.Command;

import frc.robot.RobotState;
import frc.robot.subsystems.SwerveSubsystem;
import frc.robot.lib.limelight.LimelightConfig;
import frc.robot.lib.limelight.VisionTargetSelector;
import frc.robot.lib.util.OdomHeadingController;
import frc.robot.lib.util.OdomTranslationController;

public class SetToTagCommand extends Command {

    private static final double kSaturationEpsilon = 1e-9;
    private static final boolean kEnableVerboseStatusLog = false;
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
    private final OdomTranslationController translationControl;

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
        headingPid.setIntegratorRange(-kHeadingIntegralContributionLimit, kHeadingIntegralContributionLimit);
        headingControl = new OdomHeadingController(headingPid, omegaMaximum);
        translationControl =
                new OdomTranslationController(
                        translationPidX,
                        translationPidY,
                        velocityMaximum);
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
        translationControl.reset();
        lastStatusLogSec = Double.NEGATIVE_INFINITY;
        lastSearchLogSec = Double.NEGATIVE_INFINITY;
        lastWarningLogSec = Double.NEGATIVE_INFINITY;
        lastAcceptedFusedTranslationPose = null;
        lastAcceptedFusedTimestampSec = Double.NaN;
        System.out.println("[SetToTag] INITIALIZED: searching for tag...");
    }

    @Override
    public void execute() {
        double nowSec = Timer.getFPGATimestamp();

        // フェーズ1: タグ未ロック → 探索
        if (lockedTagId < 0) {
            // 最有力の観測を取得
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
                System.out.printf("[SetToTag] !!TAG ID=%d HAS NO VALID TARGET IGNORING!!%n", tagId);
                System.out.printf("            (src=%s ta=%.2f)%n",
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

            // exclusivetag -> そのタグを含む観測以外を無視
            state.setExclusiveTag(lockedTagId);

            translationPidX.reset();
            translationPidY.reset();
            headingControl.reset();

            System.out.println("[SetToTag] TAG LOCKED ");
            System.out.printf(             "tagId=%d src=%s ta=%.2f target=(%.2f, %.2f, %.2f deg)%n",
                    tagId,
                    lockedSourceTable,
                    lockedSourceArea,
                    target.getX(),
                    target.getY(),
                    target.getRotation().getDegrees());
        }

        // フェーズ2: タグロック済み -> ドライブ
        var currentObservation = targetSelector.observationForTag(lockedTagId);
        if (currentObservation.isPresent()) {
            lockedSourceTable = currentObservation.get().tableName();
            lockedSourceArea = currentObservation.get().area();
        }

        var latest = state.getLatestFieldToRobot();
        var latestOdom = state.getLatestFieldToRobotOdom();

        // nullチェック
        if (latest == null || latestOdom == null) {
            if (nowSec - lastWarningLogSec >= kWarningLogPeriodSec) {
                lastWarningLogSec = nowSec;
                System.out.println("[SetToTag] !!POSE IS NULL!!");
                System.out.println("           skipping");
            }
            translationPidX.reset();
            translationPidY.reset();
            headingControl.reset();
            translationControl.reset();
            swerve.driveFieldOriented(new ChassisSpeeds());
            return;
        }

        // Vision融合済み観測
        double fusedX = latest.getValue().getX();
        double fusedY = latest.getValue().getY();
        double fusedHeadingRad = latest.getValue().getRotation().getRadians();

        // Vision未混入観測
        double odomX = latestOdom.getValue().getX();
        double odomY = latestOdom.getValue().getY();
        double odomHeadingRad = latestOdom.getValue().getRotation().getRadians();

        // デルタを計算
        double fusedOdomTranslationDelta = Math.hypot(fusedX - odomX, fusedY - odomY);

        // 誤差許容範囲 フラグ
        boolean fusedOdomTranslationConsistent =
                fusedOdomTranslationDelta <= kFusedOdomTranslationMismatchMeter;

        // フィールド外ポーズ フラグ
        boolean fusedInField = isInsideFieldWithMargin(fusedX, fusedY);

        // フィールド + マージン 外ポーズ フラグ
        boolean fusedUseCandidateInField =
                isInsideFieldWithCustomMargin(
                        fusedX, fusedY, kFusedUseBoundaryMarginMeter);

        // ポーズジャンプ受け入れ フラグ
        boolean fusedStepAccepted = false;

        // 融合ポーズが有限の値か判定
        if (fusedUseCandidateInField
                && Double.isFinite(fusedX)
                && Double.isFinite(fusedY)
                && Double.isFinite(nowSec)) {
            // 前回採用が無ければ無条件受け入れ
            if (lastAcceptedFusedTranslationPose == null || !Double.isFinite(lastAcceptedFusedTimestampSec)) {
                fusedStepAccepted = true;
            } else {
                double dtSec = nowSec - lastAcceptedFusedTimestampSec;

                // dtが有限の値か判定
                if (Double.isFinite(dtSec) && dtSec > 0.0) {

                    // 受け入れる最大のポーズジャンプ幅を定義
                    // = 基礎値 + 前回採用から離れるほど大きくなる比例値
                    double maxAllowedStepMeter = kFusedStepSlackMeter + kMaxFusedStepMetersPerSec * dtSec;

                    // ジャンプ幅を計算
                    double fusedStepMeter = Math.hypot(
                            fusedX - lastAcceptedFusedTranslationPose.getX(),
                            fusedY - lastAcceptedFusedTranslationPose.getY());

                    // ジャンプ受け入れフラグ
                    fusedStepAccepted = Double.isFinite(fusedStepMeter) && fusedStepMeter <= maxAllowedStepMeter;
                } else {
                    // dtSec が NaN / +Infinity / -Infinity
                    // dtSec == 0.0
                    // dtSec < 0.0（時間が逆行した状態）
                    // などの場合。
                    fusedStepAccepted = false;
                }
            }
        }

        // 融合値受け入れフラグ
        //
        // 予測値が
        //      フィールド + マージン 内 &&
        //      ポーズジャンプ許容範囲内 &&
        //      オドメトリとの誤差が許容範囲内
        //
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

        // オドメトリが有限の値か判定
        if (target == null
                || !Double.isFinite(currentX)
                || !Double.isFinite(currentY)
                || !Double.isFinite(fusedHeadingRad)
                || !Double.isFinite(odomX)
                || !Double.isFinite(odomY)
                || !Double.isFinite(currentHeadingRad)
                || !Double.isFinite(target.getX())
                || !Double.isFinite(target.getY())
                || !Double.isFinite(target.getRotation().getRadians())) {
            if (nowSec - lastWarningLogSec >= kWarningLogPeriodSec) {
                lastWarningLogSec = nowSec;
                System.out.println("[SetToTag] !!INVALID!!");
                System.out.printf ("           current=(%s, %s, %s) target=%s\n", currentX, currentY, currentHeadingRad, target);
                System.out.println("[SetToTag] ROBOT STOPPED");
            }
            translationPidX.reset();
            translationPidY.reset();
            headingControl.reset();
            translationControl.reset();
            swerve.driveFieldOriented(new ChassisSpeeds());
            return;
        }

        double targetX = target.getX();
        double targetY = target.getY();
        double targetHeadingRad = target.getRotation().getRadians();
        double errorX = targetX - currentX;
        double errorY = targetY - currentY;
        double errorNorm = Math.hypot(errorX, errorY);

        var maybeTranslationResult =
                translationControl.calculate(currentX, currentY, targetX, targetY);
        if (maybeTranslationResult.isEmpty()) {
            if (nowSec - lastWarningLogSec >= kWarningLogPeriodSec) {
                lastWarningLogSec = nowSec;
                System.out.println("[SetToTag] !!INVALID!!");
                System.out.println("           translation control input = empty");
                System.out.println("[SetToTag] ROBOT STOPPED");
            }
            translationPidX.reset();
            translationPidY.reset();
            headingControl.reset();
            translationControl.reset();
            swerve.driveFieldOriented(new ChassisSpeeds());
            return;
        }
        var translationResult = maybeTranslationResult.get();
        double translationX = translationResult.translationXMeterPerSec();
        double translationY = translationResult.translationYMeterPerSec();

        var maybeHeadingResult = headingControl.calculate(currentHeadingRad, targetHeadingRad);
        if (maybeHeadingResult.isEmpty()) {
            if (nowSec - lastWarningLogSec >= kWarningLogPeriodSec) {
                lastWarningLogSec = nowSec;
                System.out.println("[SetToTag] !!INVALID!!");
                System.out.println("           heading control input = empty");
                System.out.println("[SetToTag] ROBOT STOPPED");

            }
            translationPidX.reset();
            translationPidY.reset();
            headingControl.reset();
            translationControl.reset();
            swerve.driveFieldOriented(new ChassisSpeeds());
            return;
        }
        var headingResult = maybeHeadingResult.get();
        double omega = headingResult.omegaRadPerSec();

        boolean atTranslation = translationResult.xAtSetpoint() && translationResult.yAtSetpoint();
        boolean atHeading = headingResult.atSetpoint();
        if (atTranslation) {
            System.out.println("[SetToTag] ROBOT AT SETPOINT");
            translationX = 0.0;
            translationY = 0.0;
        }

        if (!Double.isFinite(translationX) || !Double.isFinite(translationY) || !Double.isFinite(omega)) {
            if (nowSec - lastWarningLogSec >= kWarningLogPeriodSec) {
                lastWarningLogSec = nowSec;
                System.out.println("[SetToTag] !!INVALID!!");
                System.out.printf ("           command output -> vx=%s vy=%s omega=%s%n", translationX, translationY, omega);
                System.out.println("[SetToTag] ROBOT STOPPED");
            }

            translationPidX.reset();
            translationPidY.reset();
            headingControl.reset();
            translationControl.reset();

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
            System.out.printf("[SetToTag] tag=%d age=%.2fs src=%s ta=%.2f "
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
                    translationX, saturatedX ? "*" : "",
                    translationY, saturatedY ? "*" : "",
                    omega, saturatedOmega ? "*" : "",
                    measuredRobotSpeeds.vxMetersPerSecond,
                    measuredRobotSpeeds.vyMetersPerSecond,
                    measuredRobotSpeeds.omegaRadiansPerSecond,
                    atTranslation,
                    atHeading);
            Pose2d yagslPose = swerve.getSwerveDrive().getPose();
            System.out.printf("[SetToTag][pose] yagsl=%s fused=(%.2f, %.2f, %.1f deg) "
                            + "odom=(%.2f, %.2f, %.1f deg) selected=(%.2f, %.2f, %.1f deg) "
                            + "target=(%.2f, %.2f, %.1f deg) deltaFO=%.2f useFused=%b%n",
                    formatPose(yagslPose),
                    fusedX,
                    fusedY,
                    Math.toDegrees(fusedHeadingRad),
                    odomX,
                    odomY,
                    Math.toDegrees(odomHeadingRad),
                    currentX,
                    currentY,
                    Math.toDegrees(currentHeadingRad),
                    targetX,
                    targetY,
                    Math.toDegrees(targetHeadingRad),
                    fusedOdomTranslationDelta,
                    translationFromFused);
            if (kEnableVerboseStatusLog) {

                //
                System.out.println("[SetToTag][detail]");
                System.out.printf ("                   poseF=(%.2f, %.2f, %.2f deg)\n",
                        fusedX,
                        fusedY,
                        Math.toDegrees(fusedHeadingRad));
                System.out.printf ("                   poseO=(%.2f, %.2f, %.2f deg)\n",
                        odomX,
                        odomY,
                        Math.toDegrees(odomHeadingRad));
                System.out.printf ("                   target=(%.2f, %.2f, %.2f deg)%n",
                        targetX,
                        targetY,
                        Math.toDegrees(targetHeadingRad));
                System.out.printf ("                   heading=(wrapCur %.2f deg, wrapTgt %.2f deg, "
                                 + "contCur %.2f deg, contTgt %.2f deg)%n",
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
        translationControl.reset();
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

    // 有効なLimelightを返すメソッド
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

    // TagIdを引数として、そのtagに対して目標とするべき座標を返すメソッド
    private static Pose2d resolveTargetPose(int tagId) {
        Pose2d targetPose2d = SetToTagCommandConstants.tagToVertexMap.get(tagId);
        if (isFinitePose(targetPose2d)) {
            return targetPose2d;
        }
        return null;
    }

    private static boolean isFinitePose(Pose2d pose) {
        return pose != null
                && Double.isFinite(pose.getX())
                && Double.isFinite(pose.getY())
                && Double.isFinite(pose.getRotation().getRadians());
    }

    private static String formatPose(Pose2d pose) {
        if (pose == null) {
            return "null";
        }
        return String.format(
                "(%.2f, %.2f, %.1f deg)",
                pose.getX(),
                pose.getY(),
                pose.getRotation().getDegrees());
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
