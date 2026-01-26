
//
// FaceAprilTagCommand() -> FaceAprilTag()
//

package frc.robot.commands;

import frc.robot.subsystems.SwerveSubsystem;
import static frc.robot.lib.util.Constants.SemiAutoConstants.*;
import frc.robot.lib.util.Constants.VisionConstants;

import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.wpilibj2.command.Command;

// === 担当者 ===
// ひなた
//

public class FaceAprilTagCommand extends Command {
    private final SwerveSubsystem swerve;

    private final NetworkTable table =
    NetworkTableInstance.getDefault().getTable(VisionConstants.kFaceAprilTagTableName);

    public FaceAprilTagCommand(
        SwerveSubsystem swerve
    ) {
        this.swerve = swerve;
        addRequirements(swerve);
    }

    @Override
    public void initialize() {
    }

    @Override
    public void execute() {
        boolean tv = table.getEntry("tv").getDouble(0) == 1.0; // 1なら有効  [oai_citation:4‡docs.limelightvision.io](https://docs.limelightvision.io/docs/docs-limelight/apis/complete-networktables-api?utm_source=chatgpt.com)

        if (!tv) {
            // 見えてない時は停止（前回指令が残らないように）
            swerve.setChassisSpeeds(new ChassisSpeeds(0, 0, 0));
            return;
        }

        // AprilTagの3D位置（ロボット座標）
        // targetpose_robotspace = [tx, ty, tz, pitch, yaw, roll] (meters, degrees)  [oai_citation:5‡docs.limelightvision.io](https://docs.limelightvision.io/docs/docs-limelight/apis/complete-networktables-api?utm_source=chatgpt.com)
        double[] t = table.getEntry("targetpose_robotspace").getDoubleArray(new double[6]);

        // =========================
        // 1) 並進（タグの手前で止める）
        // =========================
        double forwardErrorwithTargetMeter = t[0]; // 前後誤差
        double leftErrorwithTargetMeter = -t[1];   // 座標系が逆なため、左右反転

        double velocity_x = MathUtil.clamp(translationGain * forwardErrorwithTargetMeter, -velocityMaximum, velocityMaximum);
        double velocity_y = MathUtil.clamp(translationGain * leftErrorwithTargetMeter, -velocityMaximum, velocityMaximum);

        if (Math.abs(forwardErrorwithTargetMeter) < planeDeadbandMeter) velocity_x = 0.0;
        if (Math.abs(leftErrorwithTargetMeter) < planeDeadbandMeter) velocity_y = 0.0;

        // =========================
        // 2) 回転（タグに正対：tx角度を0に）
        // =========================
        double angularErrorwithTargetRad = Math.toRadians(table.getEntry("tx").getDouble(0.0)); // 水平角ズレ(deg)  [oai_citation:7‡docs.limelightvision.io](https://docs.limelightvision.io/docs/docs-limelight/apis/complete-networktables-api?utm_source=chatgpt.com)

        // txが右(+)なら、時計回り(omega−)で戻したいのでマイナス
        // ChassisSpeeds: omegaはrad/s  [oai_citation:8‡FIRST Robotics Competition Documentation](https://docs.wpilib.org/en/stable/docs/software/kinematics-and-odometry/intro-and-chassis-speeds.html?utm_source=chatgpt.com)
        double omega = MathUtil.clamp(angularGain * angularErrorwithTargetRad, -omegaMaximum, omegaMaximum);

        if (Math.abs(angularErrorwithTargetRad) < thetaDeadbandRad) omega = 0.0;

        // 出力：vx=前, vy=左, omega=角速度  [oai_citation:9‡FIRST Robotics Competition Documentation](https://docs.wpilib.org/en/stable/docs/software/kinematics-and-odometry/intro-and-chassis-speeds.html?utm_source=chatgpt.com)
        swerve.setChassisSpeeds(new ChassisSpeeds(velocity_x, velocity_y, omega));
    }

    @Override
    public void end(boolean interrupted) {
        swerve.setChassisSpeeds(new ChassisSpeeds(0, 0, 0));
    }
}
