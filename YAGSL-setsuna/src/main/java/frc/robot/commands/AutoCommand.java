// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.commands;

import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import frc.robot.RobotState;
import frc.robot.commands.debug.odmetry.SetZeroCommand;
import frc.robot.commands.debug.vision.ObservationOKCommand;
import frc.robot.subsystems.ShooterSubsystem;
import frc.robot.subsystems.SwerveSubsystem;

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

  // alignAndApproachTag: 「向き合わせ -> タグへ寄る」を1イベントで実行
  public static Command alignAndApproachTag(
      SwerveSubsystem drivebase,
      RobotState state,
      int tagID,
      double alignSec,
      double approachSec) {
    return Commands.sequence(
        faceTagFor(drivebase, state, tagID, alignSec),
        driveOnTagFor(drivebase, state, approachSec),
        stopDrive(drivebase));
  }

    // shooterSetTargetRps: 目標RPSをセットしてスピンアップ開始
  public static Command shooterSetTargetRps(ShooterSubsystem shooter, double targetRps) {
    return Commands.runOnce(() -> shooter.setTargetRps(targetRps), shooter);
  }

  // shooterWaitReady: Ready(誤差内150ms維持)になるまで待つ
  public static Command shooterWaitReady(ShooterSubsystem shooter, double timeoutSec) {
    return Commands.waitUntil(shooter::atSpeed).withTimeout(timeoutSec);
  }

  // shooterSpinUpAndWaitReady: スピンアップしてReady成立まで待つ
  public static Command shooterSpinUpAndWaitReady(
      ShooterSubsystem shooter,
      double targetRps,
      double timeoutSec) {
    return Commands.sequence(
        shooterSetTargetRps(shooter, targetRps),
        shooterWaitReady(shooter, timeoutSec));
  }

  // shooterRunAtRpsFor: 指定RPSで指定時間だけ回して停止
  public static Command shooterRunAtRpsFor(
      ShooterSubsystem shooter,
      double targetRps,
      double seconds) {
    return Commands.sequence(
        shooterSetTargetRps(shooter, targetRps),
        Commands.waitSeconds(seconds),
        shooterStop(shooter));
  }

  // shooterShootAtRpsForWithLogs:
  // 1) 目標RPSへスピンアップ
  // 2) Ready待ち(タイムアウト付き)
  // 3) シュート窓(秒)だけ保持
  // 4) 停止
  // ※ Sim/実機どちらでもターミナル確認できるようにログを出す
  public static Command shooterShootAtRpsForWithLogs(
    ShooterSubsystem shooter,
    double targetRps,
    double readyTimeoutSec,
    double shootWindowSec) {
    return Commands.sequence(
        Commands.runOnce(() ->
            System.out.printf("[AUTO shooter] SPINUP target=%.1fRPS timeout=%.2fs%n", targetRps, readyTimeoutSec)),
       shooterSpinUpAndWaitReady(shooter, targetRps, readyTimeoutSec),
       Commands.runOnce(() ->
           System.out.printf(
                "[AUTO shooter] READY=%b actual=%.2fRPS target=%.2fRPS -> SHOOT %.2fs%n",
                shooter.atSpeed(),
                shooter.getVelocityRps(),
                shooter.getTargetRps(),
                shootWindowSec)),
        Commands.waitSeconds(shootWindowSec),
        shooterStop(shooter),
        Commands.runOnce(() ->
            System.out.printf("[AUTO shooter] STOP actual=%.2fRPS%n", shooter.getVelocityRps()))
   ).finallyDo(interrupted -> {
     shooter.stop();
     System.out.printf(
          "[AUTO shooter] FINAL STOP interrupted=%b actual=%.2fRPS%n",
          interrupted, shooter.getVelocityRps());
      });
  }


  // shooterStop: シューター停止
  public static Command shooterStop(ShooterSubsystem shooter) {
    return Commands.runOnce(shooter::stop, shooter);
  }

  // Zoned Event専用: ゾーン中だけ回し、ゾーンを抜けたら必ず停止
  public static Command shooterZoneSpin(ShooterSubsystem shooter, double targetRps) {
    return Commands.startEnd(
        () -> {
          shooter.setTargetRps(targetRps);
          System.out.printf("[AUTO shooter] ZONE START target=%.1fRPS%n", targetRps);
        },
        () -> {
          shooter.stop();
          System.out.printf("[AUTO shooter] ZONE END actual=%.2fRPS%n", shooter.getVelocityRps());
        },
        shooter
    ).finallyDo(interrupted -> shooter.stop());
  }


  // logMarker: PathPlannerイベント発火確認ログ
  public static Command logMarker(String markerName) {
    return Commands.print("[AUTO marker] " + markerName);
  }

  

  // safeStopAll: Auto終了時の安全停止（ドライブ + シューター）
  public static Command safeStopAll(SwerveSubsystem drivebase, ShooterSubsystem shooter) {
    return Commands.sequence(
        stopDrive(drivebase),
        shooterStop(shooter));
  }
}
