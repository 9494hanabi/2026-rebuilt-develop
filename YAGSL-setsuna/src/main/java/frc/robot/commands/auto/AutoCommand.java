// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.commands.auto;

import com.pathplanner.lib.path.PathConstraints;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import frc.robot.RobotState;
import frc.robot.commands.debug.odmetry.SetZeroCommand;
import frc.robot.commands.debug.vision.ObservationOKCommand;
import frc.robot.lib.constants.commandconstants.ShootAngleCommandConstants;
import frc.robot.subsystems.SwerveSubsystem;
import frc.robot.subsystems.mechanism.ShootAngleSubsystems;
import frc.robot.subsystems.mechanism.ShooterSubsystem;
import frc.robot.subsystems.shooter.ShooterFeedSubsystem;

// === 担当者 ===
// はるた
//

// Auto用の「使い回しコマンド」をまとめるユーティリティクラス
public final class AutoCommand {
  private AutoCommand() {
    throw new UnsupportedOperationException("This is a utility class!");
  }

  // doNothing: 何もしないコマンド（Auto未選択時の退避用）
  public static Command doNothing() {
    return Commands.none();
  }

  // stopDrive: ドライブベースを即停止させる
  public static Command stopDrive(SwerveSubsystem drivebase) {
    return Commands.runOnce(
        () -> drivebase.setChassisSpeeds(new ChassisSpeeds(0.0, 0.0, 0.0)),
        drivebase);
  }

  // pathPlannerAuto: 指定Autoを実行し、開始/終了ログと最後の停止を行う
  public static Command pathPlannerAuto(SwerveSubsystem drivebase, String autoName) {
    return Commands.sequence(
        Commands.print("[AUTO] start: " + autoName),
        drivebase.getAutonomousCommand(autoName),
        stopDrive(drivebase),
        Commands.print("[AUTO] end: " + autoName));
  }

  public static Command followPath(SwerveSubsystem drivebase, String pathName) {
    return Commands.sequence(
        Commands.print("[AUTO] followPath start: " + pathName),
        drivebase.followPath(pathName),
        stopDrive(drivebase),
        Commands.print("[AUTO] followPath end: " + pathName));
  }

  public static Command pathfindToPose(
      SwerveSubsystem drivebase,
      Pose2d targetPose,
      PathConstraints constraints,
      double goalEndVelocityMps) {
    return Commands.sequence(
        Commands.print("[AUTO] pathfindToPose start"),
        drivebase.pathfindToPose(targetPose, constraints, goalEndVelocityMps),
        stopDrive(drivebase),
        Commands.print("[AUTO] pathfindToPose end"));
  }

  public static Command pathfindThenFollowPath(
      SwerveSubsystem drivebase,
      String goalPathName,
      PathConstraints constraints) {
    return Commands.sequence(
        Commands.print("[AUTO] pathfindThenFollowPath start: " + goalPathName),
        drivebase.pathfindThenFollowPath(goalPathName, constraints),
        stopDrive(drivebase),
        Commands.print("[AUTO] pathfindThenFollowPath end: " + goalPathName));
  }

  // runChassisFor: 指定時間だけ任意の速度で走る共通メソッド
  public static Command runChassisFor(
      SwerveSubsystem drivebase,
      ChassisSpeeds speeds,
      double seconds) {
    return Commands.run(() -> drivebase.setChassisSpeeds(speeds), drivebase)
        .withTimeout(seconds)
        .andThen(stopDrive(drivebase));
  }

  // driveForwardFor: 前進専用の簡易コマンド（vxのみ指定）
  public static Command driveForwardFor(
      SwerveSubsystem drivebase,
      double metersPerSec,
      double seconds) {
    return runChassisFor(drivebase, new ChassisSpeeds(metersPerSec, 0.0, 0.0), seconds);
  }

  // driveOnTagFor: タグ観測中だけ前進する既存処理をAutoイベント用にラップ
  public static Command driveOnTagFor(
      SwerveSubsystem drivebase,
      RobotState state,
      double seconds) {
    return new ObservationOKCommand(drivebase)
        .withTimeout(seconds)
        .andThen(stopDrive(drivebase));
  }

  // faceTagFor: タグ方向へ向く既存処理をAutoイベント用にラップ
  public static Command faceTagFor(
      SwerveSubsystem drivebase,
      RobotState state,
      int tagID,
      double seconds) {
    // tagIDは呼び出し側API互換のため保持。現行実装は可視タグから最適対象を選ぶ。
    return new SetZeroCommand(drivebase, state)
        .withTimeout(seconds)
        .andThen(stopDrive(drivebase));
  }

