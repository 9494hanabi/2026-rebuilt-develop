package frc.robot.lib.util;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.controller.PIDController;

import static frc.robot.lib.constants.OdomConstants.*;
import java.util.Optional;

/**
 * 安定したTranslation制御出力を作る補助クラス。
 */
public class OdomTranslationController {

    private final PIDController translationXPid;
    private final PIDController translationYPid;
    private final double maxTranslationMeterPerSec;

    public OdomTranslationController(
            PIDController translationXPid,
            PIDController translationYPid,
            double maxTranslationMeterPerSec) {
        this.translationXPid = translationXPid;
        this.translationYPid = translationYPid;
        this.maxTranslationMeterPerSec = maxTranslationMeterPerSec;
    }

    public void reset() {
        translationXPid.reset();
        translationYPid.reset();
    }

    public Optional<ControlResult> calculate(
        double currentTranslationXMeter,
        double currentTranslationYMeter,
        double targetTranslationXMeter,
        double targetTranslationYMeter
    ) {
        if (!Double.isFinite(currentTranslationXMeter)
            || !Double.isFinite(currentTranslationYMeter)
            || !Double.isFinite(targetTranslationXMeter)
            || !Double.isFinite(targetTranslationYMeter)) {
            return Optional.empty();
        }

        double translationXMeterPerSec = kOdomTranslationXSign
                * MathUtil.clamp(
                        translationXPid.calculate(currentTranslationXMeter, targetTranslationXMeter),
                        -maxTranslationMeterPerSec,
                        maxTranslationMeterPerSec);

        double translationYMeterPerSec = kOdomTranslationYSign
                * MathUtil.clamp(
                        translationYPid.calculate(currentTranslationYMeter, targetTranslationYMeter),
                        -maxTranslationMeterPerSec,
                        maxTranslationMeterPerSec);

        if (!Double.isFinite(translationXMeterPerSec) || !Double.isFinite(translationYMeterPerSec)) {
            return Optional.empty();
        }

        boolean xAtSetpoint = translationXPid.atSetpoint();
        boolean yAtSetpoint = translationYPid.atSetpoint();

        double translationXErrorMeter = targetTranslationXMeter - currentTranslationXMeter;
        double translationYErrorMeter = targetTranslationYMeter - currentTranslationYMeter;

        if (!xAtSetpoint
                && Math.abs(translationXErrorMeter) > kMinTranslationEnableErrorMeter
                && Math.abs(translationXMeterPerSec) < kMinEffectiveTranslationMeterPerSec) {
            translationXMeterPerSec = Math.copySign(kMinEffectiveTranslationMeterPerSec, translationXErrorMeter);
        }

        if (!yAtSetpoint
                && Math.abs(translationYErrorMeter) > kMinTranslationEnableErrorMeter
                && Math.abs(translationYMeterPerSec) < kMinEffectiveTranslationMeterPerSec) {
            translationYMeterPerSec = Math.copySign(kMinEffectiveTranslationMeterPerSec, translationYErrorMeter);
        }

        if (xAtSetpoint) {
            translationXMeterPerSec = 0.0;
        }
        if (yAtSetpoint) {
            translationYMeterPerSec = 0.0;
        }

        return Optional.of(
                new ControlResult(
                        translationXMeterPerSec,
                        translationYMeterPerSec,
                        currentTranslationXMeter,
                        currentTranslationYMeter,
                        targetTranslationXMeter,
                        targetTranslationYMeter,
                        translationXErrorMeter,
                        translationYErrorMeter,
                        xAtSetpoint,
                        yAtSetpoint));
    }

    public record ControlResult(
            double translationXMeterPerSec,
            double translationYMeterPerSec,
            double currentTranslationXMeter,
            double currentTranslationYMeter,
            double targetTranslationXMeter,
            double targetTranslationYMeter,
            double translationXErrorMeter,
            double translationYErrorMeter,
            boolean xAtSetpoint,
            boolean yAtSetpoint) {}
}
