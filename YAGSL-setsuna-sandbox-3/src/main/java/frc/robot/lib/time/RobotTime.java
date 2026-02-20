
//
// getTimestampSeconds() -> 現在時刻(second)
//

package frc.robot.lib.time;

import edu.wpi.first.wpilibj.Timer;

// === 担当者 ===
// ひなた
//

public class RobotTime {
    public static double getTimestampSeconds() {
        return Timer.getFPGATimestamp();
    }
}
