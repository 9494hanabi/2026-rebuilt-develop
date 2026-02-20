// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.commands;

import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import frc.robot.subsystems.SwerveSubsystem;
import frc.robot.subsystems.TurretSubsystem;
import frc.robot.RobotState;
import frc.robot.commands.debug.vision.AbsoluteDriveOKCommand;
import frc.robot.commands.debug.vision.ObservationOKCommand;

// === 担当者 ===
// はるた
//

public final class AutoCommand {
  private AutoCommand() {
    throw new UnsupportedOperationException("This is a utility class!");
  }

  public static Command doNothing() {
    return Commands.none();
  }

  public static Command stopDrive(SwerveSubsystem drivebase) {
    return Commands.runOnce(
        () -> drivebase.setChassisSpeeds(new ChassisSpeeds(0.0, 0.0, 0.0)),
        drivebase);
  }

  public static Command pathPlannerAuto(SwerveSubsystem drivebase, String autoName) {
    return Commands.sequence(
        Commands.print("[AUTO] start: " + autoName),
        drivebase.getAutonomousCommand(autoName),
        stopDrive(drivebase),
        Commands.print("[AUTO] end: " + autoName));
  }

  // 指定時間だけ任意のChassisSpeedsで走る共通メソッド。
  // PathPlannerのイベントマーカーから使う「短い動作コマンド」を作る用途。
  public static Command runChassisFor(
    SwerveSubsystem drivebase,
    ChassisSpeeds speeds,
    double seconds
  ) {
    return Commands.run(
      // 毎ループ、指定速度を出し続ける
      () -> drivebase.setChassisSpeeds(speeds),
      drivebase)
    // 指定秒数で自動終了
    .withTimeout(seconds)
    // 終了時に必ず停止して安全側に倒す
    .andThen(stopDrive(drivebase));
  }

  // 前進専用のショートかっと
  public static Command driveForwardFor(
    SwerveSubsystem drivebase,
    double metersPerSec,
    double seconds
  ) {
  return runChassisFor(drivebase, new ChassisSpeeds(metersPerSec, 0.0, 0.0), seconds);
  }

  // Limelightでタグ検出中だけ前進する既存コマンドを、Autoイベント用に包んだもの。
  // タイムアウトと停止処理を必ず付ける。
  public static Command driveOnTagFor(
    SwerveSubsystem drivebase,
    RobotState state,
    double seconds
    ) {
      return new ObservationOKCommand(drivebase)
      .withTimeout(seconds)
      .andThen(stopDrive(drivebase));
      }

  // LimelightでAprilTagへ向くコマンドをAutoイベント用に書き換えたやつ
  // timeoutで必ず抜けて、最後に停止する。
  public static Command faceTagFor(
    SwerveSubsystem drivebase,
    RobotState state,
    int tagID,
    double seconds
  ) {
    // tagIDは呼び出し側API互換のため保持。現行実装は可視タグから最適対象を選ぶ。
    return new AbsoluteDriveOKCommand(drivebase, state)
      .withTimeout(seconds)
      .andThen(stopDrive(drivebase));
    }

  // 「タグへ向く　->　タグに向かって前進」の前進のやつ
  //　イベント一個で呼べるようにしておくことでPathplannerとの連携が楽になる
  public static Command alignAndApproachTag(
    SwerveSubsystem drivebase,
    RobotState state,
    int tagID,
    double alignSec,
    double approachSec
  ) {
    return Commands.sequence(
      faceTagFor(drivebase, state, tagID, alignSec),
      driveOnTagFor(drivebase, state, approachSec),
      stopDrive(drivebase)
    );
  }

  // ---- タレットのシーケンス ----
  // タレットを指定角度へ向けるコマンド。
  // 1) 制御器を現在角度にリセット
  // 2) 目標角を設定
  // 3) 到達待ち（timeout付き）
  // 4) 最後に停止
  public static Command turretToAngle(
    TurretSubsystem turret,
    double angleDeg,
    double timeoutSec
  ) {
    return Commands.sequence(
      Commands.runOnce(() -> {
        turret.resetControllerToCurrentAngle();
        turret.setGoalDeg(angleDeg);
      }, turret),
      Commands.waitUntil(turret::atGoal).withTimeout(timeoutSec),
      Commands.runOnce(turret::stop, turret)
    );
  }

  // タレットを格納位置まで戻すコマンド(0.0度)
  public static Command turretStow(TurretSubsystem turret) {
    return turretToAngle(turret, 0.0, 1.5);
  }

  // PathPlannerイベントの発火確認用ログ。
  // AdvantageScope/コンソールで「どのマーカーが呼ばれたか」を追える。
  public static Command logMarker(String markerName) {
    return Commands.print("[AUTO marker] " + markerName);
  }

  // Auto終了時の安全停止。
  // 走行とタレットを明示的に止める。
  public static Command safeStopAll(SwerveSubsystem drivebase, TurretSubsystem turret) {
    return Commands.sequence(
      stopDrive(drivebase),
      Commands.runOnce(turret::stop, turret)
    );
  }

  // scoreCycleBasicにタイムアウトと安全停止を付けた運用版。
  // シーケンスが長引いても必ず抜ける。
  public static Command scoreCycleBasicSafe(
    SwerveSubsystem drivebase,
    RobotState state,
    TurretSubsystem turret
  ) {
    return scoreCycleBasic(drivebase, state, turret)
      .withTimeout(5.0)
      .andThen(safeStopAll(drivebase, turret));
  }

// FaceAprilTagCommandを呼び出す
  // 発射機構をまだ作ってないから、simで確認するために wait して shootをprint して疑似発射
  public static Command scoreCycleBasic(
    SwerveSubsystem drivebase,
    RobotState state,
    TurretSubsystem turret
  ) {
    return Commands.sequence(
      Commands.print("[AUTOモード] scoreCycleBasic始まるよ〜"),

      // タグへ向く（1.0秒）→ 秒数は変えちゃっておk
      faceTagFor(drivebase, state, 1, 1.0),

      // タレットを得点角へ向ける（今は20度）
      turretToAngle(turret, 20.00, 1.2),

      // 擬似発射
      Commands.print("[AUTOモード] はっしゃ〜〜〜〜"),
      Commands.waitSeconds(0.20),

      // タレットを格納・停止
      turretStow(turret),
      stopDrive(drivebase),

      Commands.print("[AUTOモード] scoreCycleBasic終わるよ〜")
    );
  }
}
