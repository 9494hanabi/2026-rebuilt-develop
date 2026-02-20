package frc.robot.lib.constants;

public final class SemiAutoConstants {
    // 速度係数 (m/s)/m
    public static final double translationGain = 1.2; // 目標位置誤差に対する並進ゲイン

    // クランプ m/s
    public static final double velocityMaximum = 1.5; // 並進速度の上限[m/s]

    // 回転速度係数 (rad/s)/rad
    public static final double angularGain = 3.0; // 目標角度誤差に対する回転ゲイン

    // クランプ rad/s
    public static final double omegaMaximum = 4.0; // 回転速度の上限[rad/s]

    // 許容誤差
    public static final double planeDeadbandMeter = 0.03;

    // 位置誤差のデッドバンド[m]
    public static final double thetaDeadbandDeg = 1;

    // 角度誤差のデッドバンド[deg]
    public static final double thetaDeadbandRad = Math.toRadians(thetaDeadbandDeg); // 角度誤差のデッドバンド[rad]

    // タグ検出時に一定方向へ進む速度
    public static final double kDriveOnTagSpeedMetersPerSec = 1.0;

}
