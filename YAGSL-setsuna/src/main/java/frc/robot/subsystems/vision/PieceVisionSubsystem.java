package frc.robot.subsystems.vision;

import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.lib.limelight.LimelightHelpers;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import frc.robot.lib.constants.AutoVisionConstants;


/*担当
 * 晴太
 */
// CPU detector の piece 観測をまとめる subsystem。も
// 「何が見えているか」と「どの塊を狙うべきか」をここで管理する。
public class PieceVisionSubsystem extends SubsystemBase {
    public record ClusterTarget(
        double centerTxDeg,
        double totalArea,
        int targetCount,
        double widthDeg,
        double score) {}

    private record PieceObservation(double txDeg, double area) {}

    private static final double kHeartbeatTimeoutSec = 0.5; // Limelight の接続監視に使う heartbeat timeout[s]

    private final NetworkTable pieceCameraTable =
        NetworkTableInstance.getDefault().getTable(AutoVisionConstants.kPieceCameraTableName);


    private boolean connected = false;
    private int rawDetectionCount = 0;
    private int acceptedDetectionCount = 0;
    private double currentPipelineIndex = Double.NaN;
    private double lastHeartbeat = Double.NaN;
    private double lastHeartbeatChangeSec = Double.NEGATIVE_INFINITY;
    private double lastSeenTimestampSec = Double.NaN;
    private Optional<ClusterTarget> bestCluster = Optional.empty();
    private Optional<ClusterTarget> lastSeenCluster = Optional.empty();

    // 以下piece detector modeのコード
    // Auto から呼ばれて detector pipeline を切り替える。
    public void setDetectorPipeline() {
        LimelightHelpers.setPipelineIndex(
            AutoVisionConstants.kPieceCameraTableName,
            AutoVisionConstants.kPieceDetectorPipelineIndex);
    }

    public void setIdlePipeline() {
        LimelightHelpers.setPipelineIndex(
            AutoVisionConstants.kPieceCameraTableName,
            AutoVisionConstants.kPieceIdlePipelineIndex);
    }


    public boolean isConnected() {
        return connected;
    }

    public boolean hasTargetCluster() {
        return bestCluster.isPresent();
    }

    public Optional<ClusterTarget> getBestCluster() {
        return bestCluster;
    }

    public Optional<ClusterTarget> getLastSeenCluster() {
        return lastSeenCluster;
    }

    public double getCurrentPipelineIndex() {
        return currentPipelineIndex;
    }

    public int getRawDetectionCount() {
        return rawDetectionCount;
    }

    public int getAcceptedDetectionCount() {
        return acceptedDetectionCount;
    }

    public double getLastSeenAgeSec() {
        if (!Double.isFinite(lastSeenTimestampSec)) {
        return Double.POSITIVE_INFINITY;
        }
        return Timer.getFPGATimestamp() - lastSeenTimestampSec;
    }

    public void clearLastSeen() {
        lastSeenCluster = Optional.empty();
        lastSeenTimestampSec = Double.NaN;
    }

    @Override
    public void periodic() {
        connected = updateConnectionStatus();
        currentPipelineIndex =
            LimelightHelpers.getCurrentPipelineIndex(AutoVisionConstants.kPieceCameraTableName);

        if (!connected) {
        rawDetectionCount = 0;
        acceptedDetectionCount = 0;
        bestCluster = Optional.empty();
        publishTelemetry();
        return;
        }

        LimelightHelpers.RawDetection[] rawDetections =
            LimelightHelpers.getRawDetections(AutoVisionConstants.kPieceCameraTableName);

        rawDetectionCount = rawDetections.length;

        List<PieceObservation> acceptedObservations = collectAcceptedObservations(rawDetections);
        acceptedDetectionCount = acceptedObservations.size();
        bestCluster = selectBestCluster(acceptedObservations);

        if (bestCluster.isPresent()) {
        lastSeenCluster = bestCluster;
        lastSeenTimestampSec = Timer.getFPGATimestamp();
        }

        publishTelemetry();
    }

    // 以下piece camera接続確認のコード
    // heartbeat を優先し、古い firmware 用に tv / tl の存在も fallback に使う。
    private boolean updateConnectionStatus() {
        double nowSec = Timer.getFPGATimestamp();
        double heartbeat = pieceCameraTable.getEntry("hb").getDouble(Double.NaN);

        if (!Double.isNaN(heartbeat)) {
        if (Double.isNaN(lastHeartbeat) || Math.abs(heartbeat - lastHeartbeat) > 1e-9) {
            lastHeartbeat = heartbeat;
            lastHeartbeatChangeSec = nowSec;
        }
        }

        if ((nowSec - lastHeartbeatChangeSec) <= kHeartbeatTimeoutSec) {
        return true;
        }

        double tvRaw = pieceCameraTable.getEntry("tv").getDouble(-1.0);
        double tlRaw = pieceCameraTable.getEntry("tl").getDouble(-1.0);
        return tvRaw >= 0.0 || tlRaw >= 0.0;
    }

