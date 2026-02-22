package frc.robot.commands.debug.odmetry;

import static frc.robot.lib.constants.SemiAutoConstants.omegaMaximum;
import static frc.robot.lib.constants.SemiAutoConstants.velocityMaximum;
import static frc.robot.lib.constants.LogConstants.*;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.wpilibj.Timer;
import frc.robot.RobotState;
import frc.robot.lib.constants.FieldConstants;
import frc.robot.subsystems.SwerveSubsystem;

public class SetZeroCommand extends DriveControllCommand {
    private static final double kFusedOdomTranslationMismatchMeter = 0.75;
    private static final double kFieldBoundaryMarginMeter = 0.25;
    private final SwerveSubsystem swerve;
    private final RobotState state;
    private double lastStatusLogSec = Double.NEGATIVE_INFINITY;

    public SetZeroCommand(
            SwerveSubsystem swerve,
            RobotState state) {
        super(
                swerve,
                state,
                () -> selectCurrentPoseForSetZero(state),
                () -> FieldConstants.kInitialFieldToRobotPose,
                velocityMaximum,
                omegaMaximum);
        this.swerve = swerve;
        this.state = state;
    }

    @Override
    public void initialize() {
        super.initialize();
        lastStatusLogSec = Double.NEGATIVE_INFINITY;
        System.out.println("[SetZero] INITIALIZED");
    }

    @Override
    public void execute() {
        super.execute();

        double nowSec = Timer.getFPGATimestamp();
        if (nowSec - lastStatusLogSec < kStatusLogPeriodSec) {
            return;
        }
        lastStatusLogSec = nowSec;

        PoseSelection selection = evaluatePoseSelection(state);
        ChassisSpeeds measured = swerve.getSwerveDrive().getRobotVelocity();

        System.out.println("[SetZero] periodic pose log");
        System.out.printf(
                "          target=%s%n",
                formatPose(FieldConstants.kInitialFieldToRobotPose));
        System.out.printf(
                "          yagsl=%s fused=%s odom=%s selected=%s%n",
                formatPose(swerve.getSwerveDrive().getPose()),
                formatPose(selection.fusedPose),
                formatPose(selection.odomPose),
                formatPose(selection.selectedPose));
        System.out.printf(
                "          useFused=%b inField=%b deltaFO=%.2f meas=(%.2f, %.2f, %.3f)%n",
                selection.useFusedTranslation,
                selection.fusedInField,
                selection.fusedOdomTranslationDeltaMeter,
                measured.vxMetersPerSecond,
                measured.vyMetersPerSecond,
                measured.omegaRadiansPerSecond);
    }

    @Override
    public void end(boolean interrupted) {
        super.end(interrupted);
        System.out.println("[SetZero] ended, interrupted=" + interrupted);
    }

    private static Pose2d selectCurrentPoseForSetZero(RobotState state) {
        return evaluatePoseSelection(state).selectedPose;
    }

    private static PoseSelection evaluatePoseSelection(RobotState state) {
        var latestOdom = state.getLatestFieldToRobotOdom();
        if (latestOdom == null || latestOdom.getValue() == null) {
            return new PoseSelection(
                    null,
                    null,
                    FieldConstants.kInitialFieldToRobotPose,
                    Double.NaN,
                    false,
                    false);
        }

        Pose2d odomPose = latestOdom.getValue();
        var latestFused = state.getLatestFieldToRobot();
        if (latestFused == null || latestFused.getValue() == null) {
            return new PoseSelection(
                    null,
                    odomPose,
                    odomPose,
                    Double.NaN,
                    false,
                    false);
        }

        Pose2d fusedPose = latestFused.getValue();
        double fusedOdomTranslationDeltaMeter =
                fusedPose.getTranslation().getDistance(odomPose.getTranslation());

        boolean useFusedTranslation =
                isInsideFieldWithMargin(fusedPose.getX(), fusedPose.getY())
                        && Double.isFinite(fusedOdomTranslationDeltaMeter)
                        && fusedOdomTranslationDeltaMeter <= kFusedOdomTranslationMismatchMeter;

        double selectedX = useFusedTranslation ? fusedPose.getX() : odomPose.getX();
        double selectedY = useFusedTranslation ? fusedPose.getY() : odomPose.getY();
        if (!Double.isFinite(selectedX)
                || !Double.isFinite(selectedY)
                || !Double.isFinite(odomPose.getRotation().getRadians())) {
            return new PoseSelection(
                    fusedPose,
                    odomPose,
                    odomPose,
                    fusedOdomTranslationDeltaMeter,
                    isInsideFieldWithMargin(fusedPose.getX(), fusedPose.getY()),
                    useFusedTranslation);
        }
        // 平面はfused優先、headingはodom優先を維持する。
        Pose2d selectedPose = new Pose2d(selectedX, selectedY, odomPose.getRotation());
        return new PoseSelection(
                fusedPose,
                odomPose,
                selectedPose,
                fusedOdomTranslationDeltaMeter,
                isInsideFieldWithMargin(fusedPose.getX(), fusedPose.getY()),
                useFusedTranslation);
    }

    private static boolean isInsideFieldWithMargin(double xMeter, double yMeter) {
        return xMeter >= -kFieldBoundaryMarginMeter
                && xMeter <= FieldConstants.fieldLengthMeter + kFieldBoundaryMarginMeter
                && yMeter >= -kFieldBoundaryMarginMeter
                && yMeter <= FieldConstants.fieldWidthMeter + kFieldBoundaryMarginMeter;
    }

    private static String formatPose(Pose2d pose) {
        if (pose == null) {
            return "null";
        }
        return String.format(
                "(%.2f, %.2f, %.1f deg)",
                pose.getX(),
                pose.getY(),
                pose.getRotation().getDegrees());
    }

    private static final class PoseSelection {
        private final Pose2d fusedPose;
        private final Pose2d odomPose;
        private final Pose2d selectedPose;
        private final double fusedOdomTranslationDeltaMeter;
        private final boolean fusedInField;
        private final boolean useFusedTranslation;

        private PoseSelection(
                Pose2d fusedPose,
                Pose2d odomPose,
                Pose2d selectedPose,
                double fusedOdomTranslationDeltaMeter,
                boolean fusedInField,
                boolean useFusedTranslation) {
            this.fusedPose = fusedPose;
            this.odomPose = odomPose;
            this.selectedPose = selectedPose;
            this.fusedOdomTranslationDeltaMeter = fusedOdomTranslationDeltaMeter;
            this.fusedInField = fusedInField;
            this.useFusedTranslation = useFusedTranslation;
        }
    }
}
