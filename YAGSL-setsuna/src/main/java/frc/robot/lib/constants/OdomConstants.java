package frc.robot.lib.constants;

public class OdomConstants {
    // =================Heading=================
    // デフォルトの最小の角速度
    public static final double kMinEffectiveOmegaRadPerSec = 0.35;

    // 許容する最小の角度誤差
    public static final double kMinOmegaEnableErrorRad = Math.toRadians(4.0);

    // キャリブレーションのためのハードコード
    public static final double kOdomOmegaSign = -1.0;

    // =================Translation=================
    // デフォルトの最小の速度
    public static final double kMinEffectiveTranslationMeterPerSec = 0.05;

    // 許容する最小の誤差
    public static final double kMinTranslationEnableErrorMeter = 0.1;

    // キャリブレーションのための速度符号
    public static final double kOdomTranslationXSign = 1.0;
    public static final double kOdomTranslationYSign = 1.0;
}
