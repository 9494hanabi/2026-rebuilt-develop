package frc.robot.commands.auto;

import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

public final class AutoTelemetry {
  private AutoTelemetry() {
    throw new UnsupportedOperationException("This is a utility class!");
  }

  public static void putState(String state) {
    SmartDashboard.putString("Auto/State", state);
    SmartDashboard.putNumber("Auto/TimestampSec", Timer.getFPGATimestamp());
  }

  public static void putEvent(String event) {
    SmartDashboard.putString("Auto/LastEvent", event);
  }

  public static void putSelectedPipeline(int pipeline) {
    SmartDashboard.putNumber("Auto/Vision/SelectedPipeline", pipeline);
  }

  public static void putExclusiveTag(int tagId) {
    SmartDashboard.putNumber("Auto/Vision/ExclusiveTagId", tagId);
  }

  public static void putPieceVision(
      String tableName,
      boolean hasTarget,
      double txDeg,
      double ta,
      double pipelineIndex,
      boolean nearWindowActive,
      double nearWindowElapsedSec,
      boolean finished) {
    SmartDashboard.putString("Auto/Vision/Piece/Table", tableName);
    SmartDashboard.putBoolean("Auto/Vision/Piece/HasTarget", hasTarget);
    SmartDashboard.putNumber("Auto/Vision/Piece/TxDeg", txDeg);
    SmartDashboard.putNumber("Auto/Vision/Piece/Ta", ta);
    SmartDashboard.putNumber("Auto/Vision/Piece/Pipeline", pipelineIndex);
    SmartDashboard.putBoolean("Auto/Vision/Piece/NearWindowActive", nearWindowActive);
    SmartDashboard.putNumber("Auto/Vision/Piece/NearWindowElapsedSec", nearWindowElapsedSec);
    SmartDashboard.putBoolean("Auto/Vision/Piece/Finished", finished);
  }
}
