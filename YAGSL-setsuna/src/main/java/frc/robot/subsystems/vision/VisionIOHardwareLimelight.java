package frc.robot.subsystems.vision;

import frc.robot.lib.util.Constants;
import frc.robot.lib.util.Constants.VisionConstants;
import frc.robot.RobotState;
import frc.robot.lib.limelight.LimelightHelpers;

import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableInstance;
import java.util.concurrent.atomic.AtomicReference;

public class VisionIOHardwareLimelight implements VisionIO {
    NetworkTable tableA =
        NetworkTableInstance.getDefault().getTable(VisionConstants.kLimelightATableName);
    NetworkTable tableB =
        NetworkTableInstance.getDefault().getTable(VisionConstants.kLimelightATableName);
    RobotState robotState;
    AtomicReference<VisionIOInputs> latestInputs = new AtomicReference<>(new VisionIOInputs());
    int imuMode = 1;

    private static final double[] DEFAULT_STDDEVS =
            new double[VisionConstants.kExpectedStdDevArrayLength];

    // このクラスを初期化します。
    // RobotState（ロボットの状態管理）への参照を受け取り、Limelight側の設定（カメラ姿勢など）を初期化します。
    public VisionIOHardwareLimelight(RobotState robotState) {
        this.robotState = robotState;
        setLLSettings();
    }

    // Limelightに対して「カメラの取り付け位置・姿勢（Robot Space）」を設定します。
    // これによりLimelightが推定するロボット姿勢が、ロボット座標系に正しく整合するようになります。
    private void setLLSettings() {
        double[] cameraAPose = {
            Constants.VisionConstants.kRobotToCameraAForward,
            Constants.VisionConstants.kRobotToCameraASide,
            VisionConstants.kCameraAHeightOffGroundMeters,
            0.0,
            VisionConstants.kCameraAPitchDegrees,
            VisionConstants.kCameraAYawOffset.getDegrees()
        };

        tableA.getEntry("camerapose_robotspace_set").setDoubleArray(cameraAPose);

        double[] cameraBPose = {
            Constants.VisionConstants.kRobotToCameraBForward,
            Constants.VisionConstants.kRobotToCameraBSide,
            VisionConstants.kCameraBHeightOffGroundMeters,
            0.0,
            VisionConstants.kCameraBPitchDegrees,
            VisionConstants.kCameraBYawOffset.getDegrees()
        };

        tableB.getEntry("camerapose_robotspace_set").setDoubleArray(cameraBPose);
    }

    // VisionIOの入力構造体（VisionIOInputs）を更新します。
    // Limelight A/B それぞれのNetworkTableから値を読み取り、周期処理（periodic）側で使える最新データとして保持します。
    @Override
    public void readInputs(VisionIOInputs inputs) {
        readCameraData(tableA, inputs.cameraA, VisionConstants.kLimelightATableName);
        readCameraData(tableB, inputs.cameraB, VisionConstants.kLimelightATableName);
        latestInputs.set(inputs);
    }

    // 1台のLimelight（= 1つのNetworkTable）から観測データを読み取り、CameraInputsに詰めます。
    // 「ターゲットが見えているか(tv)」を確認し、見えている場合のみ
    // - BotPose 推定（MegaTag系の推定）
    // - Fiducial（AprilTag）観測一覧
    // - stddevs（推定のばらつき）
    // を取り出して格納します。
    private void readCameraData(
        NetworkTable table, VisionIOInputs.CameraInputs camera, String limelightName) {

        camera.seesTarget = table.getEntry("tv").getDouble(0) == 1.0;

        if (camera.seesTarget) {
            try {
                var megatag = LimelightHelpers.getBotPoseEstimate_wpiBlue(limelightName);
                var robotPose3d =
                        LimelightHelpers.toPose3D(
                            LimelightHelpers.getBotPose_wpiBlue(limelightName));

                // MegaTag（推定結果）が取れた場合は、推定姿勢・タグ数・タグ観測（rawFiducials）を詰めます。
                if (megatag != null) {
                    camera.megatagPoseEstimate = MegatagPoseEstimate.fromLimelight(megatag);
                    camera.megatagCount = megatag.tagCount;
                    camera.fiducialObservations =
                            FiducialObservation.fromLimelight(megatag.rawFiducials);
                }

                // 3D姿勢が取れた場合は、可視化やデバッグ用の pose3d を詰めます。
                if (robotPose3d != null) {
                    camera.pose3d = robotPose3d;
                }

                // 推定の標準偏差（stddevs）を読み込みます。値が無い場合は DEFAULT_STDDEVS を使います。
                camera.standardDeviations =
                        table.getEntry("stddevs").getDoubleArray(DEFAULT_STDDEVS);

            } catch (Exception e) {
                System.err.println("Error proccessing Limelight data: " + e.getMessage());
            }
        }
    }
}