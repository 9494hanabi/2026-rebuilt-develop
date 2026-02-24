package frc.robot.lib.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.wpi.first.apriltag.AprilTag;
import edu.wpi.first.apriltag.AprilTagFieldLayout;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.wpilibj.Filesystem;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Comparator;
import java.util.List;

/**
 * Limelight Map Builder形式(.fmap)をAprilTagFieldLayoutへ変換する。
 * fmap座標はフィールド中心原点を前提に、WPILibの青同盟側コーナー原点へ変換する。
 */

// === 担当者 ===
// ひなた
//

public final class FmapFieldLayoutLoader {
  private static final ObjectMapper MAPPER =
      new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  private FmapFieldLayoutLoader() {
    throw new UnsupportedOperationException("This is a utility class!");
  }

  /**
   * deployディレクトリ配下のfmapを読み込み、AprilTagFieldLayoutを生成する。
   *
   * @param relativePath Filesystem.getDeployDirectory() からの相対パス
   */
  public static AprilTagFieldLayout loadFromDeploy(String relativePath) {
    File fmapFile = new File(Filesystem.getDeployDirectory(), relativePath);
    if (!fmapFile.isFile()) {
      throw new IllegalStateException("fmap file not found: " + fmapFile.getAbsolutePath());
    }

    final FmapRoot root;
    try {
      root = MAPPER.readValue(fmapFile, FmapRoot.class);
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to parse fmap: " + fmapFile.getAbsolutePath(), e);
    }

    validateRoot(root, fmapFile);

    List<AprilTag> tags =
        root.fiducials.stream()
            .map(fiducial -> toAprilTag(fiducial, root.fieldlength, root.fieldwidth))
            .sorted(Comparator.comparingInt(tag -> tag.ID))
            .toList();

    return new AprilTagFieldLayout(tags, root.fieldlength, root.fieldwidth);
  }

  private static void validateRoot(FmapRoot root, File fmapFile) {
    if (root == null) {
      throw new IllegalStateException("fmap is empty: " + fmapFile.getAbsolutePath());
    }
    if (root.fieldlength <= 0.0 || root.fieldwidth <= 0.0) {
      throw new IllegalStateException(
          "fmap has invalid field size: " + fmapFile.getAbsolutePath());
    }
    if (root.fiducials == null || root.fiducials.isEmpty()) {
      throw new IllegalStateException("fmap has no fiducials: " + fmapFile.getAbsolutePath());
    }
  }

  private static AprilTag toAprilTag(FmapFiducial fiducial, double fieldLength, double fieldWidth) {
    if (fiducial == null) {
      throw new IllegalStateException("fmap contains null fiducial entry");
    }
    if (fiducial.transform == null || fiducial.transform.length != 16) {
      throw new IllegalStateException("fmap fiducial transform must have 16 elements");
    }
    return new AprilTag(fiducial.id, toPose(fiducial.transform, fieldLength, fieldWidth));
  }

  /**
   * fmap transform配列は行優先4x4行列:
   * [r11,r12,r13,tx, r21,r22,r23,ty, r31,r32,r33,tz, 0,0,0,1]
   */
  private static Pose3d toPose(double[] transform, double fieldLength, double fieldWidth) {
    double xCenter = transform[3];
    double yCenter = transform[7];
    double z = transform[11];

    double x = xCenter + fieldLength / 2.0;
    double y = yCenter + fieldWidth / 2.0;

    double r11 = transform[0];
    double r21 = transform[4];
    double r31 = transform[8];
    double r32 = transform[9];
    double r33 = transform[10];

    double roll = Math.atan2(r32, r33);
    double pitch = Math.atan2(-r31, Math.hypot(r32, r33));
    double yaw = Math.atan2(r21, r11);

    return new Pose3d(x, y, z, new Rotation3d(roll, pitch, yaw));
  }

  private static final class FmapRoot {
    public List<FmapFiducial> fiducials;
    public double fieldlength;
    public double fieldwidth;
  }

  private static final class FmapFiducial {
    public int id;
    public double[] transform;
  }
}
