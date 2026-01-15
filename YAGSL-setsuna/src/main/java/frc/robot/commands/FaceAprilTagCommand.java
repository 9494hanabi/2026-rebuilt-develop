package frc.robot.commands;

// import java.util.function.DoubleSupplier;

import edu.wpi.first.math.MathUtil;
// import edu.wpi.first.math.controller.PIDController;
// import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.wpilibj2.command.Command;
// import edu.wpi.first.math.geometry.Pose2d;

// import com.studica.frc.AHRS;
// import edu.wpi.first.wpilibj.SPI;

import frc.robot.subsystems.SwerveSubsystem;
// import frc.robot.vision.updateRobotPoseViaVision;
// import frc.robot.vision.visionPoseMeasurement;

import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableInstance;

// LimelightLib（LimelightHelpers.java）をプロジェクトに入れている想定
// import frc.robot.lib.LimelightHelpers;

public class FaceAprilTagCommand extends Command {
    private final SwerveSubsystem swerve;

    // 角度誤差[rad] -> 角速度[rad/s]
    // private final PIDController turnPid = new PIDController(4.0, 0, 0);

    // 自己位置
    // private static final visionPoseMeasurement robotPose = new visionPoseMeasurement();

    // 自己位置更新
    // private static final updateRobotPoseViaVision updateRobotPose = new updateRobotPoseViaVision();

    private final NetworkTable table =
    NetworkTableInstance.getDefault().getTable("limelight");

    public FaceAprilTagCommand(
        SwerveSubsystem swerve
        // AHRS gyro
    ) {
        this.swerve = swerve;
        addRequirements(swerve);
    }

    @Override
    public void initialize() {
        // turnPid.reset();
    }

    @Override
    public void execute() {
        double[] t = table.getEntry("targetpose_robotspace").getDoubleArray(new double[6]);
        boolean tv = table.getEntry("tv").getDouble(0) == 1;

        if (tv) {
            double x = t[0]; // 前方向[m]（ロボット座標）
            double y = t[1]; // 左方向[m]

            // 目標：タグの手前 stopDist で止まりたいなら x - stopDist を誤差にする
            // double stopDist = 0.8; // 80cm手前で止まる例
            double ex = x;
            double ey = -y;

            double kP = 1.2; // (m/s) / m
            double vx = MathUtil.clamp(kP * ex, -1.5, 1.5);
            double vy = MathUtil.clamp(kP * ey, -1.5, 1.5);

            // タグの方向を向きたいなら、tx（角度）や atan2 でomegaを作る（ここは例）
            double headingErr = Math.atan2(y, x); // rad
            double kPtheta = 2.0;
            double omega = MathUtil.clamp(kPtheta * headingErr, -2.5, 2.5);

            swerve.setChassisSpeeds(new ChassisSpeeds(vx, vy, omega));
        }
    }

    @Override
    public void end(boolean interrupted) {
        swerve.setChassisSpeeds(new ChassisSpeeds(0, 0, 0));
    }
}