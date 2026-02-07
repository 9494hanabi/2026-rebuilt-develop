package frc.robot;

import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.CommandScheduler;
import com.pathplanner.lib.commands.PathPlannerAuto;
import edu.wpi.first.wpilibj2.command.Commands;


// === 担当者 ===
// はるた
//

public class Robot extends TimedRobot {
  private Command m_autonomousCommand;
  private final RobotContainer m_robotContainer;
  private final LogCleanupService logCleanupService = new LogCleanupService();

  //以下スマートダッシュボードを使ったAutoのこと
  private static final String kDoNothingAuto = "Do Nothing";
  private static final String kMyAuto = "My Auto";
  private String m_autoSelected;
  private final SendableChooser<String> m_chooser = new SendableChooser<>();


  public Robot() {
    m_robotContainer = new RobotContainer();
    System.out.println("RobotContainer" +  m_robotContainer);

    //smartdashboadで出てくる選択肢
    m_chooser.setDefaultOption("Do Nothing", kDoNothingAuto);
    m_chooser.addOption("My Auto", kMyAuto);

    // スマートダッシュボードにウェジットを追加する（名前：Auto setting）
    SmartDashboard.putData("Auto setting", m_chooser);
  }

  @Override
  public void robotPeriodic() {
    CommandScheduler.getInstance().run();
    logCleanupService.periodic();
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

    switch (m_autoSelected) {
      case kMyAuto:
        m_autonomousCommand = new PathPlannerAuto(kMyAuto);
        break;
      case kDoNothingAuto:
      default:
        m_autonomousCommand = Commands.none();
        break;
    }
    // schedule the autonomous command (example)
    if (m_autonomousCommand != null) {
      m_autonomousCommand.schedule();
    }
  }

  // Periodic：エネイブル中に約20msごとに繰り返し呼ばれる関数
  @Override
  public void autonomousPeriodic() {
  }

  @Override
  public void teleopInit() {
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
