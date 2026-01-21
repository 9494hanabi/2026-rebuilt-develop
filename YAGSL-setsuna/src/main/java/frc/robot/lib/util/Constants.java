// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.lib.util;

import edu.wpi.first.math.util.Units;

// === 担当者 ===
// ひなた
//

public final class Constants {
  public static class OperatorConstants {
    public static final int kDriverControllerPort = 0;
    public static final double DEADBAND = 0.08;
  }

  public static class VisionConstants {
    public static final boolean useMegaTag2 = true;
    public static final String limelightName = "limelight";
  }

  public static class FieldConstants {
    public static final double fieldLengthMeter = 16.54;
    public static final double kMidlineBufferMeter = 5.0;
  }

  public static class SemiAutoConstants {
    // 速度係数 (m/s)/m
    public static final double translationGain = 1.2;

    // クランプ m/s
    public static final double velocityMaximum = 1.5;

    // 回転速度係数 (rad/s)/rad
    public static final double angularGain = 3.0;

    // クランプ rad/s
    public static final double omegaMaximum = 2.5;

    // 許容誤差
    public static final double planeDeadbandMeter = 0.03;
    public static final double thetaDeadbandDeg = 1;
    public static final double thetaDeadbandRad = Math.toRadians(thetaDeadbandDeg);
    
  }
  public static final double maxSpeed  = Units.feetToMeters(4.5);
}
