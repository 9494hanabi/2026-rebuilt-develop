package frc.robot.subsystems.vision;

import edu.wpi.first.wpilibj2.command.SubsystemBase;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.Matrix;
import edu.wpi.first.math.VecBuilder;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Transform2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.numbers.N1;
import edu.wpi.first.math.numbers.N3;
import edu.wpi.first.math.util.Units;

import swervelib.SwerveDrive;
import frc.robot.lib.util.Constants.FieldConstants;
import frc.robot.lib.util.Constants.VisionConstants;
import frc.robot.lib.limelight.LimelightHelpers;
import frc.robot.RobotState;
import frc.robot.lib.util.Constants.VisionConstants;
import frc.robot.lib.util.MathHelpers;
import frc.robot.lib.time.RobotTime;

import java.util.Optional;
import org.littletonrobotics.junction.Logger;

// === 担当者 ===
// ひなた
//

public class VisionSubsystem extends SubsystemBase {

    private final VisionIO io;
    private final RobotState state;
    private final VisionIO.VisionIOInputs inputs = new VisionIO.VisionIOInputs();

    private boolean useVision = true;

    // コンストラクタ
    public VisionSubsystem(VisionIO io, RobotState state) {
        // 依存するI/OとRobotStateの参照を保持する。
        this.io = io;
        this.state = state;
    }

    // ポーズの分散を元に、ポーズを融合している。
    private VisionFieldPoseEstimate fuseEstimates(
            VisionFieldPoseEstimate a, VisionFieldPoseEstimate b) {
        // 2つの推定を分散の重みで融合する。
        if (b.getTimestampSeconds() < a.getTimestampSeconds()) {
            VisionFieldPoseEstimate tmp = a;
            a = b;
            b = tmp;
        }

        // ベクトル計算
        // A = O -> a
        // B = O -> b
        // a_T_b = B - A (A -> B)
        // (回転も含む計算)
        Transform2d a_T_b = 
                state.getFieldToRobot(b.getTimestampSeconds())
                        .get()
                        .minus(state.getFieldToRobot(a.getTimestampSeconds()).get());
        
        
        Pose2d poseA = a.getVisionRobotPoseMeters();
        Pose2d poseB = b.getVisionRobotPoseMeters();

        // 標準偏差を元に、カメラによって推定される姿勢の分散を計算。
        var varianceA = 
                a.getVisionMeasurementsStdDevs().elementTimes(a.getVisionMeasurementsStdDevs());
        var varianceB = 
                b.getVisionMeasurementsStdDevs().elementTimes(b.getVisionMeasurementsStdDevs());

        // 進行方向の合成
        // ベクトル計算
        // if (varianceA < 信頼度 && varianceB < 信頼度 )
        //      fusedHeading = ( cos(R_a) / variance + cos(R_b) / variance
        //                     ( sin(R_a) / variance + sin(R_b) / variance
        // 逆三角関数で角度だけ取り出すので、長さは問題にならない。
        // どちらかのベクトルが極端に影響をあたえないようにvarianceを見てガードしている。
        Rotation2d fusedHeading = poseB.getRotation();
        if (varianceA.get(2, 0) < VisionConstants.kLargeVariance
                && varianceB.get(2, 0) < VisionConstants.kLargeVariance) {
            fusedHeading = 
                    new Rotation2d(
                        poseA.getRotation().getCos() / varianceA.get(2, 0)
                                + poseB.getRotation().getCos() / varianceB.get(2, 0),
                        poseA.getRotation().getSin() / varianceA.get(2, 0)
                                + poseB.getRotation().getSin() / varianceB.get(2, 0));
        }

        // 分散の逆数を取っている。
        // 分散が小さい -> 信頼度が高い
        // -> 逆数をとることで分散が小さくなるほど重みが大きくなるようにしている。
        double weightAx = 1.0 / varianceA.get(0, 0);
        double weightAy = 1.0 / varianceA.get(1, 0);
        double weightBx = 1.0 / varianceB.get(0, 0);
        double weightBy = 1.0 / varianceB.get(1, 0);

        // ２つの推定を重み付きで平均で融合
        // fuesdPose = 
        //              ( (( X_a * w_a_x + X_b * w_b_x ) / ( w_a_x + w_b_x ),
        //                 ( Y_a * w_a_y + Y_b * w_b_y ) / ( w_a_y + w_b_y )) , <-重み付き平均
        //                fusedHeading
        //              )
        Pose2d fusedPose = 
                new Pose2d(
                        new Translation2d(
                                (poseA.getTranslation().getX() * weightAx
                                                + poseB.getTranslation().getX() * weightBx)
                                        / (weightAx + weightBx),
                                (poseA.getTranslation().getY() * weightAy
                                                + poseB.getTranslation().getY() * weightBy)
                                        / (weightAy + weightBy)),
                        fusedHeading);
        
        // 重みを融合
        // fusedStdDev = 
        //              ( √ 1 / (w_a_x + w_b_x),
        //                √ 1 / (w_a_y + w_b_y),
        //                √ 1 / ( 1 / varianceA + 1 / barianceB )
        //              )
        Matrix<N3, N1> fusedStdDev =
                VecBuilder.fill(
                    Math.sqrt(1.0 / (weightAx + weightBx)),
                    Math.sqrt(1.0 / (weightAy + weightBy)),
                    Math.sqrt(1.0 / (1.0 / varianceA.get(2, 0) + 1.0 / varianceB.get(2,0))));
        
        // 推定に使われたタグの数。
        int numTags = a.getNumTags() + b.getNumTags();

        // タイムスタンプ
        double time = b.getTimestampSeconds();

        // 推定したポーズを返す。
        return new VisionFieldPoseEstimate(fusedPose, time, fusedStdDev, numTags);
    }

