package frc.robot.lib.util;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.controller.PIDController;
import java.util.Optional;

/**
 * odom由来のラップ角(-pi..pi)を連続角へ展開し、安定したHeading制御出力を作る補助クラス。
 */
public class OdomHeadingController {
    private static final double kDefaultMinEffectiveOmegaRadPerSec = 0.35;
    private static final double kDefaultMinOmegaEnableErrorRad = Math.toRadians(4.0);
    private static final double kDefaultOdomOmegaSign = -1.0;

    private final PIDController pid;
    private final double maxOmegaRadPerSec;
    private final double minEffectiveOmegaRadPerSec;
    private final double minOmegaEnableErrorRad;
    private final double odomOmegaSign;

    private boolean hasPreviousHeading = false;
    private double previousWrappedHeadingRad = 0.0;
    private double continuousHeadingRad = 0.0;

    public OdomHeadingController(PIDController pid, double maxOmegaRadPerSec) {
        this(
                pid,
                maxOmegaRadPerSec,
                kDefaultMinEffectiveOmegaRadPerSec,
                kDefaultMinOmegaEnableErrorRad,
                kDefaultOdomOmegaSign);
    }

    public OdomHeadingController(
            PIDController pid,
            double maxOmegaRadPerSec,
            double minEffectiveOmegaRadPerSec,
            double minOmegaEnableErrorRad,
            double odomOmegaSign) {
        this.pid = pid;
        this.maxOmegaRadPerSec = maxOmegaRadPerSec;
        this.minEffectiveOmegaRadPerSec = minEffectiveOmegaRadPerSec;
        this.minOmegaEnableErrorRad = minOmegaEnableErrorRad;
        this.odomOmegaSign = odomOmegaSign;
    }

    public void reset() {
        pid.reset();
        hasPreviousHeading = false;
        previousWrappedHeadingRad = 0.0;
        continuousHeadingRad = 0.0;
    }

    public Optional<ControlResult> calculate(double wrappedCurrentHeadingRad, double wrappedTargetHeadingRad) {
        if (!Double.isFinite(wrappedCurrentHeadingRad) || !Double.isFinite(wrappedTargetHeadingRad)) {
            return Optional.empty();
        }

        if (!hasPreviousHeading) {
            hasPreviousHeading = true;
            previousWrappedHeadingRad = wrappedCurrentHeadingRad;
            continuousHeadingRad = wrappedCurrentHeadingRad;
        } else {
            double delta = MathUtil.angleModulus(wrappedCurrentHeadingRad - previousWrappedHeadingRad);
            continuousHeadingRad += delta;
            previousWrappedHeadingRad = wrappedCurrentHeadingRad;
        }

        // 現在角に最も近い等価角へ目標角を持ち上げる。
        double targetContinuousHeadingRad =
                continuousHeadingRad + MathUtil.angleModulus(wrappedTargetHeadingRad - wrappedCurrentHeadingRad);

        double omega = odomOmegaSign
                * MathUtil.clamp(
                        pid.calculate(continuousHeadingRad, targetContinuousHeadingRad),
                        -maxOmegaRadPerSec,
                        maxOmegaRadPerSec);
        if (!Double.isFinite(omega)) {
            return Optional.empty();
        }

        boolean atSetpoint = pid.atSetpoint();
        double headingErrorRad = targetContinuousHeadingRad - continuousHeadingRad;
        if (!atSetpoint
                && Math.abs(headingErrorRad) > minOmegaEnableErrorRad
                && Math.abs(omega) < minEffectiveOmegaRadPerSec) {
            omega = Math.copySign(minEffectiveOmegaRadPerSec, headingErrorRad);
        }
        if (atSetpoint) {
            omega = 0.0;
        }

        return Optional.of(
                new ControlResult(
                        omega,
                        wrappedCurrentHeadingRad,
                        wrappedTargetHeadingRad,
                        continuousHeadingRad,
                        targetContinuousHeadingRad,
                        headingErrorRad,
                        atSetpoint));
    }

    public record ControlResult(
            double omegaRadPerSec,
            double wrappedCurrentHeadingRad,
            double wrappedTargetHeadingRad,
            double continuousCurrentHeadingRad,
            double continuousTargetHeadingRad,
            double headingErrorRad,
            boolean atSetpoint) {}
}
