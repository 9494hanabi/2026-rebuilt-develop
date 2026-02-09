// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.subsystems;

import com.pathplanner.lib.auto.AutoBuilder;
import com.pathplanner.lib.commands.PathPlannerAuto;
import com.pathplanner.lib.commands.PathfindingCommand;
import com.pathplanner.lib.config.PIDConstants;
import com.pathplanner.lib.config.RobotConfig;
import com.pathplanner.lib.controllers.PPHolonomicDriveController;

import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.CommandScheduler;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.lib.util.Constants;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
// import edu.wpi.first.math.kinematics.SwerveDriveKinematics;
import edu.wpi.first.math.kinematics.SwerveModulePosition;
import edu.wpi.first.math.kinematics.SwerveDriveOdometry;

import java.io.File;
import java.util.function.Supplier;

import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.Filesystem;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import swervelib.parser.SwerveParser;
import swervelib.telemetry.SwerveDriveTelemetry;
import swervelib.telemetry.SwerveDriveTelemetry.TelemetryVerbosity;
import swervelib.SwerveDrive;
// import swervelib.SwerveInputStream;
// import edu.wpi.first.math.util.Units;

// 254系
import frc.robot.subsystems.vision.VisionFieldPoseEstimate;
import frc.robot.RobotState;

import edu.wpi.first.wpilibj.smartdashboard.Field2d;

// === 担当者 ===
// ひなた
//

public class SwerveSubsystem extends SubsystemBase {

  // SimuGUIに反映させるためのインスタンス。
  private final Field2d field = new Field2d();

  // 起動直後のCommandScheduler overrun回避のため、ウォームアップは必要時のみ有効化する。
  private static final boolean ENABLE_PATHFINDING_WARMUP = false;
  private static final double DASHBOARD_UPDATE_PERIOD_SEC = 0.10;
  // NavXはYAGSLが内部で作成するため、手動で作成しない（二重初期化防止）
  // private SwerveDrivePoseEstimator poseEstimator;

  File directory = new File(Filesystem.getDeployDirectory(),"swerve");
  SwerveDrive  swerveDrive;
  private final SwerveDriveOdometry odometry;

  private final RobotState robotState;
  private double lastDashboardUpdateSec = Double.NEGATIVE_INFINITY;

  public SwerveSubsystem(RobotState robotState) {
    // YAGSLのテレメトリを詳細表示モードにする（デバッグ用の情報を多く出す設定）
    /*＝＝＝＝＝＝＝＝＝＝＝まじ大事＝＝＝＝＝＝＝＝＝＝
    *この下にある SwerveDriveTelemetry.verbosity = TelemetryVerbosity.HIGH;
    *は大会になったら絶対に消すことまじで大事
    *めっちゃデータを送るから消さないとロボットが遅くなる。
    *消す時は SwerveDriveTelemetry.verbosity = TelemetryVerbosity.NONE;　にする
    */
    // パフォーマンス向上のためLOWに設定（HIGHは大量データ送信でループ遅延の原因）
    // デバッグ時のみHIGHに戻すこと
    SwerveDriveTelemetry.verbosity = TelemetryVerbosity.LOW;

    // ステートのイニシャライズ
    this.robotState = robotState;

    SmartDashboard.putData("Field", field);

    try
    {
      swerveDrive =
          new SwerveParser(directory)
              .createSwerveDrive(Constants.maxSpeed, Constants.FieldConstants.kInitialFieldToRobotPose);
    } catch (Exception e)
    {
      throw new RuntimeException(e);
    }
    odometry = new SwerveDriveOdometry(
        swerveDrive.kinematics,
        swerveDrive.getYaw(),
        swerveDrive.getModulePositions(),
        swerveDrive.getPose());
    setupPathPlanner();
  }

  // ====================================Planner========================================
  public void setupPathPlanner()
  {
    File settingsFile = new File(Filesystem.getDeployDirectory(), "pathplanner/settings.json");
    if (!settingsFile.exists()) {
      DriverStation.reportWarning(
          "PathPlanner settings.json が見つからないため AutoBuilder をスキップします。", false);
      return;
    }

    // GUI設定からRobotConfigを読み込みます。
    // これを定数ファイルに保存すべきです
    RobotConfig config;
    try
    {
      config = RobotConfig.fromGUISettings();

      final boolean enableFeedforward = true;
      // AutoBuilder を最後に設定する
      AutoBuilder.configure(
          swerveDrive::getPose,
          // ロボットポーズ提供元
          swerveDrive::resetOdometry,
          // 走行距離計のリセット方法（車両に開始姿勢がある場合に呼び出されます）
          swerveDrive::getRobotVelocity,
          // シャーシ速度の供給元。ロボット相対でなければならない
          (speedsRobotRelative, moduleFeedForwards) -> {
            if (enableFeedforward)
            {
              swerveDrive.drive(
                  speedsRobotRelative,
                  swerveDrive.kinematics.toSwerveModuleStates(speedsRobotRelative),
                  moduleFeedForwards.linearForces()
                               );
            } else
            {
              swerveDrive.setChassisSpeeds(speedsRobotRelative);
            }
          },
          // ROBOT RELATIVE ChassisSpeeds に基づいてロボットを駆動するメソッド。オプションで個々のモジュールのフィードフォワードを出力可能。
          new PPHolonomicDriveController(
              // PPHolonomicControllerは、ホロノミック駆動系向けの組み込みパス追従制御器です
              new PIDConstants(5.0, 0.0, 0.0),
              // PID定数
              new PIDConstants(5.0, 0.0, 0.0)
              // 回転PID定数
          ),
          config,
          // The robot configuration
          () -> {
            // 赤アライアンス向けに経路を反転させるタイミングを制御するブール値の提供元
            // これにより、追跡中の経路がフィールドの赤アライアンスへ反転する。
            // 起点（オリジン）は青アライアンスに残る

            var alliance = DriverStation.getAlliance();
            if (alliance.isPresent())
            {
              return alliance.get() == DriverStation.Alliance.Red;
            }
            return false;
          },
          this
          // このサブシステムを参照して要件を設定する
                           );
    } catch (Exception e)
    {
      DriverStation.reportWarning(
          "PathPlanner 初期化に失敗したため AutoBuilder を無効化します: " + e.getMessage(), false);
      return;
    }
    // PathPlannerの経路探索プリロードはCPU負荷が高く、
    // 起動直後にループオーバーランを誘発するためデフォルトでは無効化。
    if (ENABLE_PATHFINDING_WARMUP) {
      CommandScheduler.getInstance().schedule(PathfindingCommand.warmupCommand());
    }
  }