    @Override
    public void periodic() {
        // 入力更新と推定統合を行い、RobotStateへ反映する。
        double startTime = RobotTime.getTimestampSeconds();
        io.readInputs(inputs);

        // カメラ入力のログ
        logCameraInputs("Vision/CameraA", inputs.cameraA);
        logCameraInputs("Vision/CameraB", inputs.cameraB);

        // ビジョン推定結果
        var maybeMTA = processCamera(inputs.cameraA, "CameraA", VisionConstants.kRobotToCameraA);
        var maybeMTB = processCamera(inputs.cameraB, "CameraB", VisionConstants.kRobotToCameraB);

        // ビジョンを使わない時の処理を拾っている。
        if (!useVision) {
            Logger.recordOutput("Vision/usingVision", false);
            Logger.recordOutput("Vision/exclusiveTagId", state.getExclusiveTag().orElse(-1));
            Logger.recordOutput(
                "Vision/latencyPeriodicSec", RobotTime.getTimestampSeconds() - startTime);
            return;
        }

        Logger.recordOutput("Vision/usingVision", true);

        // ビジョンを片方だけ採用するか、融合して採用するか決めている。
        Optional<VisionFieldPoseEstimate> accepted = Optional.empty();
        if (maybeMTA.isPresent() != maybeMTB.isPresent()) {
            accepted = maybeMTA.isPresent() ? maybeMTA : maybeMTB;
        } else if (maybeMTA.isPresent() && maybeMTB.isPresent()) {
            accepted = Optional.of(fuseEstimates(maybeMTA.get(), maybeMTB.get()));
        }

        // 推定を採用したときだけログを出力
        accepted.ifPresent(
            est -> {
                Logger.recordOutput("Vision/fusedAccepted", est.getVisionRobotPoseMeters());
                state.updateMegatagEstimate((est));
            });
        
        Logger.recordOutput("Vision/exclusiveTagId", state.getExclusiveTag().orElse(-1));
        Logger.recordOutput(
            "Vision/latencyPeriodicSec", RobotTime.getTimestampSeconds() - startTime);
    }

    // カメラ系のIOのnullを拾ってログを出力している。
    private void logCameraInputs(String prefix, VisionIO.VisionIOInputs.CameraInputs cam) {
        // カメラ入力の状態をログ/ダッシュボードに出力する。
        Logger.recordOutput(prefix + "/SeesTarget", cam.seesTarget);
        Logger.recordOutput(prefix + "/MegatagCount", cam.megatagCount);

        if (DriverStation.isDisabled()) {
            SmartDashboard.putBoolean(prefix + "/SeesTarget", cam.seesTarget);
            SmartDashboard.putNumber(prefix + "MegatagCount", cam.megatagCount);
        }

        if (cam.pose3d != null) {
            Logger.recordOutput(prefix + "/Pose3d", cam.pose3d);
        }

        if (cam.megatagPoseEstimate != null) {
            Logger.recordOutput(
                prefix + "/MegatagPoseEstimate", cam.megatagPoseEstimate.fieldToRobot());
            Logger.recordOutput(prefix + "/Quality", cam.megatagPoseEstimate.quality());
            Logger.recordOutput(prefix + "/AvgTagArea", cam.megatagPoseEstimate.avgTagArea());
        }

        if (cam.fiducialObservations != null ) {
            Logger.recordOutput(prefix + "/FiducialCount", cam.fiducialObservations.length);
        }
    }

