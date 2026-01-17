package frc.robot.subsystems.vision;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import swervelib.SwerveDrive;
import frc.robot.lib.util.Constants.VisionConstants;
import frc.robot.lib.limelight.LimelightHelpers;

public class VisionSubsystem extends SubsystemBase {
  private final SwerveDrive swerveDrive;
  private final String limelightlName = VisionConstants.limelightName;

  public VisionSubsystem(SwerveDrive swerve) {
    this.swerveDrive = swerve;
  }

  @Override
  public void periodic() {
    // ① YAGSLの「信頼できる見出し角」をLimelightへ送る（毎周期）
    double yawDeg = this.swerveDrive.getOdometryHeading().getDegrees();
    LimelightHelpers.SetRobotOrientation(limelightlName, yawDeg, 0, 0, 0, 0, 0);

    // ② MegaTag2の推定値（wpiblue）を取得
    var mt2 = LimelightHelpers.getBotPoseEstimate_wpiBlue_MegaTag2(limelightlName);

    // ③ タグが見えてる時だけYAGSLへ融合
    if (mt2.tagCount > 0) {
      swerveDrive.addVisionMeasurement(mt2.pose, mt2.timestampSeconds);
    }
  }
}