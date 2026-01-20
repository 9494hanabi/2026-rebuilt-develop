/**
 * 2026 Season Robot Code
 *
 * CREDITS & ACKNOWLEDGEMENTS
 *
 * [ Special Thanks to the Giants ]
 * We stand on the shoulders of giants. A huge thank you to:
 * - Team 254 (The Cheesy Poofs): For their inspiring code architecture and libraries.
 * - The YAGSL Developers: Your Yet Another Generic Swerve Library made our drive system possible.
 *
 * [ Robot Members ]
 * Without the dedicated efforts of the members who built the robot and constructed the field,
 * we would not have been able to take the field in FRC.
 *
 *   [Software]
 * - Haruto, Haruta, Atsuo, Ryo, Sachi
 *
 *   [Hardware]
 * - Sena, Kim, Souraku
 *
 *   [Electronics]
 * - Oriyu, Wakana, Riko
 *
 *   [Field]
 * - Chizuru, Toma, Mocky, Moe, Shurina
 *
 * [ Outreach Members ]
 * Without the outreach members who shared our activities and brought smiles to children,
 * Hanabi could not have continued to walk its own vision.
 * - Ema, Suke, Sorano
 *
 * [ Operations Team ]
 * Without the Operations Team, our organization would not have taken shape,
 * nor would it have survived for three years.
 * - Rikka, Minorin, Niko, Kokone, Gal, Naoto
 *
 * [ Alumni ]
 * To our alumni: we stand on the path you built.
 * With gratitude to:
 * - Isshin, Satoshi, Yui, Enen, Misaki, Erena, Tomoko, Mayumi, Kizuki,
 *   Mamoru, Shuta, Soya, Haru, Ayuma, Yoshika, China, Kairi
 *
 * [ Supporters ]
 * To our supporters: thank you for supporting our activities.
 * With gratitude to:
 * - Mr. Machida
 *
 * [ Our Captain ]
 * To our captain: thank you for leading this team.
 * - Motoki Tatsuta
 *
 * We express our deepest respect for your dedication and honor you here.
 *
 * "May the FRC community and Team 9494 continue to ignite the spark of innovation."
 * 「FRCコミュニティーとTeam 9494が、革新の火種を絶やさず発展し続けることを願って。」
 */

// === 担当者 ===
// はるた
//

package frc.robot;
import edu.wpi.first.wpilibj.RobotBase;
public final class Main {
  private Main() {}
  public static void main(String... args) {
    RobotBase.startRobot(Robot::new);
  }
}
