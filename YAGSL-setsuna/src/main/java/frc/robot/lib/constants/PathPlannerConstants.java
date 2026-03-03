package frc.robot.lib.constants;

import com.pathplanner.lib.path.PathConstraints;

// === 担当者 ===
// ひなた
//

public final class PathPlannerConstants {
    private PathPlannerConstants() {}

    public static final double kDefaultPathfindMaxVelocityMps = TeleopConstants.maxSpeed;
    public static final double kDefaultPathfindMaxAccelerationMpsSq = 4.0;
    public static final double kDefaultPathfindMaxAngularVelocityRadPerSec = Math.PI;
    public static final double kDefaultPathfindMaxAngularAccelerationRadPerSecSq = 4.0 * Math.PI;

    public static final PathConstraints kDefaultPathfindingConstraints =
            new PathConstraints(
                    kDefaultPathfindMaxVelocityMps,
                    kDefaultPathfindMaxAccelerationMpsSq,
                    kDefaultPathfindMaxAngularVelocityRadPerSec,
                    kDefaultPathfindMaxAngularAccelerationRadPerSecSq);
}
