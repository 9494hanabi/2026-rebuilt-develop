package frc.robot.subsystems.vision;

import edu.wpi.first.math.Matrix;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.numbers.N1;
import edu.wpi.first.math.numbers.N3;


// === 担当者 ===
// ひなた
//

public class VisionFieldPoseEstimate {
    

    private final Pose2d visionRobotPoseMeters;
    private final double timestampSeconds;

    // 信頼度を標準偏差で表現している。
    private final Matrix<N3, N1> visionMeasurementStdDevs;
    
    private final int numTags;

    public VisionFieldPoseEstimate(
        Pose2d visionRobotPoseMeters,
        double timestampSeconds,
        Matrix<N3, N1> visionMeasurementStdDev,
        int numTags
    ) {
        this.visionRobotPoseMeters = visionRobotPoseMeters;
        this.timestampSeconds = timestampSeconds;
        this.visionMeasurementStdDevs = visionMeasurementStdDev;
        this.numTags = numTags;
    }

    public Pose2d getVisionRobotPoseMeters() {
        return visionRobotPoseMeters;
    }

    public double getTimestampSeconds() {
        return timestampSeconds;
    }

    public Matrix<N3, N1> getVisionMeasurementsStdDevs() {
        return visionMeasurementStdDevs;
    }

    public int getNumTags() {
        return numTags;
    }
}