    // カメラプロセスを記述
    private Optional<VisionFieldPoseEstimate> processCamera(
            VisionIO.VisionIOInputs.CameraInputs cam, String label, Transform2d robotToCamera) {
        // 単一カメラの推定を評価して採用可能な推定のみ返す。
        String logPrefix = "Vision/" + label;

        // カメラが何もみていない時にemptyを返す。
        if (!cam.seesTarget) {
            return Optional.empty();
        }


        Optional<VisionFieldPoseEstimate> estimate = Optional.empty();

        if (cam.megatagPoseEstimate != null) {
            Optional<VisionFieldPoseEstimate> mtEstimate = 
                    processMegatagPoseEstimate(cam.megatagPoseEstimate, cam, logPrefix);
            
            // 観測したときだけログを出力
            mtEstimate.ifPresent(
                est -> 
                        Logger.recordOutput(
                            logPrefix + "/AcceptMegatagEstimate",
                            est.getVisionRobotPoseMeters())); 
            
            Optional<VisionFieldPoseEstimate> gyroEstimate =
                    fuseWithGyro(cam.megatagPoseEstimate, cam, logPrefix);
            
            // 観測したときだけログを出力
            gyroEstimate.ifPresent(
                est ->
                        Logger.recordOutput(
                            logPrefix + "/FuseWithGyroEstimate",
                            est.getVisionRobotPoseMeters()));
            
            // MegatagがあるときはMegatagを、無い時はGyroを使う。
            if (mtEstimate.isPresent()) {
                estimate = mtEstimate;
                Logger.recordOutput(logPrefix + "/AcceptMegatag", true);
                Logger.recordOutput(logPrefix + "/AcceptGyro", false);
            } else if (gyroEstimate.isPresent()) {
                estimate = gyroEstimate;
                Logger.recordOutput(logPrefix + "/AcceptMegatag", false);
                Logger.recordOutput(logPrefix + "/AcceptGyro", true);
            } else {
                Logger.recordOutput(logPrefix + "/AcceptMegatag", false);
                Logger.recordOutput(logPrefix + "/AcceptGyro", false);
            }
        }

        return estimate;
    }

    // ジャイロを使った姿勢推定プロセスを記述
    private Optional<VisionFieldPoseEstimate> fuseWithGyro(
            MegatagPoseEstimate poseEstimate,
            VisionIO.VisionIOInputs.CameraInputs cam,
            String logPrefix) {
        // 単一タグ推定をジャイロと整合させて補正する。
        if (poseEstimate.timestampSeconds() <= state.lastUsedMegatagTimestamp()) {
            return Optional.empty();
        }

        if (poseEstimate.fiducialIds().length > 1) {
            return Optional.empty();
        }

        final double kHighYawLookbackS = 0.3;

        // 使わない。
        final double kHighYawVelocityRadS = 5.0;

        // 不自然な推定(大きすぎるヨー角は捨てる。)
        if (state.getMaxAbsDriveYawAngularVelocityInRange(
                                poseEstimate.timestampSeconds() - kHighYawLookbackS,
                                poseEstimate.timestampSeconds())
                        .orElse(Double.POSITIVE_INFINITY)
                > kHighYawLookbackS) {
            return Optional.empty();
        }

        // タイムスタンプ時点でのロボットの姿勢を格納している。
        var priorPose = state.getFieldToRobot(poseEstimate.timestampSeconds());

        // priorPoseのエンプティを拾っている。
        if (priorPose.isEmpty()) {
            return Optional.empty();
        }

        // タグのレイアウトを読み込む
        var maybeFieldToTag =
                FieldConstants.kAprilTagLayout.getTagPose(poseEstimate.fiducialIds()[0]);
        if (maybeFieldToTag.isEmpty()) {
            return Optional.empty();
        }

        // タグの姿勢を格納
        Pose2d fieldToTag = maybeFieldToTag.get().toPose2d(); // タグの姿勢(回転も含む)

        // ロボットのタグに対する相対姿勢
        Pose2d robotToTag = fieldToTag.relativeTo(poseEstimate.fieldToRobot());

        // ベクトル計算
        // fieldToTag = O -> T
        // fieldToRobot = O -> R
        // posteriorPose = fieldToTag - fieldToRobot
        Pose2d posteriorPose =
                new Pose2d(
                        fieldToTag
                                .getTranslation()
                                .minus(
                                        robotToTag
                                                .getTranslation()
                                                .rotateBy(priorPose.get().getRotation())),
                        priorPose.get().getRotation());
        
        // 標準偏差
        double xStd = cam.standardDeviations[VisionConstants.kMegatag1XStdDevIndex];
        double yStd = cam.standardDeviations[VisionConstants.kMegatag1YStdDevIndex];
        double xyStd = Math.max(xStd, yStd);

        // 補正した姿勢情報からデータクラスVisionFieldPoseEstimateを作成して返り値として代入
        return Optional.of(
                new VisionFieldPoseEstimate(
                        posteriorPose,
                        poseEstimate.timestampSeconds(),
                        VecBuilder.fill(xyStd, xyStd, VisionConstants.kLargeVariance),
                        poseEstimate.fiducialIds().length));
    }

