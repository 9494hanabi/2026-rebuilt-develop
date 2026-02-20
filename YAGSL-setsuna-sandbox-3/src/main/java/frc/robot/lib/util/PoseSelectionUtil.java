package frc.robot.lib.util;

import edu.wpi.first.math.geometry.Pose2d;
import java.util.Map;
import java.util.Optional;

public final class PoseSelectionUtil {
    private PoseSelectionUtil() {
        throw new UnsupportedOperationException("This is a utility class!");
    }

    public static Optional<Pose2d> selectFinitePose(
            Map.Entry<Double, Pose2d> preferred,
            Map.Entry<Double, Pose2d> fallback) {
        Pose2d preferredPose = preferred == null ? null : preferred.getValue();
        Pose2d fallbackPose = fallback == null ? null : fallback.getValue();
        return selectFinitePose(preferredPose, fallbackPose);
    }

    public static Optional<Pose2d> selectFinitePose(Pose2d preferred, Pose2d fallback) {
        if (isFinitePose(preferred)) {
            return Optional.of(preferred);
        }
        if (isFinitePose(fallback)) {
            return Optional.of(fallback);
        }
        return Optional.empty();
    }

    public static boolean isFinitePose(Pose2d pose) {
        return pose != null
                && Double.isFinite(pose.getX())
                && Double.isFinite(pose.getY())
                && Double.isFinite(pose.getRotation().getRadians());
    }
}
