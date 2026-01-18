/*----------------------------------------------------------------------------*/
/* Copyright (c) 2026 FRC Team 9494 Hanabi. All Rights Reserved.              */
/* Open Source Software - may be modified and shared by FRC teams.            */
/*----------------------------------------------------------------------------*/

/**
 * 2026 Season Robot Code
 * * CREDITS & ACKNOWLEDGEMENTS:
 * * [Special Thanks to the Giants]
 * We stand on the shoulders of giants. A huge thank you to:
 * - Team 254 (The Cheesy Poofs): For their inspiring code architecture and libraries.
 * - YAGSL Developers: Your Yet Another Generic Swerve Library made our drive system possible.
 * * [Team 9494 Hanabi Software Team]
 * Developed with passion by the students of Team 9494.
 * * [Vision]
 * "May the FRC community and Team 9494 continue to ignite the spark of innovation."
 * 「FRCコミュニティーとTeam 9494が、革新の火種を絶やさず発展し続けることを願って。」
 * */

package frc.robot;
import edu.wpi.first.wpilibj.RobotBase;
public final class Main {
  private Main() {}
  public static void main(String... args) {
    RobotBase.startRobot(Robot::new);
  }
}
