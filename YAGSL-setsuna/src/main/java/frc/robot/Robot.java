package frc.robot;

import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.CommandScheduler;

// === 担当者 ===
// はるた
//

public class Robot extends TimedRobot {
  private Command m_autonomousCommand;
  private final RobotContainer m_robotContainer;

  //以下スマートダッシュボードを使ったAutoのこと
  private static final String kDefaultAuto = "Default";
  private static final String kCustomAuto = "My Auto";
  private String m_autoSelected;
  private final SendableChooser<String> m_chooser = new SendableChooser<>();


  public Robot() {
    m_robotContainer = new RobotContainer();
    System.out.println("RobotContainer" +  m_robotContainer);

    //smartdashboadで出てくる選択肢
    m_chooser.setDefaultOption("Default Auto", kDefaultAuto);
    m_chooser.addOption("My Auto", kCustomAuto);

    // スマートダッシュボードにウェジットを追加する（名前：Auto setting）
    SmartDashboard.putData("Auto setting", m_chooser);
  }

  @Override
  public void robotPeriodic() {
    CommandScheduler.getInstance().run();
  }

  @Override
  public void disabledInit() {}

  @Override
  public void disabledPeriodic() {}

  //　Init：一度だけ呼ばれるプログラム
  @Override
  public void autonomousInit() {
    //始まる時にダッシュボードで選択する → m_autoSelectedに代入
    m_autoSelected = m_chooser.getSelected();
    System.out.println("Auto selected: " + m_autoSelected);

    //getAutonomousCommand()が返してきたコマンドを、そのまま schedule する
    m_autonomousCommand = m_robotContainer.getAutonomousCommand();

    // schedule the autonomous command (example)
    if (m_autonomousCommand != null) {
      m_autonomousCommand.schedule();
    }
  }

  // Periodic：エネイブル中に約20msごとに繰り返し呼ばれる関数
  @Override
  public void autonomousPeriodic() {
    //選択内容によっての分岐
    switch (m_autoSelected) {
      case kCustomAuto:
        // Put custom auto code here
        // カスタム自律の動き
        break;
      case kDefaultAuto:
      default:
        // Put default auto code here
        // カスタム自律の動き
        break;
    }
  }

  @Override
  public void teleopInit() {
    if (m_autonomousCommand != null) {
      m_autonomousCommand.cancel();
    }

    // ・このコードは、テレオペが始まったときに自律が確実に止まるようにするもの
    // ・自律を「他のコマンドに割り込まれるまで」動かし続けたいなら、この行を消すかコメントアウトする
    if (m_autonomousCommand != null) {
      m_autonomousCommand.cancel();
    }
  }
  
  @Override
  public void teleopPeriodic() {}

  @Override
  public void testInit() {
    CommandScheduler.getInstance().cancelAll();
  }

  @Override
  public void testPeriodic() {}

  @Override
  public void simulationInit() {}

  @Override
  public void simulationPeriodic() {}
}
