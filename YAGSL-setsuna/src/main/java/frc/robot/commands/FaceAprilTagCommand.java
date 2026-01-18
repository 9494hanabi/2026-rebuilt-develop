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
        // ターゲットが見えてるか
        boolean tv = table.getEntry("tv").getDouble(0) == 1.0; // 1なら有効  [oai_citation:4‡docs.limelightvision.io](https://docs.limelightvision.io/docs/docs-limelight/apis/complete-networktables-api?utm_source=chatgpt.com)

        if (!tv) {
            // 見えてない時は停止（前回指令が残らないように）
            swerve.setChassisSpeeds(new ChassisSpeeds(0, 0, 0));
            return;
        }

        // AprilTagの3D位置（ロボット座標）
        // targetpose_robotspace = [tx, ty, tz, pitch, yaw, roll] (meters, degrees)  [oai_citation:5‡docs.limelightvision.io](https://docs.limelightvision.io/docs/docs-limelight/apis/complete-networktables-api?utm_source=chatgpt.com)
        double[] t = table.getEntry("targetpose_robotspace").getDoubleArray(new double[6]);

        double tagX_fwd_m   = t[0]; // 前方向 + (m)
        double tagY_right_m = t[1]; // 右方向 + (m)  ← Limelight座標

        // WPILibのvyは「左+」なので反転（右+ -> 左+）  [oai_citation:6‡FIRST Robotics Competition Documentation](https://docs.wpilib.org/en/stable/docs/software/kinematics-and-odometry/intro-and-chassis-speeds.html?utm_source=chatgpt.com)
        double tagY_left_m = -tagY_right_m;

        // =========================
        // 1) 並進（タグの手前で止める）
        // =========================
        double ex = tagX_fwd_m; // 前後誤差
        double ey = tagY_left_m;// 左右誤差

        double kPxy = 1.2;          // (m/s)/m
        double vMax = 1.5;          // m/s

        double vx = MathUtil.clamp(kPxy * ex, -vMax, vMax);
        double vy = MathUtil.clamp(kPxy * ey, -vMax, vMax);

        // 近づいたらガタガタしないように小さい誤差は0に（任意）
        double deadband = 0.03; // 3cm
        if (Math.abs(ex) < deadband) vx = 0.0;
        if (Math.abs(ey) < deadband) vy = 0.0;

        // =========================
        // 2) 回転（タグに正対：tx角度を0に）
        // =========================
        double txDeg = table.getEntry("tx").getDouble(0.0); // 水平角ズレ(deg)  [oai_citation:7‡docs.limelightvision.io](https://docs.limelightvision.io/docs/docs-limelight/apis/complete-networktables-api?utm_source=chatgpt.com)
        double txRad = Math.toRadians(txDeg);

        double kPtheta = 3.0;       // (rad/s)/rad
        double omegaMax = 2.5;      // rad/s

        // txが右(+)なら、時計回り(omega−)で戻したいのでマイナス
        // ChassisSpeeds: omegaはrad/s  [oai_citation:8‡FIRST Robotics Competition Documentation](https://docs.wpilib.org/en/stable/docs/software/kinematics-and-odometry/intro-and-chassis-speeds.html?utm_source=chatgpt.com)
        double omega = MathUtil.clamp(kPtheta * txRad, -omegaMax, omegaMax);

        // 角度も小さければ止める（任意）
        double thetaDeadbandRad = Math.toRadians(1.0);
        if (Math.abs(txRad) < thetaDeadbandRad) omega = 0.0;

        // 出力：vx=前, vy=左, omega=角速度  [oai_citation:9‡FIRST Robotics Competition Documentation](https://docs.wpilib.org/en/stable/docs/software/kinematics-and-odometry/intro-and-chassis-speeds.html?utm_source=chatgpt.com)
        swerve.setChassisSpeeds(new ChassisSpeeds(vx, vy, omega));
    }

    @Override
    public void end(boolean interrupted) {
        swerve.setChassisSpeeds(new ChassisSpeeds(0, 0, 0));
    }
}