  // logMarker: PathPlannerイベント発火確認ログ
  public static Command logMarker(String markerName) {
    return Commands.print("[AUTO marker] " + markerName);
  }

  // 以下shoot angle開始コマンドのコード
  // L4start などで使う「角度移動開始」だけを残す。
  public static Command shootAngleSetTargetRot(ShootAngleSubsystems shootAngle, double targetRot) {
    return Commands.runOnce(() -> shootAngle.setTargetMotorRot(targetRot), shootAngle);
  }

  // 以下Auto shoot profile実行コマンドのコード
  // 角度到達後に指定RPSで指定秒数だけ回して停止する共通処理。
  public static Command shootWithAngleProfile(
      ShooterSubsystem shooter,
      ShootAngleSubsystems shootAngle,
      double targetAngleRot,
      double targetRps,
      double shootSec,
      String profileName) {
    final double safeShootSec = Math.max(0.0, shootSec);

    return Commands.sequence(
        Commands.print("[AUTO shoot] " + profileName + " start"),
        shootAngleSetTargetRot(shootAngle, targetAngleRot),
        // 角度到達を待つが、2秒で未到達でも次へ進む
        Commands.waitUntil(shootAngle::atTarget).withTimeout(2.0),
        Commands.startEnd(
            () -> shooter.setTargetRps(targetRps),
            shooter::stop,
            shooter).withTimeout(safeShootSec),
        Commands.print("[AUTO shoot] " + profileName + " end"));
  }

  // 以下Auto shooter+feeder連携コマンドのコード
  // 角度を合わせたあと、shooterを先に回し、遅延後にfeederで供給する。
  public static Command shootWithAngleAndFeedProfile(
      ShooterSubsystem shooter,
      ShootAngleSubsystems shootAngle,
      ShooterFeedSubsystem shooterFeed,
      double targetAngleRot,
      double targetRps,
      double feederDelaySec,
      double feederRunSec,
      String profileName) {
    final double safeFeederDelaySec = Math.max(0.0, feederDelaySec);
    final double safeFeederRunSec = Math.max(0.0, feederRunSec);

    return Commands.sequence(
        Commands.print("[AUTO shoot+feed] " + profileName + " start"),
        shootAngleSetTargetRot(shootAngle, targetAngleRot),
        Commands.waitUntil(shootAngle::atTarget).withTimeout(2.0),
        Commands.runOnce(() -> shooter.setTargetRps(targetRps), shooter),
        Commands.waitSeconds(safeFeederDelaySec),
        Commands.startEnd(
            shooterFeed::feedDefault,
            shooterFeed::stop,
            shooterFeed).withTimeout(safeFeederRunSec),
        shooterStop(shooter),
        Commands.print("[AUTO shoot+feed] " + profileName + " end"));
  }

  // start用: shooterを先に回してからfeederを回す
  public static Command startShootWithFeedCommand(
      ShooterSubsystem shooter,
      ShootAngleSubsystems shootAngle,
      ShooterFeedSubsystem shooterFeed) {
    return shootWithAngleAndFeedProfile(
        shooter,
        shootAngle,
        shooterFeed,
        ShootAngleCommandConstants.kAutoStartShotAngleRot,
        ShootAngleCommandConstants.kAutoStartShotRps,
        1.0,
        ShootAngleCommandConstants.kAutoStartShotSec,
        "startshootcommand");
  }

  // outpost用: shooterを先に回してからfeederを回す
  public static Command outpostShootWithFeedCommand(
      ShooterSubsystem shooter,
      ShootAngleSubsystems shootAngle,
      ShooterFeedSubsystem shooterFeed) {
    return shootWithAngleAndFeedProfile(
        shooter,
        shootAngle,
        shooterFeed,
        ShootAngleCommandConstants.kAutoOutpostShotAngleRot,
        ShootAngleCommandConstants.kAutoOutpostShotRps,
        1.0,
        ShootAngleCommandConstants.kAutoOutpostShotSec,
        "outpostshootcommand");
  }

  // 以下shooter停止コマンドのコード
  // safeStopAll から使う停止処理だけを残す。
  public static Command shooterStop(ShooterSubsystem shooter) {
    return Commands.runOnce(shooter::stop, shooter);
  }

  // safeStopAll: Auto終了時の安全停止（ドライブ + シューター）
  public static Command safeStopAll(SwerveSubsystem drivebase, ShooterSubsystem shooter) {
    return Commands.sequence(
        stopDrive(drivebase),
        shooterStop(shooter));
  }
}
