package frc.robot.subsystems.vision;

import edu.wpi.first.math.Matrix;
import edu.wpi.first.math.VecBuilder;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Transform2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.numbers.N1;
import edu.wpi.first.math.numbers.N3;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.RobotState;
import frc.robot.lib.time.RobotTime;
import frc.robot.lib.util.Constants.VisionConstants;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.littletonrobotics.junction.Logger;

public class VisionSubsystem extends SubsystemBase {
    private final VisionIO io;
    private final RobotState state;
    private final VisionIO.VisionIOInputs inputs = new VisionIO.VisionIOInputs();

    private boolean useVision = true;

    private static boolean isUsingMegaTag2() {
        return VisionConstants.useMegaTag2;
    }

    private static int getVisionXStdDevIndex() {
        return isUsingMegaTag2()
                ? VisionConstants.kMegatag2XStdDevIndex
                : VisionConstants.kMegatag1XStdDevIndex;
    }

    private static int getVisionYStdDevIndex() {
        return isUsingMegaTag2()
                ? VisionConstants.kMegatag2YStdDevIndex
                : VisionConstants.kMegatag1YStdDevIndex;
    }

    private static int getVisionYawStdDevIndex() {
        return isUsingMegaTag2()
                ? VisionConstants.kMegatag2YawStdDevIndex
                : VisionConstants.kMegatag1YawStdDevIndex;
    }

    public VisionSubsystem(VisionIO io, RobotState state) {
        this.io = io;
        this.state = state;
    }

    private VisionFieldPoseEstimate fuseEstimates(VisionFieldPoseEstimate a, VisionFieldPoseEstimate b) {
        if (b.getTimestampSeconds() < a.getTimestampSeconds()) {
            VisionFieldPoseEstimate tmp = a;
            a = b;
            b = tmp;
        }

        var maybePoseA = state.getFieldToRobotOdom(a.getTimestampSeconds());
        var maybePoseB = state.getFieldToRobotOdom(b.getTimestampSeconds());
        if (maybePoseA.isEmpty() || maybePoseB.isEmpty()) {
            return b;
        }

        Transform2d aToB = maybePoseB.get().minus(maybePoseA.get());
        Pose2d poseA = a.getVisionRobotPoseMeters().transformBy(aToB);
        Pose2d poseB = b.getVisionRobotPoseMeters();

        var varianceA = a.getVisionMeasurementsStdDevs().elementTimes(a.getVisionMeasurementsStdDevs());
        var varianceB = b.getVisionMeasurementsStdDevs().elementTimes(b.getVisionMeasurementsStdDevs());

        Rotation2d fusedHeading = poseB.getRotation();
        if (varianceA.get(2, 0) < VisionConstants.kLargeVariance
                && varianceB.get(2, 0) < VisionConstants.kLargeVariance) {
            fusedHeading = new Rotation2d(
                    poseA.getRotation().getCos() / varianceA.get(2, 0)
                            + poseB.getRotation().getCos() / varianceB.get(2, 0),
                    poseA.getRotation().getSin() / varianceA.get(2, 0)
                            + poseB.getRotation().getSin() / varianceB.get(2, 0));
        }

        double weightAx = 1.0 / varianceA.get(0, 0);
        double weightAy = 1.0 / varianceA.get(1, 0);
        double weightBx = 1.0 / varianceB.get(0, 0);
        double weightBy = 1.0 / varianceB.get(1, 0);

        Pose2d fusedPose = new Pose2d(
                new Translation2d(
                        (poseA.getTranslation().getX() * weightAx + poseB.getTranslation().getX() * weightBx)
                                / (weightAx + weightBx),
                        (poseA.getTranslation().getY() * weightAy + poseB.getTranslation().getY() * weightBy)
                                / (weightAy + weightBy)),
                fusedHeading);

        Matrix<N3, N1> fusedStdDev = VecBuilder.fill(
                Math.sqrt(1.0 / (weightAx + weightBx)),
                Math.sqrt(1.0 / (weightAy + weightBy)),
                Math.sqrt(1.0 / (1.0 / varianceA.get(2, 0) + 1.0 / varianceB.get(2, 0))));

        return new VisionFieldPoseEstimate(
                fusedPose,
                b.getTimestampSeconds(),
                fusedStdDev,
                a.getNumTags() + b.getNumTags());
    }

