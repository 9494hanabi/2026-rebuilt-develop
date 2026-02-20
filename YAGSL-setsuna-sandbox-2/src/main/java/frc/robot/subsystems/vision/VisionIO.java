package frc.robot.subsystems.vision;

// use FiducalObservation.java
import edu.wpi.first.math.geometry.Transform2d;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Pose2d;
import java.util.ArrayList;
import java.util.List;

// === 担当 ===
// ひなた
//

public interface VisionIO {

    class VisionIOInputs {
        public static class CameraInputs {
            public String name = "";
            public String tableName = "";
            public Transform2d robotToCamera = Transform2d.kZero;
            public boolean connected;
            public boolean seesTarget;
            public FiducialObservation[] fiducialObservations = new FiducialObservation[0];
            public MegatagPoseEstimate megatagPoseEstimate;
            public MegatagPoseEstimate megatag2PoseEstimate;
            public int megatag2Count;
            public int megatagCount;
            public Pose3d pose3d;
            public int primaryTagId = -1;
            public Pose2d botPoseTargetSpace = null;
            public double[] standardDeviations = 
                    new double[12];
                    // [MT1x, MT1y, MT1z, MT1roll, MT1pitch, MT1Yaw, MT2x, Mt2y, MT2roll, MT2pitch, MT2yaw]
        }

        public List<CameraInputs> cameras = new ArrayList<>();
    }

    void readInputs(VisionIOInputs inputs);
}
