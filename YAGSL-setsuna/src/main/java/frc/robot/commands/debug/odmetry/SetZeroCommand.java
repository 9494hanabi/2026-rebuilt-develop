package frc.robot.commands.debug.odmetry;

import static frc.robot.lib.constants.SemiAutoConstants.omegaMaximum;
import static frc.robot.lib.constants.SemiAutoConstants.velocityMaximum;

import edu.wpi.first.math.geometry.Pose2d;
import frc.robot.RobotState;
import frc.robot.lib.constants.FieldConstants;
import frc.robot.subsystems.SwerveSubsystem;

public class SetZeroCommand extends DriveControllCommand {
    private static final double kFusedOdomTranslationMismatchMeter = 0.75;
    private static final double kFieldBoundaryMarginMeter = 0.25;

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
    }

    private static Pose2d selectCurrentPoseForSetZero(RobotState state) {
        var latestOdom = state.getLatestFieldToRobotOdom();
        if (latestOdom == null || latestOdom.getValue() == null) {
            return FieldConstants.kInitialFieldToRobotPose;
        }

        Pose2d odomPose = latestOdom.getValue();
        var latestFused = state.getLatestFieldToRobot();
        if (latestFused == null || latestFused.getValue() == null) {
            return odomPose;
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
            return odomPose;
        }
        // 平面はfused優先、headingはodom優先を維持する。
        return new Pose2d(selectedX, selectedY, odomPose.getRotation());
    }

    private static boolean isInsideFieldWithMargin(double xMeter, double yMeter) {
        return xMeter >= -kFieldBoundaryMarginMeter
                && xMeter <= FieldConstants.fieldLengthMeter + kFieldBoundaryMarginMeter
                && yMeter >= -kFieldBoundaryMarginMeter
                && yMeter <= FieldConstants.fieldWidthMeter + kFieldBoundaryMarginMeter;
    }
}
