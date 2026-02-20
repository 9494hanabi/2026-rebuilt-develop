package frc.robot.commands.debug.odmetry;

import static frc.robot.lib.constants.SemiAutoConstants.omegaMaximum;
import static frc.robot.lib.constants.SemiAutoConstants.velocityMaximum;
import static frc.robot.lib.constants.PIDConstants.*;
import static frc.robot.lib.constants.LogConstants.*;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj2.command.Command;

import frc.robot.RobotState;
import frc.robot.subsystems.SwerveSubsystem;
import frc.robot.lib.constants.FieldConstants;
import frc.robot.lib.util.OdomHeadingController;

public class SetZeroCommand extends Command {
    private static final double kFusedOdomTranslationMismatchMeter = 0.75;

    private static final double kFieldBoundaryMarginMeter = 0.25;
    private static final double kTargetXMeter = FieldConstants.kInitialFieldToRobotPose.getX();
    private static final double kTargetYMeter = FieldConstants.kInitialFieldToRobotPose.getY();
    private static final double kTargetHeadingRad = FieldConstants.kInitialFieldToRobotPose.getRotation().getRadians();

    private final SwerveSubsystem swerve;
    private final RobotState state;

    private final PIDController translationPidX =
        new PIDController(kTranslationKp, kTranslationKi, kTranslationKd);
    private final PIDController translationPidY =
        new PIDController(kTranslationKp, kTranslationKi, kTranslationKd);
    private final PIDController headingPid =
        new PIDController(kHeadingKp, kHeadingKi, kHeadingKd);
    private final OdomHeadingController headingControl;
    private double lastStatusLogSec = Double.NEGATIVE_INFINITY;

    public SetZeroCommand(
        SwerveSubsystem swerve,
        RobotState state
    ) {
        this.swerve = swerve;
        this.state = state;
        addRequirements(swerve);

        translationPidX.setTolerance(kTranslationToleranceMeters, kTranslationVelocityToleranceMPerSec);
        translationPidX.setIntegratorRange(-kTranslationIntegralContributionLimit, kTranslationIntegralContributionLimit);
        translationPidY.setTolerance(kTranslationToleranceMeters, kTranslationVelocityToleranceMPerSec);
        translationPidY.setIntegratorRange(-kTranslationIntegralContributionLimit, kTranslationIntegralContributionLimit);
        headingPid.disableContinuousInput();
        headingPid.setTolerance(kHeadingToleranceRad, kHeadingVelocityToleranceRadPerSec);
        headingPid.setIntegratorRange(-kHeadingIntegralContributionLimit, kHeadingIntegralContributionLimit);
        headingControl = new OdomHeadingController(headingPid, omegaMaximum);
    }

    @Override
    public void initialize() {
        translationPidX.reset();
        translationPidY.reset();
        headingControl.reset();
        lastStatusLogSec = Double.NEGATIVE_INFINITY;
        System.out.printf(
                "[SetZero] INITIALIZED:target=(%.2f, %.2f, %.2f deg)%n",
                kTargetXMeter,
                kTargetYMeter,
                Math.toDegrees(kTargetHeadingRad));
    }

