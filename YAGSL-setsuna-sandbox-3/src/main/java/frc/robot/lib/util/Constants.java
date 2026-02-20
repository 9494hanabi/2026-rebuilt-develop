package frc.robot.lib.util;

import edu.wpi.first.apriltag.AprilTagFieldLayout;
import edu.wpi.first.apriltag.AprilTagFields;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.util.Units;
import java.util.Map;

public final class Constants {
    private Constants() {}

    public static class OperatorConstants {
        public static final int kDriverControllerPort = 0;
        public static final double DEADBAND = 0.08;
    }

    public static class VisionConstants {
        public static final boolean useMegaTag2 = true;
        public static final double kLargeVariance = 1e6;

        public static final int kMegatag1XStdDevIndex = 0;
        public static final int kMegatag1YStdDevIndex = 1;
        public static final int kMegatag1YawStdDevIndex = 5;

        public static final int kMegatag2XStdDevIndex = 6;
        public static final int kMegatag2YStdDevIndex = 7;
        public static final int kMegatag2YawStdDevIndex = 11;

        public static final int kExpectedStdDevArrayLength = 12;

        public static final String kLimelightATableName = "limelight-lb";
        public static final String kLimelightBTableName = "limelight-fc";
    }

    public static class FieldConstants {
        public static final double kMidlineBufferMeter = 5.0;
        public static final boolean useTestField = true;

        private static final String testFieldFmapPath = "maps/fiducials.fmap";

        public static final AprilTagFieldLayout kAprilTagLayout =
                useTestField
                        ? FmapFieldLayoutLoader.loadFromDeploy(testFieldFmapPath)
                        : AprilTagFieldLayout.loadField(AprilTagFields.kDefaultField);

        public static final double fieldLengthMeter = kAprilTagLayout.getFieldLength();
        public static final double fieldWidthMeter = kAprilTagLayout.getFieldWidth();

        public static final Pose2d kInitialFieldToRobotPose =
                new Pose2d(fieldLengthMeter / 2.0, fieldWidthMeter / 2.0, Rotation2d.fromDegrees(0.0));
    }

    public static class SemiAutoConstants {
        public static final double translationGain = 1.2;
        public static final double velocityMaximum = 1.5;
        public static final double angularGain = 3.0;
        public static final double omegaMaximum = 4.0;
        public static final double planeDeadbandMeter = 0.03;
        public static final double thetaDeadbandDeg = 1;
        public static final double thetaDeadbandRad = Math.toRadians(thetaDeadbandDeg);
        public static final double kDriveOnTagSpeedMetersPerSec = 1.0;
    }

    public static class PIDConstants {
        public static final double kHeadingKp = 5.00;
        public static final double kHeadingKd = 0.18;
        public static final double kHeadingKi = 0;

        public static final double kTranslationKp = 3;
        public static final double kTranslationKd = 0.10;
        public static final double kTranslationKi = 0;
    }

    public static class CommandConstants {
        private static final double kMaxX = FieldConstants.fieldLengthMeter;
        private static final double kMinX = 0.0;
        private static final double kMaxY = FieldConstants.fieldWidthMeter;
        private static final double kMinY = 0.0;
        private static final double kInnerOffset = 0.3535533905932738;
        private static final double kNearMaxX = kMaxX - kInnerOffset;
        private static final double kNearMinX = kMinX + kInnerOffset;
        private static final double kNearMaxY = kMaxY - kInnerOffset;
        private static final double kNearMinY = kMinY + kInnerOffset;

        public static final Map<Integer, Pose2d> tagToVertexMap = Map.ofEntries(
                Map.entry(1, new Pose2d(kMaxX, kNearMaxY, Rotation2d.fromDegrees(-135.0))),
                Map.entry(2, new Pose2d(kNearMaxX, kMaxY, Rotation2d.fromDegrees(-135.0))),
                Map.entry(3, new Pose2d(kNearMinX, kMaxY, Rotation2d.fromDegrees(-45.0))),
                Map.entry(4, new Pose2d(kMinX, kNearMaxY, Rotation2d.fromDegrees(-45.0))),
                Map.entry(5, new Pose2d(kMaxX, kNearMinY, Rotation2d.fromDegrees(135.0))),
                Map.entry(6, new Pose2d(kNearMaxX, kMinY, Rotation2d.fromDegrees(135.0))),
                Map.entry(7, new Pose2d(kNearMinX, kMinY, Rotation2d.fromDegrees(45.0))),
                Map.entry(8, new Pose2d(kMinX, kNearMinY, Rotation2d.fromDegrees(45.0))));
    }

    public static final double maxSpeed = Units.feetToMeters(4.5);
}
