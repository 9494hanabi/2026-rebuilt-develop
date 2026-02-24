// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.lib.constants;

import edu.wpi.first.math.util.Units;

// === 担当者 ===
// ひなた
//

public final class Constants {
  public static class OperatorConstants {
    public static final int kDriverControllerPort = 0;      // ドライバー用コントローラのUSBポート番号
    public static final double DEADBAND = 0.08;             // スティック入力の無効域（微小入力を無視）
  }




  public static final double maxSpeed  = 2.5; // 最大走行速度[m/s]
}