  //イベント付きのパスフォロワーを取得する。
  // @param PathPlannerAuto(〜〜〜) 〜〜〜はPathPlannerの.atuoのパス名。
  // ====================================Autonomous=========================================
  public Command getAutonomousCommand(String pathName)
  {
    // AutoBuilderを使用してパス追跡コマンドを作成します。これによりイベントマーカーもトリガーされます。
    return new PathPlannerAuto(pathName);
  }
  //===========PathPlannreの設定終わり==============


  public Command exampleMethodCommand() {
    return runOnce(
        () -> {
        });
  }


  public boolean exampleCondition() {
    return false;
  }

  @Override
  public void periodic() {

    field.setRobotPose(swerveDrive.getPose());

    double ts = Timer.getFPGATimestamp();
    Pose2d pose = getSwerveDrive().getPose();
    Pose2d odomPose = odometry.update(swerveDrive.getYaw(), swerveDrive.getModulePositions());
    var gyroRotation3d = swerveDrive.getGyroRotation3d();
    ChassisSpeeds measuredRobotRelativeSpeeds = swerveDrive.getRobotVelocity();
    ChassisSpeeds measuredFieldRelativeSpeeds =
        ChassisSpeeds.fromRobotRelativeSpeeds(
            measuredRobotRelativeSpeeds.vxMetersPerSecond,
            measuredRobotRelativeSpeeds.vyMetersPerSecond,
            measuredRobotRelativeSpeeds.omegaRadiansPerSecond,
            pose.getRotation());

    robotState.addOdometryMeasurement(ts, pose);
    robotState.addOdometryOnlyMeasurement(ts, odomPose);
    robotState.addDriveMotionMeasurements(
        ts,
        0.0,
        0.0,
        measuredRobotRelativeSpeeds.omegaRadiansPerSecond,
        gyroRotation3d.getY(),
        gyroRotation3d.getX(),
        0.0,
        0.0,
        measuredRobotRelativeSpeeds,
        measuredFieldRelativeSpeeds,
        measuredRobotRelativeSpeeds,
        measuredFieldRelativeSpeeds,
        measuredFieldRelativeSpeeds);

    // SmartDashboard更新を間引いてNT帯域とCPU負荷を抑える。
    if (ts - lastDashboardUpdateSec >= DASHBOARD_UPDATE_PERIOD_SEC) {
      lastDashboardUpdateSec = ts;
      SmartDashboard.putNumber("IMU Yaw", swerveDrive.getYaw().getDegrees());
      SmartDashboard.putNumber("IMU Pitch", Math.toDegrees(gyroRotation3d.getY()));
      SmartDashboard.putNumber("IMU Roll", Math.toDegrees(gyroRotation3d.getX()));
    }
  }

  @Override
  public void simulationPeriodic() {
  }

  public SwerveDrive getSwerveDrive() {
    return swerveDrive;
  }

  public void driveFieldOriented(ChassisSpeeds velocity) {
    swerveDrive.driveFieldOriented(velocity);
  }
  public Command driveFieldOriented(Supplier<ChassisSpeeds> velocity) {
    return run(() -> {
      swerveDrive.driveFieldOriented(velocity.get());
    });
  }
  private SwerveModulePosition[] getModulePositions() {
    // 各モジュールのdrive距離と角度を返す（あなたの実装）
    return new SwerveModulePosition[] { /* FL, FR, BL, BR */ };
  }

  // SwerveSubsystem に追加
  public void setChassisSpeeds(ChassisSpeeds speeds) {
    swerveDrive.setChassisSpeeds(speeds);
  }

  public void addVisionMeasurement(VisionFieldPoseEstimate est) {
    // YAGSLのPoseEstimatorへ注入
    swerveDrive.addVisionMeasurement(
      est.getVisionRobotPoseMeters(),
      est.getTimestampSeconds(),
      est.getVisionMeasurementsStdDevs()
    );
  }
}