    @Override
    public void periodic() {
        double startTime = RobotTime.getTimestampSeconds();

        if (DriverStation.isDisabled() || !useVision) {
            Logger.recordOutput("Vision/usingVision", false);
            return;
        }

        io.readInputs(inputs);

        List<VisionFieldPoseEstimate> acceptedByCamera = new ArrayList<>();
        for (var cam : inputs.cameras) {
            processCamera(cam).ifPresent(acceptedByCamera::add);
        }

        Logger.recordOutput("Vision/usingVision", true);

        Optional<VisionFieldPoseEstimate> accepted = Optional.empty();
        if (acceptedByCamera.size() == 1) {
            accepted = Optional.of(acceptedByCamera.get(0));
        } else if (!acceptedByCamera.isEmpty()) {
            acceptedByCamera.sort(Comparator.comparingDouble(VisionFieldPoseEstimate::getTimestampSeconds));
            VisionFieldPoseEstimate fused = acceptedByCamera.get(0);
            for (int i = 1; i < acceptedByCamera.size(); i++) {
                fused = fuseEstimates(fused, acceptedByCamera.get(i));
            }
            accepted = Optional.of(fused);
        }

        accepted.ifPresent(est -> {
            Logger.recordOutput("Vision/fusedAccepted", est.getVisionRobotPoseMeters());
            state.updateMegatagEstimate(est);
        });

        Logger.recordOutput("Vision/latencyPeriodicSec", RobotTime.getTimestampSeconds() - startTime);
    }

    private Optional<VisionFieldPoseEstimate> processCamera(VisionIO.VisionIOInputs.CameraInputs cam) {
        if (cam == null || !cam.seesTarget || cam.megatagPoseEstimate == null) {
            return Optional.empty();
        }
        return processMegatagPoseEstimate(cam.megatagPoseEstimate, cam);
    }

    private Optional<VisionFieldPoseEstimate> processMegatagPoseEstimate(
            MegatagPoseEstimate poseEstimate,
            VisionIO.VisionIOInputs.CameraInputs cam) {
        if (poseEstimate == null) {
            return Optional.empty();
        }

        double timestampSec = poseEstimate.timestampSeconds();
        if (!Double.isFinite(timestampSec) || timestampSec <= state.lastUsedMegatagTimestamp()) {
            return Optional.empty();
        }

        Pose2d estimatePose = poseEstimate.fieldToRobot();
        if (!isFinitePose(estimatePose)) {
            return Optional.empty();
        }

        double quality = poseEstimate.quality();
        double scaleFactor = (Double.isFinite(quality) && quality > 0.0)
                ? 1.0 / Math.max(quality, 1e-3)
                : 1.0;

        double xStd = readStdDev(cam.standardDeviations, getVisionXStdDevIndex()) * scaleFactor;
        double yStd = readStdDev(cam.standardDeviations, getVisionYStdDevIndex()) * scaleFactor;
        double rotStd = readStdDev(cam.standardDeviations, getVisionYawStdDevIndex()) * scaleFactor;

        double xyStd = sanitizeStdDev(Math.max(xStd, yStd));
        rotStd = sanitizeStdDev(rotStd);
        if (isUsingMegaTag2()) {
            rotStd = Math.max(rotStd, VisionConstants.kLargeVariance);
        }

        Matrix<N3, N1> visionStdDevs = VecBuilder.fill(xyStd, xyStd, rotStd);

        return Optional.of(new VisionFieldPoseEstimate(
                estimatePose,
                timestampSec,
                visionStdDevs,
                poseEstimate.fiducialIds().length));
    }

    private static boolean isFinitePose(Pose2d pose) {
        return pose != null
                && Double.isFinite(pose.getX())
                && Double.isFinite(pose.getY())
                && Double.isFinite(pose.getRotation().getRadians());
    }

    private static double readStdDev(double[] stdDevs, int index) {
        if (stdDevs == null || index < 0 || index >= stdDevs.length) {
            return 1.0;
        }
        return stdDevs[index];
    }

    private static double sanitizeStdDev(double stdDev) {
        return Double.isFinite(stdDev) && stdDev > 0.0 ? stdDev : 1.0;
    }

    public void setUseVision(boolean useVision) {
        this.useVision = useVision;
    }
}
