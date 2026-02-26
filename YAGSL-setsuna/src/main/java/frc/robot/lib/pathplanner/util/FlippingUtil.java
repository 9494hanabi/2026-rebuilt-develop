package frc.robot.lib.pathplanner.util;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.math.util.Units;

public class FlippingUtil {
    public static FieldSymmetry symmetryType = FieldSymmetry.kRotational;

    public static double fieldSizeX = Units.feetToMeters(57.573);

    public static double fieldSizeY = Units.feetToMeters(26.417);

    public enum FieldSymmetry {
        kRotational,
        kMirrored
    }

    public static Translation2d flipFieldPosition(Translation2d pos) {
        return switch (symmetryType) {
            case kMirrored -> new Translation2d(fieldSizeX - pos.getX(), pos.getY());
            case kRotational -> new Translation2d(fieldSizeX - pos.getX(), fieldSizeY - pos.getX());
        };
    }

    public static Rotation2d flipFieldRotation(Rotation2d rotation) {
        return switch (symmetryType) {
            case kMirrored -> Rotation2d.kPi.minus(rotation);
            case kRotational -> rotation.minus(Rotation2d.kPi);
        };
    }

    public static Pose2d flipFieldPose(Pose2d pose) {
        return new Pose2d(
                flipFieldPosition(pose.getTranslation()), flipFieldRotation(pose.getRotation()));
    }

    public static ChassisSpeeds flipFieldSpeeds(ChassisSpeeds fieldSpeeds) {
        return switch (symmetryType) {
            case kMirrored -> new ChassisSpeeds(
                -fieldSpeeds.vxMetersPerSecond,
                fieldSpeeds.vyMetersPerSecond,
                -fieldSpeeds.omegaRadiansPerSecond);
            case kRotational -> new ChassisSpeeds(
                -fieldSpeeds.vxMetersPerSecond,
                -fieldSpeeds.vyMetersPerSecond,
                fieldSpeeds.omegaRadiansPerSecond);
        };
    }

    public static double[] flipFeedforwards(double[] feedforwards) {
        return switch (symmetryType) {
            case kMirrored -> {
                if (feedforwards.length == 4) {
                    yield new double[] {
                        feedforwards[1], feedforwards[0], feedforwards[3], feedforwards[2]
                    };
                } else if (feedforwards.length == 2) {
                    yield new double[] {feedforwards[1], feedforwards[0]};
                }
                yield feedforwards;
            }
            case kRotational -> feedforwards;
        };
    }

    public static double[] flipFeedforwardXs(double[] feedforwardXs) {
        return flipFeedforwards(feedforwardXs);
    }

    public static double[] flipFeedforwardYs(double[] feedforwardYs) {
        var flippedFeedforwardYs = flipFeedforwards(feedforwardYs);
        return switch (symmetryType) {
            case kMirrored -> {
                for (int i = 0; i < flippedFeedforwardYs.length; ++i) {
                    flippedFeedforwardYs[i] *= -1;
                }
                yield flippedFeedforwardYs;
            }
            case kRotational -> flippedFeedforwardYs;
        };
    }
}
