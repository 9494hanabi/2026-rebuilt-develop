package frc.robot.subsystems.vision;

import frc.robot.RobotState;
import frc.robot.lib.limelight.LimelightConfig;
import frc.robot.lib.limelight.LimelightConfig.LimelightEntry;
import frc.robot.lib.limelight.LimelightHelpers;
import frc.robot.lib.time.RobotTime;
import frc.robot.lib.util.Constants.VisionConstants;

import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Transform2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.networktables.DoubleArrayEntry;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.networktables.TimestampedDoubleArray;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class VisionIOHardwareLimelight implements VisionIO {
    private static final double HEARTBEAT_TIMEOUT_SEC = 0.5;
    private static final double[] DEFAULT_STDDEVS;
    static {
        DEFAULT_STDDEVS = new double[VisionConstants.kExpectedStdDevArrayLength];
        Arrays.fill(DEFAULT_STDDEVS, 1.0);
    }

    private static class CameraHandle {
        final String name;
        final String tableName;
        final NetworkTable table;
        final Transform2d robotToCamera;
        final double[] cameraPose;
        final double stdDevScale;
        final int pipeline;

        double lastHeartbeat = Double.NaN;
        double lastHeartbeatChangeSec = Double.NEGATIVE_INFINITY;

        CameraHandle(LimelightEntry entry) {
            this.name = entry.name == null ? "" : entry.name;
            this.tableName = entry.table == null ? "" : entry.table;
            this.table = NetworkTableInstance.getDefault().getTable(this.tableName);

            double robotToCameraX = entry.robotToCamera == null ? 0.0 : entry.robotToCamera.x;
            // JSONはWPILib座標系で定義する:
            // +X=forward, +Y=left, yaw+CCW
            double robotToCameraYWpi = entry.robotToCamera == null ? 0.0 : entry.robotToCamera.y;
            double robotToCameraYawDegWpi =
                    entry.robotToCamera == null ? 0.0 : entry.robotToCamera.yawDeg;
            this.robotToCamera =
                    new Transform2d(
                            new Translation2d(robotToCameraX, robotToCameraYWpi),
                            Rotation2d.fromDegrees(robotToCameraYawDegWpi));

            double cameraHeight = entry.cameraPose == null ? 0.0 : entry.cameraPose.heightMeters;
            double cameraPitchDeg = entry.cameraPose == null ? 0.0 : entry.cameraPose.pitchDeg;
            // Limelight RobotSpaceは+Y=rightのため符号を変換する。
            double limelightRightMeters = -robotToCameraYWpi;
            double limelightYawDeg = -robotToCameraYawDegWpi;
            this.cameraPose =
                    new double[] {
                        robotToCameraX, limelightRightMeters, cameraHeight, 0.0, cameraPitchDeg,
                        limelightYawDeg
                    };

            this.stdDevScale = entry.stdDevScale > 0.0 ? entry.stdDevScale : 1.0;
            this.pipeline = (int) Math.round(entry.pipeline);
        }
    }

    private final RobotState robotState;
    private final List<CameraHandle> cameraHandles;
    private final AtomicReference<VisionIOInputs> latestInputs =
            new AtomicReference<>(new VisionIOInputs());

    public VisionIOHardwareLimelight(RobotState robotState) {
        this.robotState = robotState;
        this.cameraHandles = buildCameraHandles();
        setLLSettings();
    }

    private List<CameraHandle> buildCameraHandles() {
        List<CameraHandle> handles = new ArrayList<>();
        for (LimelightEntry entry : LimelightConfig.getInstance().getEnabledLimelights()) {
            if (entry.table == null || entry.table.isBlank()) {
                continue;
            }
            handles.add(new CameraHandle(entry));
        }
        return handles;
    }

    private void setLLSettings() {
        for (CameraHandle camera : cameraHandles) {
            // cameraPoseはLimelight Web UIで設定するため、コードからの上書きは行わない
            camera.table.getEntry("pipeline").setDouble(camera.pipeline);
        }
    }

    @Override
    public void readInputs(VisionIOInputs inputs) {
        inputs.cameras.clear();

        // ビジョン融合済みではなくオドメトリのみのyawを使用してフィードバックループを防ぐ
        var latestOdomPose = robotState.getFieldToRobotOdom(RobotTime.getTimestampSeconds());
        boolean sentRobotOrientation = false;
        for (CameraHandle camera : cameraHandles) {
            VisionIOInputs.CameraInputs cameraInputs = new VisionIOInputs.CameraInputs();
            cameraInputs.name = camera.name;
            cameraInputs.tableName = camera.tableName;
            cameraInputs.robotToCamera = camera.robotToCamera;

            if (latestOdomPose.isPresent()) {
                double yawDegrees = latestOdomPose.get().getRotation().getDegrees();
                LimelightHelpers.SetRobotOrientation_NoFlush(
                        camera.tableName, yawDegrees, 0, 0, 0, 0, 0);
                sentRobotOrientation = true;
            }

            readCameraData(camera, cameraInputs);
            inputs.cameras.add(cameraInputs);
        }
        if (VisionConstants.useMegaTag2 && sentRobotOrientation) {
            NetworkTableInstance.getDefault().flush();
        }
        latestInputs.set(inputs);
    }

    private void readCameraData(CameraHandle camera, VisionIOInputs.CameraInputs cameraInputs) {
        cameraInputs.connected = isCameraConnected(camera);
        cameraInputs.seesTarget = false;
        cameraInputs.megatagPoseEstimate = null;
        cameraInputs.megatag2PoseEstimate = null;
        cameraInputs.megatagCount = 0;
        cameraInputs.megatag2Count = 0;
        cameraInputs.fiducialObservations = new FiducialObservation[0];
        cameraInputs.pose3d = null;
        cameraInputs.primaryTagId = -1;
        cameraInputs.botPoseTargetSpace = null;
        cameraInputs.standardDeviations = DEFAULT_STDDEVS.clone();

        if (!cameraInputs.connected) {
            return;
        }

        cameraInputs.seesTarget = camera.table.getEntry("tv").getDouble(0) == 1.0;
        cameraInputs.standardDeviations =
                scaleStdDevs(
                        camera.table.getEntry("stddevs").getDoubleArray(DEFAULT_STDDEVS),
                        camera.stdDevScale);

        if (!cameraInputs.seesTarget) {
            return;
        }

        try {
            // 一度だけNetworkTableを読み取る（最適化: 2回の読み取りを1回に削減）
            // useMegaTag2=trueのときはMT2キー(botpose_orb_wpiblue)を使用する
            boolean isMegaTag2 = VisionConstants.useMegaTag2;
            String poseKey = isMegaTag2 ? "botpose_orb_wpiblue" : "botpose_wpiblue";
            DoubleArrayEntry poseEntry =
                    LimelightHelpers.getLimelightDoubleArrayEntry(camera.tableName, poseKey);
            TimestampedDoubleArray tsValue = poseEntry.getAtomic();
            double[] poseArray = tsValue.value;

            if (poseArray.length < 6) {
                return;
            }

            // Pose3dを構築（Z座標チェック用）
            cameraInputs.pose3d = LimelightHelpers.toPose3D(poseArray);

            // PoseEstimateを構築（getBotPoseEstimate_wpiBlueと同等のロジック）
            var pose2d = LimelightHelpers.toPose2D(poseArray);
            double latency = poseArray.length > 6 ? poseArray[6] : 0;
            int tagCount = poseArray.length > 7 ? (int) poseArray[7] : 0;
            double tagSpan = poseArray.length > 8 ? poseArray[8] : 0;
            double tagDist = poseArray.length > 9 ? poseArray[9] : 0;
            double tagArea = poseArray.length > 10 ? poseArray[10] : 0;
            double adjustedTimestamp = (tsValue.timestamp / 1000000.0) - (latency / 1000.0);

            // RawFiducialsの構築
            int valsPerFiducial = 7;
            int expectedTotalVals = 11 + valsPerFiducial * tagCount;
            LimelightHelpers.RawFiducial[] rawFiducials = new LimelightHelpers.RawFiducial[tagCount];

            if (poseArray.length == expectedTotalVals) {
                for (int i = 0; i < tagCount; i++) {
                    int baseIndex = 11 + (i * valsPerFiducial);
                    int id = (int) poseArray[baseIndex];
                    double txnc = poseArray[baseIndex + 1];
                    double tync = poseArray[baseIndex + 2];
                    double ta = poseArray[baseIndex + 3];
                    double distToCamera = poseArray[baseIndex + 4];
                    double distToRobot = poseArray[baseIndex + 5];
                    double ambiguity = poseArray[baseIndex + 6];
                    rawFiducials[i] = new LimelightHelpers.RawFiducial(
                            id, txnc, tync, ta, distToCamera, distToRobot, ambiguity);
                }
            }

            var megatag = new LimelightHelpers.PoseEstimate(
                    pose2d, adjustedTimestamp, latency, tagCount,
                    tagSpan, tagDist, tagArea, rawFiducials, isMegaTag2);

            cameraInputs.megatagPoseEstimate = MegatagPoseEstimate.fromLimelight(megatag);
            cameraInputs.megatagCount = tagCount;
            if (isMegaTag2) {
                cameraInputs.megatag2PoseEstimate = cameraInputs.megatagPoseEstimate;
                cameraInputs.megatag2Count = tagCount;
            }
            cameraInputs.fiducialObservations = FiducialObservation.fromLimelight(rawFiducials);
            if (tagCount > 0 && rawFiducials[0] != null) {
                cameraInputs.primaryTagId = rawFiducials[0].id;
            } else {
                cameraInputs.primaryTagId = (int) camera.table.getEntry("tid").getDouble(-1.0);
            }

            double[] botPoseTargetSpaceArray =
                    camera.table.getEntry("botpose_targetspace").getDoubleArray(new double[0]);
            if (botPoseTargetSpaceArray.length >= 6) {
                // botpose_targetspace の座標系: X=タグ右方向, Y=タグ上方向(高さ), Z=タグ正面方向(奥行き)
                // toPose2D([0],[1],[5]) は高さを y に使うため不正確。
                // WPILib 2D用に正しく変換:
                //   x   = [2] = Zt (タグ正面方向の奥行き = WPILib x "前方")
                //   y   = -[0] = -Xt (タグ左方向 = WPILib y "左" = -右)
                //   yaw = [4] = pitch (垂直Y軸周りの回転 = 水平面での向き)
                Pose2d botPoseTargetSpace = new Pose2d(
                        botPoseTargetSpaceArray[2],
                        -botPoseTargetSpaceArray[0],
                        Rotation2d.fromDegrees(botPoseTargetSpaceArray[4]));
                if (Double.isFinite(botPoseTargetSpace.getX())
                        && Double.isFinite(botPoseTargetSpace.getY())
                        && Double.isFinite(botPoseTargetSpace.getRotation().getRadians())) {
                    cameraInputs.botPoseTargetSpace = botPoseTargetSpace;
                }
            }
        } catch (Exception e) {
            System.err.println("Error processing Limelight data: " + e.getMessage());
        }
    }

    private boolean isCameraConnected(CameraHandle camera) {
        double nowSec = RobotTime.getTimestampSeconds();
        double heartbeat = camera.table.getEntry("hb").getDouble(Double.NaN);
        if (!Double.isNaN(heartbeat)) {
            if (Double.isNaN(camera.lastHeartbeat)
                    || Math.abs(heartbeat - camera.lastHeartbeat) > 1e-9) {
                camera.lastHeartbeat = heartbeat;
                camera.lastHeartbeatChangeSec = nowSec;
            }
        }

        if (nowSec - camera.lastHeartbeatChangeSec <= HEARTBEAT_TIMEOUT_SEC) {
            return true;
        }

        // Older firmware can omit heartbeat; fall back to basic key presence check.
        double tvRaw = camera.table.getEntry("tv").getDouble(-1.0);
        double tlRaw = camera.table.getEntry("tl").getDouble(-1.0);
        return tvRaw >= 0.0 || tlRaw >= 0.0;
    }

    private double[] scaleStdDevs(double[] input, double scale) {
        double[] source = input == null ? DEFAULT_STDDEVS : input;
        double[] scaled = source.clone();
        for (int i = 0; i < scaled.length; i++) {
            scaled[i] *= scale;
        }
        return scaled;
    }
}
