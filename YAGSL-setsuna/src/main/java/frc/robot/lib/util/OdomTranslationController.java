package frc.robot.lib.util;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.controller.PIDController;
import java.util.Optional;

/**
 * odom由来のラップ角(-pi..pi)を連続角へ展開し、安定したTranslation制御出力を作る補助クラス。
 */
public class OdomTranslationController {
    // デフォルトの最小の速度
    private static final double kDefaultMinEffectiveTranslationMeterPerSec = 0.05;

    // 許容する最小の誤差
    private static final double kDefaultMinTranslationEnableErrorMeter = 0.1;

    // キャリブレーションのためのハードコード
    private static final double kDefaultOdomTranslationXSign = 1.0;
    private static final double kDefaultOdomTranslationYSign = 1.0;


    private final PIDController translationXPid;
    private final PIDController translationYPid;
    private final double maxTranslationMeterPerSec;
    private final double minEffectiveTranslationMeterPerSec;
    private final double minTranslationEnableErrorMeter;
    private final double odomTranslationXSign;
    private final double odomTranslationYSign;

    private double continuousTranslationMeter = 0.0;

    public OdomTranslationController(PIDController translationXPid, PIDController translationYPid, double maxTranslationMeterPerSec) {
        // コンストラクタ2を呼んでいる
        this(
                translationXPid,
                translationYPid,
                maxTranslationMeterPerSec,
                kDefaultMinEffectiveTranslationMeterPerSec,
                kDefaultMinTranslationEnableErrorMeter,
                kDefaultOdomTranslationXSign,
                kDefaultOdomTranslationYSign);
    }

    // コンストラクタ2
    public OdomTranslationController(
            PIDController translationXPid,
            PIDController translationYPid,
            double maxTranslationMeterPerSec,
            double minEffectiveTranslationMeterPerSec,
            double minTranslationEnableErrorMeter,
            double odomTranslationXSign,
            double odomTranslationYSign) {
        this.translationXPid = translationXPid;
        this.translationYPid = translationYPid;
        this.maxTranslationMeterPerSec = maxTranslationMeterPerSec;
        this.minEffectiveTranslationMeterPerSec = minEffectiveTranslationMeterPerSec;
        this.minTranslationEnableErrorMeter = minTranslationEnableErrorMeter;
        this.odomTranslationXSign = odomTranslationXSign;
        this.odomTranslationYSign = odomTranslationYSign;
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
        // 現在値と目標値が有限な値かどうかをチェックしている。
        if (!Double.isFinite(currentTranslationXMeter)
            || !Double.isFinite(currentTranslationYMeter)
            || !Double.isFinite(targetTranslationXMeter)
            || !Double.isFinite(targetTranslationYMeter)) {
            return Optional.empty();
        }

        // TranslationXを計算
        double TranslationXMeterPerSec = odomTranslationXSign
                * MathUtil.clamp(
                        translationXPid.calculate(continuousTranslationMeter, targetTranslationXMeter),
                        -maxTranslationMeterPerSec,
                        maxTranslationMeterPerSec);
        
        // TranslationYを計算
        double TranslationYMeterPerSec = odomTranslationYSign
                * MathUtil.clamp(
                        translationXPid.calculate(continuousTranslationMeter, targetTranslationYMeter),
                        -maxTranslationMeterPerSec,
                        maxTranslationMeterPerSec);
        
        if (!Double.isFinite(TranslationXMeterPerSec) || !Double.isFinite(TranslationYMeterPerSec)) {
            return Optional.empty();
        }

        boolean xAtSetpoint = translationXPid.atSetpoint();
        boolean yAtSetpoint = translationYPid.atSetpoint();

        double TranslationXErrorMeter = targetTranslationXMeter - currentTranslationXMeter;
        double TranslationYErrorMeter = targetTranslationYMeter - currentTranslationYMeter;

        if (!xAtSetpoint
                && Math.abs(TranslationXErrorMeter) > minTranslationEnableErrorMeter
                && Math.abs(TranslationXMeterPerSec) < minEffectiveTranslationMeterPerSec) {
            TranslationXMeterPerSec = Math.copySign(minEffectiveTranslationMeterPerSec, TranslationXErrorMeter);
        }

        if (!yAtSetpoint
                && Math.abs(TranslationYErrorMeter) > minTranslationEnableErrorMeter
                && Math.abs(TranslationYMeterPerSec) < minEffectiveTranslationMeterPerSec) {
            TranslationXMeterPerSec = Math.copySign(minEffectiveTranslationMeterPerSec, TranslationYErrorMeter);
        }

        if (xAtSetpoint) {
            TranslationXMeterPerSec = 0.0;
        }
        if (yAtSetpoint) {
            TranslationYMeterPerSec = 0.0;
        }

        return Optional.of(
                new ControlResult(
                        TranslationXMeterPerSec,
                        TranslationYMeterPerSec,
                        currentTranslationXMeter,
                        currentTranslationYMeter,
                        targetTranslationXMeter,
                        targetTranslationXMeter,
                        TranslationXErrorMeter,
                        TranslationYErrorMeter,
                        xAtSetpoint,
                        yAtSetpoint));
    }

    public record ControlResult(
            double TranslationXMeterPerSec,
            double TranslationYMeterPerSec,
            double currentTranslationXMeter,
            double currentTranslationYMeter,
            double targetTranslationXMeter,
            double targetTranslationYMeter,
            double TranslationXErrorMeter,
            double TranslationYErrorMeter,
            boolean xAtSetpoint,
            boolean yAtSetpoint) {}
}
