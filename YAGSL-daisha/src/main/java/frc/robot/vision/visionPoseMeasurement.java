package frc.robot.vision;
import edu.wpi.first.math.geometry.Pose2d;

public class visionPoseMeasurement {
  public boolean hasTarget = false;
  public Pose2d pose = new Pose2d();
  public double timestampSeconds = 0.0;

  // おまけ（必要なら使う）
  public double tagCount = 0;
  public double avgTagDist = 0;
  public double tagSpan = 0;
}