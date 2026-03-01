package frc.robot.lib.constants;

import edu.wpi.first.units.measure.Velocity;

public final class ShootAngleConstants {
  private ShootAngleConstants() {}

  // 旧deg設定(80/100/220)を現在の換算値でRot化した仮値
  // TODO: 実機で最終調整
  public static final double kL1Rot = -0.3; //せな
  public static final double kL2Rot = -0.4; //晴太
  public static final double kL3Rot = -0.5;

  public static final int kL1Index = 0;
  public static final int kL2Index = 1;
  public static final int kL3Index = 2;

  public static final double[] kPresetRot = {kL1Rot, kL2Rot, kL3Rot};
  public static final int kDefaultPresetIndex = kL1Index; // L1

  //Auto用
  public static final int kAutoTag7PresetIndex = kL2Index; // L2にしているけどL1でもL3でもいい
  public static final double kAutoTag7Rot = kPresetRot[kAutoTag7PresetIndex];
}
