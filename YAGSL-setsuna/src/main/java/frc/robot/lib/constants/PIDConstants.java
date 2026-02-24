package frc.robot.lib.constants;

public final class PIDConstants {

    // 調整の手順

    // 担当晴太
    // 1. まず Ki と Kd を 0 にして、Kp だけで調整する
    //   - Kp を下げていき、振動しなくなるギリギリの値を見つける
    //   - 目標に到達できるが、オーバーシュートしない程度
    // 2. Kd を少しずつ上げる
    //   - 振動を抑え、目標付近でのブレーキ効果を出す
    //   - 上げすぎるとピクピクの原因になるので注意
    // 3. 最後に Ki を少しだけ入れる
    //   - 定常偏差（目標に近いけど完全に到達しない）がある場合のみ
    //   - Ki は非常に小さい値（0.01〜0.05程度）から始める

    public static final double kHeadingKp = 5.00;
    public static final double kHeadingKd = 0.18;
    public static final double kHeadingKi = 0;


    public static final double kTranslationKp = 3;
    public static final double kTranslationKd = 0.10;
    public static final double kTranslationKi = 0;

    // 許容誤差をラジアンで表した定数。
    public static final double kHeadingToleranceRad = Math.toRadians(1.0);

    // 誤差の変化量の許容値
    public static final double kHeadingVelocityToleranceRadPerSec = Math.toRadians(8.0);

    // PIDのI値の出力上限
    public static final double kHeadingIntegralContributionLimit = 0.3;

    // 許容誤差(m)
    public static final double kTranslationToleranceMeters = 0.05;

    // 速度の変化の許容量(m/s)
    public static final double kTranslationVelocityToleranceMPerSec = 0.1;

    // I値の影響上限
    public static final double kTranslationIntegralContributionLimit = 0.3;
}
