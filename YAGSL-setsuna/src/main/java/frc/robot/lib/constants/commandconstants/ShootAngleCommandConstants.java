package frc.robot.lib.constants.commandconstants;

public final class ShootAngleCommandConstants {
  private ShootAngleCommandConstants() {}

  // 旧deg設定(80/100/220)を現在の換算値でRot化した仮値
  // TODO: 実機で最終調整
  public static final double kL1Rot = -0.25;
  public static final double kL2Rot = -0.1; // 晴太
  public static final double kL3Rot = 0;
  public static final double kL4Rot = 0.2;
  public static final double kL5Rot = 0.25;
  public static final double kL6Rot = 0.3;
  public static final double kL7Rot = 0.35;
  public static final double kL8Rot = 0.4;
  public static final double kL9Rot = 0.45;
  public static final double kL10Rot = 0.5;
  public static final double kL11Rot = 0.55;
  public static final double kL12Rot = 0.6;
  public static final double kL13Rot = 0.65;
  public static final double kL14Rot = 0.7;
  public static final double kL15Rot = 0.75;
  public static final double kL16Rot = 0.8;

  public static final int kL1Index = 0;
  public static final int kL2Index = 1;
  public static final int kL3Index = 2;
  public static final int kL4Index = 3;
  public static final int kL5Index = 4;
  public static final int kL6Index = 5;
  public static final int kL7Index = 6;
  public static final int kL8Index = 7;
  public static final int kL9Index = 8;
  public static final int kL10Index = 9;
  public static final int kL11Index = 10;
  public static final int kL12Index = 11;
  public static final int kL13Index = 12;
  public static final int kL14Index = 13;
  public static final int kL15Index = 14;
  public static final int kL16Index = 15;

  public static final double[] kPresetRot = {
    kL1Rot,
    kL2Rot,
    kL3Rot,
    kL4Rot,
    kL5Rot,
    kL6Rot,
    kL7Rot,
    kL8Rot,
    kL9Rot,
    kL10Rot,
    kL11Rot,
    kL12Rot,
    kL13Rot,
    kL14Rot,
    kL15Rot,
    kL16Rot
  };

  public static final int kDefaultPresetIndex = kL1Index; // L1

  // Autoで狙うタグ7用（旧設定との互換）
  public static final int kAutoTag7PresetIndex = kL2Index;
  public static final double kAutoTag7Rot = kPresetRot[kAutoTag7PresetIndex];

  // 以下Auto shoot profileの定数
  // 開始直後のショット（startshootcommand）用
  public static final double kAutoStartShotAngleRot = kL4Rot;
  public static final double kAutoStartShotRps = 70.0;
  public static final double kAutoStartShotSec = 5.0;

  // アウトポスト用ショット（outpostshootcommand）用
  public static final double kAutoOutpostShotAngleRot = kL8Rot;
  public static final double kAutoOutpostShotRps = 90.0;
  public static final double kAutoOutpostShotSec = 5.0;
}