    // 
    private Optional<VisionFieldPoseEstimate> processMegatagPoseEstimate(
            MegatagPoseEstimate poseEstimate,
            VisionIO.VisionIOInputs.CameraInputs cam,
            String logPrefix) {
        // MegaTag推定の妥当性チェックと標準偏差の算出を行う。
        if (poseEstimate.timestampSeconds() <= state.lastUsedMegatagTimestamp()) {
            return Optional.empty();
        }

        // フレームに入っているIdの数で分岐
        if (poseEstimate.fiducialIds().length < 2) {

            for (var fiducial : cam.fiducialObservations) {
                // 曖昧さ(ambiguity)のしきい値で弾く
                if (fiducial.ambiguity() > VisionConstants.kDefaultAmbiguityThreshold) {
                    return Optional.empty();
                }
            }

            // Megatagに必要な最小面積で弾く。
            if (poseEstimate.avgTagArea() < VisionConstants.kTagMinAreaForSingleTagMegatag) {
                return Optional.empty();
            }

            // ヨー角の確認に必要なタグの最小面積で弾く
            var priorPose = state.getFieldToRobot(poseEstimate.timestampSeconds());
            if (poseEstimate.avgTagArea() < VisionConstants.kTagAreaThresholdForYawCheck
                    && priorPose.isPresent()) {
                double yawDiff = 
                        Math.abs(
                                MathUtil.angleModulus(
                                        priorPose.get().getRotation().getRadians()
                                                - poseEstimate
                                                        .fieldToRobot()
                                                        .getRotation()
                                                        .getRadians()));
                if (yawDiff > Units.degreesToRadians(VisionConstants.kDefaultYawDiffThreshold)) {
                    return Optional.empty();
                }
            }
        }

        // 位置ベクトルの絶対下限で弾く
        if (poseEstimate.fieldToRobot().getTranslation().getNorm()
                < VisionConstants.kDefaultNormThreshold) {
            return Optional.empty();
        }

        // Z誤差の許容しきい値で弾く
        if (Math.abs(cam.pose3d.getZ()) > VisionConstants.kDefaultZThreshold) {
            return Optional.empty();
        }

        // 必須タグの有無で弾くフィルタ
        var exclusiveTag = state.getExclusiveTag();
        boolean hasExclusiveId = 
                exclusiveTag.isPresent()
                        && java.util.Arrays.stream(poseEstimate.fiducialIds())
                                .anyMatch(id -> id == exclusiveTag.get());

        if (exclusiveTag.isPresent() && !hasExclusiveId) {
            return Optional.empty();
        }

        // ポーズのログ
        var loggedPose = state.getFieldToRobot(poseEstimate.timestampSeconds());
        if (loggedPose.isEmpty()) {
            return Optional.empty();
        }

        // 返り値を作る
        Pose2d estimatePose = poseEstimate.fieldToRobot();

        double scaleFactor = 1.0 / poseEstimate.quality();
        double xStd = cam.standardDeviations[VisionConstants.kMegatag1XStdDevIndex] * scaleFactor;
        double yStd = cam.standardDeviations[VisionConstants.kMegatag1YStdDevIndex] * scaleFactor;
        double rotStd = 
                cam.standardDeviations[VisionConstants.kMegatag1YawStdDevIndex] * scaleFactor;
        
        double xyStd = Math.max(xStd, yStd);
        Matrix<N3, N1> visionStdDevs = VecBuilder.fill(xyStd, xyStd, rotStd);

        return Optional.of(
                new VisionFieldPoseEstimate(
                    estimatePose, 
                    poseEstimate.timestampSeconds(),
                    visionStdDevs,
                    poseEstimate.fiducialIds().length));
    }

    public void setUseVision(boolean useVision) {
        // ビジョン推定の有効/無効を切り替える。
        this.useVision = useVision;
    }
}
