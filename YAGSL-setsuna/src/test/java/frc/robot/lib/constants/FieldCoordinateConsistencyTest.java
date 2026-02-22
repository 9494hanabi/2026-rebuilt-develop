package frc.robot.lib.constants;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.wpi.first.apriltag.AprilTagFieldLayout;
import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.geometry.Pose2d;
import frc.robot.lib.constants.commandconstants.SetToTagCommandConstants;
import frc.robot.lib.util.FmapFieldLayoutLoader;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FieldCoordinateConsistencyTest {

  @Test
  void fmapLoaderConvertsTagPosesToExpectedWpiBlueCoordinates() {
    AprilTagFieldLayout layout = FmapFieldLayoutLoader.loadFromDeploy("maps/fiducials.fmap");

    assertEquals(6.0, layout.getFieldLength(), 1e-9);
    assertEquals(9.0, layout.getFieldWidth(), 1e-9);

    var tag1 = layout.getTagPose(1).orElseThrow();
    assertEquals(5.525, tag1.getX(), 1e-3);
    assertEquals(9.000, tag1.getY(), 1e-3);
    assertEquals(-135.0, wrapDeg(Math.toDegrees(tag1.getRotation().getZ())), 1e-2);

    var tag3 = layout.getTagPose(3).orElseThrow();
    assertEquals(6.025, tag3.getX(), 1e-3);
    assertEquals(0.500, tag3.getY(), 1e-3);
    assertEquals(135.0, wrapDeg(Math.toDegrees(tag3.getRotation().getZ())), 1e-2);
  }

  @Test
  void setToTagTargetsStayInsideFieldAndMatchConfiguredCorners() {
    Map<Integer, Pose2d> map = SetToTagCommandConstants.tagToVertexMap;
    assertEquals(8, map.size());

    double nearMaxX = FieldConstants.fieldLengthMeter - 0.5;
    double nearMinX = 0.5;
    double nearMaxY = FieldConstants.fieldWidthMeter - 0.5;
    double nearMinY = 0.5;

    for (Pose2d pose : map.values()) {
      assertTrue(Double.isFinite(pose.getX()));
      assertTrue(Double.isFinite(pose.getY()));
      assertTrue(Double.isFinite(pose.getRotation().getRadians()));
      assertTrue(pose.getX() >= -1e-9 && pose.getX() <= FieldConstants.fieldLengthMeter + 1e-9);
      assertTrue(pose.getY() >= -1e-9 && pose.getY() <= FieldConstants.fieldWidthMeter + 1e-9);
    }

    assertPoseEquals(map.get(1), map.get(2));
    assertPoseEquals(map.get(3), map.get(4));
    assertPoseEquals(map.get(5), map.get(6));
    assertPoseEquals(map.get(7), map.get(8));

    assertEquals(nearMaxX, map.get(1).getX(), 1e-9);
    assertEquals(nearMaxY, map.get(1).getY(), 1e-9);
    assertEquals(-135.0, wrapDeg(map.get(1).getRotation().getDegrees()), 1e-9);

    assertEquals(nearMaxX, map.get(3).getX(), 1e-9);
    assertEquals(nearMinY, map.get(3).getY(), 1e-9);
    assertEquals(135.0, wrapDeg(map.get(3).getRotation().getDegrees()), 1e-9);

    assertEquals(nearMinX, map.get(5).getX(), 1e-9);
    assertEquals(nearMinY, map.get(5).getY(), 1e-9);
    assertEquals(45.0, wrapDeg(map.get(5).getRotation().getDegrees()), 1e-9);

    assertEquals(nearMinX, map.get(7).getX(), 1e-9);
    assertEquals(nearMaxY, map.get(7).getY(), 1e-9);
    assertEquals(-45.0, wrapDeg(map.get(7).getRotation().getDegrees()), 1e-9);
  }

  private static void assertPoseEquals(Pose2d a, Pose2d b) {
    assertEquals(a.getX(), b.getX(), 1e-12);
    assertEquals(a.getY(), b.getY(), 1e-12);
    assertEquals(a.getRotation().getRadians(), b.getRotation().getRadians(), 1e-12);
  }

  private static double wrapDeg(double degrees) {
    return MathUtil.inputModulus(degrees, -180.0, 180.0);
  }
}