    // 以下piece detector filterのコード
    // detector の生結果から、狙う価値のある Fuel だけを残す。
    private List<PieceObservation> collectAcceptedObservations(
        LimelightHelpers.RawDetection[] rawDetections) {
        List<PieceObservation> acceptedObservations = new ArrayList<>();

        for (LimelightHelpers.RawDetection rawDetection : rawDetections) {
        if (!Double.isFinite(rawDetection.txnc) || !Double.isFinite(rawDetection.ta)) {
            continue;
        }

        if (Math.abs(rawDetection.txnc) > AutoVisionConstants.kMaxPieceAbsTxDeg) {
            continue;
        }

        if (rawDetection.ta < AutoVisionConstants.kMinPieceDetectionArea) {
            continue;
        }

        if (AutoVisionConstants.kPieceTargetClassId >= 0
            && rawDetection.classId != AutoVisionConstants.kPieceTargetClassId) {
            continue;
        }

        acceptedObservations.add(new PieceObservation(rawDetection.txnc, rawDetection.ta));
        }

        acceptedObservations.sort(Comparator.comparingDouble(PieceObservation::txDeg));
        return acceptedObservations;
    }

    // 以下piece cluster選択のコード
    // 横方向に近い detection 同士を 1 つの塊にまとめて、最も価値の高い塊を返す。
    private Optional<ClusterTarget> selectBestCluster(List<PieceObservation> acceptedObservations) {
        if (acceptedObservations.isEmpty()) {
        return Optional.empty();
        }

        List<ClusterTarget> clusterTargets = new ArrayList<>();
        List<PieceObservation> currentCluster = new ArrayList<>();
        currentCluster.add(acceptedObservations.get(0));

        for (int i = 1; i < acceptedObservations.size(); i++) {
        PieceObservation previousObservation = acceptedObservations.get(i - 1);
        PieceObservation nextObservation = acceptedObservations.get(i);

        if (Math.abs(nextObservation.txDeg() - previousObservation.txDeg())
            <= AutoVisionConstants.kPieceClusterMergeTxDeg) {
            currentCluster.add(nextObservation);
        } else {
            clusterTargets.add(buildCluster(currentCluster));
            currentCluster = new ArrayList<>();
            currentCluster.add(nextObservation);
        }
        }

        clusterTargets.add(buildCluster(currentCluster));

        return clusterTargets.stream()
            .max(Comparator.comparingDouble(ClusterTarget::score));
    }

    private ClusterTarget buildCluster(List<PieceObservation> clusterObservations) {
        double totalArea = 0.0;
        double weightedTxSum = 0.0;
        double minTxDeg = Double.POSITIVE_INFINITY;
        double maxTxDeg = Double.NEGATIVE_INFINITY;

        for (PieceObservation observation : clusterObservations) {
        totalArea += observation.area();
        weightedTxSum += observation.txDeg() * observation.area();
        minTxDeg = Math.min(minTxDeg, observation.txDeg());
        maxTxDeg = Math.max(maxTxDeg, observation.txDeg());
        }

        double centerTxDeg = totalArea > 1e-9 ? (weightedTxSum / totalArea) : 0.0;
        double widthDeg = clusterObservations.size() == 1 ? 0.0 : (maxTxDeg - minTxDeg);
        double score =
            totalArea
                + (AutoVisionConstants.kPieceClusterCountWeight * clusterObservations.size())
                - (AutoVisionConstants.kPieceClusterWidthPenalty * widthDeg);

        return new ClusterTarget(
            centerTxDeg,
            totalArea,
            clusterObservations.size(),
            widthDeg,
            score);
    }

  // 以下piece telemetryのコード
  // Auto 以外でも見やすいように subsystem 側で観測値をまとめて出す。
    private void publishTelemetry() {
        ClusterTarget activeCluster = bestCluster.orElse(null);

        SmartDashboard.putBoolean("Vision/Piece/Connected", connected);
        SmartDashboard.putNumber("Vision/Piece/Pipeline", currentPipelineIndex);
        SmartDashboard.putNumber("Vision/Piece/RawDetectionCount", rawDetectionCount);
        SmartDashboard.putNumber("Vision/Piece/AcceptedDetectionCount", acceptedDetectionCount);
        SmartDashboard.putBoolean("Vision/Piece/HasTargetCluster", bestCluster.isPresent());
        SmartDashboard.putNumber(
            "Vision/Piece/BestClusterTxDeg",
            activeCluster != null ? activeCluster.centerTxDeg() : Double.NaN);
        SmartDashboard.putNumber(
            "Vision/Piece/BestClusterArea",
            activeCluster != null ? activeCluster.totalArea() : Double.NaN);
        SmartDashboard.putNumber(
            "Vision/Piece/BestClusterCount",
            activeCluster != null ? activeCluster.targetCount() : 0.0);
        SmartDashboard.putNumber(
            "Vision/Piece/BestClusterWidthDeg",
            activeCluster != null ? activeCluster.widthDeg() : Double.NaN);
        SmartDashboard.putNumber(
            "Vision/Piece/BestClusterScore",
            activeCluster != null ? activeCluster.score() : Double.NaN);
        SmartDashboard.putNumber(
            "Vision/Piece/LastSeenAgeSec",
            Double.isFinite(lastSeenTimestampSec) ? getLastSeenAgeSec() : -1.0);
    }
}