    @Override
    public void execute() {
        double nowSec = Timer.getFPGATimestamp();

        // Vision融合済み
        var latest = state.getLatestFieldToRobot();

        // Vision非混入
        var latestOdom = state.getLatestFieldToRobotOdom();

        if (latest == null || latestOdom == null) {
            System.out.println("[SetZero] !!POSE IS NULL!!");
            System.out.println("          skipping");
            translationPidX.reset();
            translationPidY.reset();
            headingControl.reset();
            swerve.driveFieldOriented(new ChassisSpeeds());
            return;
        }

        double currentXMeter = latest.getValue().getX();
        double currentYMeter = latest.getValue().getY();
        double currentHeadingRad = latest.getValue().getRotation().getRadians();

        double fusedXMeter = currentXMeter;
        double fusedYMeter = currentYMeter;
        double fusedHeadingRad = currentHeadingRad;
        
        double odomXMeter = latestOdom.getValue().getX();
        double odomYMeter = latestOdom.getValue().getY();
        double odomHeadingRad = latestOdom.getValue().getRotation().getRadians();

        double fusedOdomTranslationDeltaMeter = Math.hypot(fusedXMeter - odomXMeter, fusedYMeter - odomYMeter);
        double fusedOdomHeadingDeltaRad = fusedHeadingRad - odomHeadingRad;

        // ロボットがマージン内にいるか?
        boolean fusedInField = isInsideFieldWithMargin(fusedXMeter, fusedYMeter);

        // Fused採用判定フラグ
        // ロボットがマージン内にいる && fusedOdomDeltaMeterが有限の値 && fusedOdomDeltaMeterが許容誤差の範囲内
        boolean translationFromFused =
                fusedInField
                        && Double.isFinite(fusedOdomTranslationDeltaMeter)
                        && fusedOdomTranslationDeltaMeter <= kFusedOdomTranslationMismatchMeter;
        
        // flag = 0 のときodometry(Vision非混入を採用)
        if (!translationFromFused) {
            System.out.println("[SetZero] !!FUSED POSE IS ILLEGAL!!");
            System.out.printf ("          fusedInField      = %b\n", fusedInField);
            System.out.printf ("          isFinite(delta_t) = %b\n", Double.isFinite(fusedOdomTranslationDeltaMeter));
            System.out.printf ("          in_Tolerance      = %b\n", fusedOdomTranslationDeltaMeter <= kFusedOdomTranslationMismatchMeter);
            System.out.printf( "          delta = %.2f m\n", fusedOdomTranslationDeltaMeter);
            System.out.printf( "          delta = %.2f rad\n", fusedOdomHeadingDeltaRad);
            System.out.println("[SetZero] FUSED POSE REJECTED");

            currentXMeter = odomXMeter;
            currentYMeter = odomYMeter;
        }

        // 有限かチェック
        if (!Double.isFinite(currentXMeter)
                || !Double.isFinite(currentYMeter)
                || !Double.isFinite(odomHeadingRad)) {
            System.out.println("[SetZero] !!CURRENT POSE IS ILLEGAL!!");
            System.out.printf ("          isFinite(X_current) = %b", Double.isFinite(currentXMeter));
            System.out.printf ("          isFinite(Y_current) = %b", Double.isFinite(currentYMeter));
            System.out.printf ("          isFinite(H_current) = %b", Double.isFinite(odomHeadingRad));
            System.out.println("[SetZero] ODMETRY REJECTED");

            translationPidX.reset();
            translationPidY.reset();
            headingControl.reset();
            swerve.driveFieldOriented(new ChassisSpeeds());
            return;
        }

        // velocityを計算
        double translationX = MathUtil.clamp(
                        translationPidX.calculate(currentXMeter, kTargetXMeter),
                        -velocityMaximum, velocityMaximum);

        double translationY = MathUtil.clamp(
                        translationPidY.calculate(currentYMeter, kTargetYMeter),
                        -velocityMaximum, velocityMaximum);
                        
        var maybeHeadingResult = headingControl.calculate(odomHeadingRad, kTargetHeadingRad);


        if (maybeHeadingResult.isEmpty()) {
            System.out.println("[SetZero] !!MAYBEHEADINGRESULTS IS ILLEGAL!!");
            System.out.println("          maybeHeadingResult = empty");
            System.out.println("[SetZero] ODMETRY REJECTED");

            translationPidX.reset();
            translationPidY.reset();
            headingControl.reset();
            swerve.driveFieldOriented(new ChassisSpeeds());
            return;
        }

        var headingResult = maybeHeadingResult.get();
        double omega = headingResult.omegaRadPerSec();

        // 目標地点到達フラグ
        boolean atTranslation = translationPidX.atSetpoint() && translationPidY.atSetpoint();
        boolean atHeading = headingResult.atSetpoint();

        // flag = trueならvelocity = 0へ
        if (atTranslation) {
            System.out.println("[SetZero] ROBOT AT SETPOINT");
            translationX = 0.0;
            translationY = 0.0;
        }

        if (nowSec - lastStatusLogSec >= kStatusLogPeriodSec) {
            lastStatusLogSec = nowSec;
            System.out.println("[SetZero] periodic log");
            System.out.printf ("          pos=(%.2f, %.2f, %.2f deg)\n", currentXMeter, currentYMeter, Math.toDegrees(headingResult.wrappedCurrentHeadingRad()));
            System.out.printf ("          fused=(%.2f, %.2f, %.2f)\n", fusedXMeter, fusedYMeter, fusedHeadingRad);
            System.out.printf ("          odom=(%.2f, %.2f, %.2f)\n", odomXMeter, odomYMeter, odomHeadingRad);
            System.out.printf ("          vel=(%.2f, %.2f, %.2f)\n", translationX, translationY, omega);
            System.out.printf ("          src=%s d_t=%.2f d_h=%.2f atTranslation=%b atHeading=%b%n",
                    translationFromFused ? "fused" : "odom",
                    fusedOdomTranslationDeltaMeter,
                    fusedOdomHeadingDeltaRad,
                    atTranslation,
                    atHeading);
        }

        swerve.driveFieldOriented(new ChassisSpeeds(translationX, translationY, omega));
    }

    private static boolean isInsideFieldWithMargin(double xMeter, double yMeter) {
        return xMeter >= -kFieldBoundaryMarginMeter
                && xMeter <= FieldConstants.fieldLengthMeter + kFieldBoundaryMarginMeter
                && yMeter >= -kFieldBoundaryMarginMeter
                && yMeter <= FieldConstants.fieldWidthMeter + kFieldBoundaryMarginMeter;
    }

    @Override
    public void end(boolean interrupted) {
        translationPidX.reset();
        translationPidY.reset();
        headingControl.reset();
        swerve.driveFieldOriented(new ChassisSpeeds());
        System.out.println("[SetZero] ended, interrupted=" + interrupted);
    }
}
