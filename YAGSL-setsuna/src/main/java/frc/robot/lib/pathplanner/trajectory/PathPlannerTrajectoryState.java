package frc.robot.lib.pathplanner.trajectory;

import com.pathplanner.lib.util.DriveFeedforwards;

import frc.robot.lib.pathplanner.path.PathConstraints;
import frc.robot.lib.pathplanner.util.FlippingUtil;

import edu.wpi.first.math.interpolation.Interpolatable;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.geometry.Translation2d;

public class PathPlannerTrajectoryState implements Interpolatable<PathPlannerTrajectoryState> {
    public double timeSeconds = 0.0;

    public ChassisSpeeds fieldSpeeds = new ChassisSpeeds();

    public Pose2d pose = Pose2d.kZero;

    public double linearVelocity = 0.0;

    public Rotation2d heading = Rotation2d.kZero;

    public double curvature = 0.0;

    public double waypointRelativePose = 0.0;

    public DriveFeedforwards feedforwards;

    protected double deltaPos = 0.0;

    protected Rotation2d deltaRot = Rotation2d.kZero;

    protected SwerveModuleTrajectoryState[] moduleStates;

    protected PathConstraints constraints;

    public PathPlannerTrajectoryState interpolate(PathPlannerTrajectoryState endVal, double t) {
        var lerpedState = new PathPlannerTrajectoryState();

        lerpedState.timeSeconds = MathUtil.interpolate(timeSeconds, endVal.timeSeconds, t);

        double deltaT = lerpedState.timeSeconds - timeSeconds;
        if (deltaT < 0) {
            return endVal.interpolate(this, 1 - t);
        }

        lerpedState.heading = heading;
        lerpedState.linearVelocity = MathUtil.interpolate(linearVelocity, endVal.linearVelocity, t);
        Translation2d speeds = new Translation2d(lerpedState.linearVelocity, lerpedState.heading);
        lerpedState.fieldSpeeds = 
                new ChassisSpeeds(
                        speeds.getX(),
                        speeds.getY(),
                        MathUtil.interpolate(
                                fieldSpeeds.omegaRadiansPerSecond,
                                endVal.fieldSpeeds.omegaRadiansPerSecond,
                                t));
        
        double lerpedXPos = pose.getX();
        double lerpedYPos = pose.getY();
        double intTime = timeSeconds + 0.01;
        while (true) {
            double intT = (intTime - timeSeconds) / (lerpedState.timeSeconds - timeSeconds);
            double intLinearVel = 
                    MathUtil.interpolate(linearVelocity, lerpedState.linearVelocity, intT);
            double intVX = intLinearVel * lerpedState.heading.getCos();
            double intVY = intLinearVel * lerpedState.heading.getSin();

            if (intTime >= lerpedState.timeSeconds - 0.01) {
                double dt = lerpedState.timeSeconds - intTime;
                lerpedXPos += intVX * dt;
                lerpedYPos += intVY * dt;
                break;
            }

            lerpedXPos += intVX * 0.01;
            lerpedYPos += intVY * 0.01;

            intTime += 0.01;
        }

        lerpedState.pose = 
                new Pose2d(
                        lerpedXPos,
                        lerpedYPos,
                        pose.getRotation().interpolate(endVal.pose.getRotation(), t));
        lerpedState.feedforwards = feedforwards.interpolate(endVal.feedforwards, t);

        double lerpedToPrev = 
                lerpedState.pose.getTranslation().getDistance(this.pose.getTranslation());
        double lerpedToNext =
                lerpedState.pose.getTranslation().getDistance(endVal.pose.getTranslation());
        double s = lerpedToPrev / (lerpedToPrev + lerpedToNext);
        lerpedState.waypointRelativePose =
                MathUtil.interpolate(waypointRelativePose, endVal.waypointRelativePose, s);

        return lerpedState;
    }

    public PathPlannerTrajectoryState reverse() {
        var reversed = new PathPlannerTrajectoryState();

        reversed.timeSeconds = timeSeconds;
        Translation2d reversedSpeeds =
                new Translation2d(fieldSpeeds.vxMetersPerSecond, fieldSpeeds.vyMetersPerSecond)
                        .rotateBy(Rotation2d.k180deg);
        reversed.fieldSpeeds =
                new ChassisSpeeds(
                        reversedSpeeds.getX(),
                        reversedSpeeds.getY(),
                        fieldSpeeds.omegaRadiansPerSecond);
        reversed.pose =
                new Pose2d(pose.getTranslation(), pose.getRotation().plus(Rotation2d.k180deg));
        reversed.linearVelocity = -linearVelocity;
        reversed.feedforwards = feedforwards.reverse();
        reversed.heading = heading.plus(Rotation2d.k180deg);

        return reversed;
    }

    public PathPlannerTrajectoryState flip() {
        var flipped = new PathPlannerTrajectoryState();

        flipped.timeSeconds = timeSeconds;
        flipped.linearVelocity = linearVelocity;
        flipped.pose = FlippingUtil.flipFieldPose(pose);
        flipped.fieldSpeeds = FlippingUtil.flipFieldSpeeds(fieldSpeeds);
        flipped.feedforwards = feedforwards.flip();
        flipped.heading = FlippingUtil.flipFieldRotation(heading);

        return flipped;
    }

    public PathPlannerTrajectoryState copyWithTime(double time) {
        PathPlannerTrajectoryState copy = new PathPlannerTrajectoryState();
        copy.timeSeconds = time;
        copy.fieldSpeeds = fieldSpeeds;
        copy.pose = pose;
        copy.linearVelocity = linearVelocity;
        copy.feedforwards = feedforwards;
        copy.heading = heading;
        copy.deltaPos = deltaPos;
        copy.deltaRot = deltaRot;
        copy.moduleStates = moduleStates;
        copy.constraints = constraints;
        copy.waypointRelativePose = waypointRelativePose;

        return copy;
    }
}
