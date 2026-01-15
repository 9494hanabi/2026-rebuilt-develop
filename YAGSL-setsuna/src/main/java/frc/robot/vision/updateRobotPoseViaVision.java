package frc.robot.vision;

import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.DriverStation.Alliance;

import frc.robot.lib.LimelightHelpers;
import frc.robot.util.Constants.VisionConstans;

public final class updateRobotPoseViaVision {

  private final boolean useMegaTag2;
  private final String limelightName;
  private final Alliance alliance;

  updateRobotPoseViaVision() {
    this.alliance = DriverStation.getAlliance().orElse(Alliance.Blue);
    this.useMegaTag2 = VisionConstans.useMegaTag2;
    this.limelightName = VisionConstans.limeligtName;
  }

  public void updatePose(visionPoseMeasurement out) {

    
    LimelightHelpers.PoseEstimate est;

    if (!this.useMegaTag2) {
      est = (this.alliance == Alliance.Red)
          ? LimelightHelpers.getBotPoseEstimate_wpiRed(limelightName)
          : LimelightHelpers.getBotPoseEstimate_wpiBlue(limelightName);
    } else {
      // MegaTag2はロボットの向きを事前に渡すのが必須（例はLimelight公式のチュートリアル参照）
      // LimelightHelpers.SetRobotOrientation(limelightName, robotYawDeg, 0, 0, 0, 0, 0);
      est = (alliance == Alliance.Red)
          ? LimelightHelpers.getBotPoseEstimate_wpiRed_MegaTag2(limelightName)
          : LimelightHelpers.getBotPoseEstimate_wpiBlue_MegaTag2(limelightName);
    }

    out.hasTarget = est.tagCount > 0;
    if (!out.hasTarget) return;

    out.pose = est.pose;
    out.timestampSeconds = est.timestampSeconds;   // これが最重要
    out.tagCount = est.tagCount;
    out.avgTagDist = est.avgTagDist;
    out.tagSpan = est.tagSpan;
  } 
}