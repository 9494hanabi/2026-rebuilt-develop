package frc.robot.lib.util;

import static frc.robot.lib.constants.OdomConstants.*;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.controller.PIDController;

import java.util.Optional;

/**
 * odom由来のラップ角(-pi..pi)を連続角へ展開し、安定したHeading制御出力を作る補助クラス。
 */
public class OdomHeadingController {

    private final PIDController pid;
    private final double maxOmegaRadPerSec;

    private boolean hasPreviousHeading = false;
    private double previousWrappedHeadingRad = 0.0;
    private double continuousHeadingRad = 0.0;

    // コンストラクタ2
    public OdomHeadingController(
            PIDController pid,
            double maxOmegaRadPerSec) {
        this.pid = pid;
        this.maxOmegaRadPerSec = maxOmegaRadPerSec;
    }

    public void reset() {
        pid.reset();
        hasPreviousHeading = false;
        previousWrappedHeadingRad = 0.0;
        continuousHeadingRad = 0.0;
    }

    // π ~ -π に正規化された(wrapepdされた)角を受け取り、連続角(continuous)に変換する。
    /*
     * wrapped（-180°〜180°）を continuous に展開して、... -360, -180, 0, 180, 360 ... のように連続的に扱えるようにしています。
     * これでラップ境界で角度が飛ばず、PIDが安定します。
     */

    public Optional<ControlResult> calculate(double wrappedCurrentHeadingRad, double wrappedTargetHeadingRad) {
        // 現在角と目標角が有限な値かどうかをチェックしている。
        if (!Double.isFinite(wrappedCurrentHeadingRad) || !Double.isFinite(wrappedTargetHeadingRad)) {
            return Optional.empty();
        }

        // 前回呼び出しからの引き継ぎ角がない場合
        if (!hasPreviousHeading) {
            hasPreviousHeading = true;
            // 現在角を次回の呼び出しに引き継ぐ変数へ格納
            previousWrappedHeadingRad = wrappedCurrentHeadingRad;

            // 現在角をwrappedからcontinousへ展開
            continuousHeadingRad = wrappedCurrentHeadingRad;

        // 前回からの引き継ぎがある場合
        } else {
            // 直前からのΔを計算
            double delta = MathUtil.angleModulus(wrappedCurrentHeadingRad - previousWrappedHeadingRad);
            continuousHeadingRad += delta;
            previousWrappedHeadingRad = wrappedCurrentHeadingRad;

            /*
            * continuousHeadingRad = wrappedCurrentHeadingRad だと ラップ境界（±π）をまたぐ瞬間に壊れる ので、+= delta が必要です。
            *
            * previousWrappedHeadingRad は「前回の wrapped 値」
            * continuousHeadingRad は「累積した連続角（unwrapped）」
            * 普段はほぼ同じ値に見えますが、境界をまたぐと差が出ます。
            * 
            * 例（2回呼び出し）:
            * 
            * 1回目: wrapped = +179°
            * continuous = +179°
            * 2回目: wrapped = -179°（実際は +2° 回っただけ）
            * このとき

            * 代入方式: continuous = -179°（-358°ジャンプしたように見える）
            * 現在の方式:
            * delta = angleModulus(-179 - 179) = +2°
            * continuous = 179 + 2 = 181°
            * つまり += delta は「実際の回転量を連続的に積み上げる」ための処理です。
            * PIDの誤差が不連続に跳ねるのを防ぐ目的があります。
            */
        }

        double targetContinuousHeadingRad =
                continuousHeadingRad + MathUtil.angleModulus(wrappedTargetHeadingRad - wrappedCurrentHeadingRad);
        /*
         * |========continuousHeadingRad========| + |========errorRad========|
         * |==========================targetRad==============================|
         * 
         * 式:
         * targetCont = currentCont + angleModulus(targetWrap - currentWrap)
         * 
         * 境界またぎ（+179° → -179°）
         * currentWrap = +179
         * targetWrap = -179
         * currentCont = +179
         * 生差分: -179 - 179 = -358
         * angleModulus(-358) = +2
         * targetCont = 179 + 2 = 181
         * 結果: 「-358°回る」のではなく「+2°だけ回る」目標になる。
         * 
         * 逆向き境界またぎ（-179° → +179°）
         * currentWrap = -179
         * targetWrap = +179
         * currentCont = -179
         * 生差分: 179 - (-179) = +358
         * angleModulus(+358) = -2
         * targetCont = -179 + (-2) = -181
         * 結果: 「+358°回る」のではなく「-2°だけ回る」目標になる。
         * このように、境界で誤差が大ジャンプしないようにしています。
         */

        // omegaを計算
        double omega = kOdomOmegaSign
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
                && Math.abs(headingErrorRad) > kMinOmegaEnableErrorRad
                && Math.abs(omega) < kMinEffectiveOmegaRadPerSec) {
            omega = Math.copySign(kMinEffectiveOmegaRadPerSec, headingErrorRad);
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
