// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.subsystems;
import static edu.wpi.first.units.Units.Meter;

import com.pathplanner.lib.auto.AutoBuilder;
import com.pathplanner.lib.commands.PathPlannerAuto;
import com.pathplanner.lib.commands.PathfindingCommand;
import com.pathplanner.lib.config.PIDConstants;
import com.pathplanner.lib.config.RobotConfig;
import com.pathplanner.lib.controllers.PPHolonomicDriveController;

import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.lib.limelight.LimelightHelpers;
import frc.robot.lib.util.Constants;
import edu.wpi.first.math.estimator.SwerveDrivePoseEstimator;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
// import edu.wpi.first.math.kinematics.SwerveDriveKinematics;
import edu.wpi.first.math.kinematics.SwerveModulePosition;

import java.io.File;
import java.util.function.Supplier;

import com.studica.frc.AHRS;

import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.Filesystem;
import swervelib.parser.SwerveParser;
import swervelib.telemetry.SwerveDriveTelemetry;
import swervelib.telemetry.SwerveDriveTelemetry.TelemetryVerbosity;
import swervelib.SwerveDrive;
// import swervelib.SwerveInputStream;
import edu.wpi.first.math.geometry.Rotation2d;
// import edu.wpi.first.math.util.Units;

// === 担当者 ===
// はるた
//

public class SwerveSubsystem extends SubsystemBase {
  /** Creates a new ExampleSubsystem. */
  private final AHRS navx = new AHRS(AHRS.NavXComType.kMXP_SPI);
  // private SwerveDrivePoseEstimator poseEstimator;

  File directory = new File(Filesystem.getDeployDirectory(),"swerve");
  SwerveDrive  swerveDrive;

  public SwerveSubsystem() {
    // YAGSLのテレメトリを詳細表示モードにする（デバッグ用の情報を多く出す設定）
    /*＝＝＝＝＝＝＝＝＝＝＝まじ大事＝＝＝＝＝＝＝＝＝＝
    *この下にある SwerveDriveTelemetry.verbosity = TelemetryVerbosity.HIGH;
    *は大会になったら絶対に消すことまじで大事
    *めっちゃデータを送るから消さないとロボットが遅くなる。
    *消す時は SwerveDriveTelemetry.verbosity = TelemetryVerbosity.NONE;　にする
    */
    SwerveDriveTelemetry.verbosity = TelemetryVerbosity.HIGH;

    try
    {
      swerveDrive = new SwerveParser(directory).createSwerveDrive(Constants.maxSpeed,
                                                                  new Pose2d(new Translation2d(Meter.of(1),
                                                                                              Meter.of(4)),
                                                                            Rotation2d.fromDegrees(0)));
      // Alternative method if you don't want to supply the conversion factor via JSON files.
      // swerveDrive = new SwerveParser(directory).createSwerveDrive(maximumSpeed, angleConversionFactor, driveConversionFactor);
    } catch (Exception e)
    {
      throw new RuntimeException(e);
    }
    setupPathPlanner();
  }

  public void setupPathPlanner()
  {
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
      // 必要に応じて例外を処理する
      e.printStackTrace();
    }
    //PathPlannerの経路探索をプリロード
    // カスタム経路探索を使用する場合はこの行の前に追加
    PathfindingCommand.warmupCommand().schedule();
  }

  //イベント付きのパスフォロワーを取得する。
  // @param PathPlannerAuto(〜〜〜) 〜〜〜はPathPlannerの.atuoのパス名。

  public Command getAutonomousCommand(String pathName)
  {
    // AutoBuilderを使用してパス追跡コマンドを作成します。これによりイベントマーカーもトリガーされます。
    return new PathPlannerAuto(pathName);
  }
  //===========PathPlannreの設定終わり==============

  /**
   * Example command factory method.
   *
   * @return a command
   */
  public Command exampleMethodCommand() {
    // Inline construction of command goes here.
    // Subsystem::RunOnce implicitly requires `this` subsystem.
    return runOnce(
        () -> {
          /* one-time action goes here */
        });
  }

  /**
   * An example method querying a boolean state of the subsystem (for example, a digital sensor).
   *
   * @return value of some boolean subsystem state, such as a digital sensor.
   */
  public boolean exampleCondition() {
    // Query some boolean state, such as a digital sensor.
    return false;
  }

  @Override
  public void periodic() {
    // This method will be called once per scheduler run
        // 1) まずはオドメトリ更新（毎周期必須）
    // poseEstimator.update(navx.getRotation2d(), getModulePositions());

    // 2) MegaTag2用にYawをLimelightへ毎周期送る
    double yawDeg = navx.getRotation2d().getDegrees();
    LimelightHelpers.SetRobotOrientation("limelight", yawDeg, 0, 0, 0, 0, 0);

    // 3) MegaTag2取得 → 有効なら融合
    var mt2 = LimelightHelpers.getBotPoseEstimate_wpiBlue_MegaTag2("limelight"); // SetRobotOrientation後に呼ぶ  [oai_citation:5‡Limelight Vision](https://limelightlib-wpijava-reference.limelightvision.io/frc/robot/LimelightHelpers.html?utm_source=chatgpt.com)
    if (mt2 != null && mt2.tagCount > 0) {
      if (mt2.tagCount > 0) {
        swerveDrive.addVisionMeasurement(mt2.pose, mt2.timestampSeconds);
      }
    }
  }

  @Override
  public void simulationPeriodic() {
    // This method will be called once per scheduler run during simulation
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
}
