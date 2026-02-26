package frc.robot;

import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import frc.robot.RobotContainer;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.CommandScheduler;
import edu.wpi.first.wpilibj2.command.Commands;
import frc.robot.lib.util.LogCleanupService;


// === 担当者 ===
// はるた
//

public class Robot extends TimedRobot {
  private Command m_autonomousCommand;
  private final RobotContainer m_robotContainer;
  private final LogCleanupService logCleanupService = new LogCleanupService();

  // 以下smartdashboadを使ったAutoのこと
  private static final String kDoNothingAuto = "Do Nothing";
  private static final String kMyAuto = "New New Auto";
  private String m_autoSelected;
  private final SendableChooser<String> m_chooser = new SendableChooser<>();

  public Robot() {
    m_robotContainer = new RobotContainer();
    System.out.println("RobotContainer" +  m_robotContainer);

    // smartdashboadで出てくる選択肢
    m_chooser.setDefaultOption("New New Auto", kMyAuto);
    m_chooser.addOption("Do Nothing", kDoNothingAuto);

    // smartdashboadにウェジットを追加する（名前：Auto setting）
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
    m_autonomousCommand = m_robotContainer.getAutonomousCommand();
    if (m_autonomousCommand == null) {
      m_autonomousCommand = Commands.none();
    }
    CommandScheduler.getInstance().schedule(m_autonomousCommand);
  }

  // Periodic：エネイブル中に約20msごとに繰り返し呼ばれる関数
  @Override
  public void autonomousPeriodic() {
  }

  @Override
  public void teleopInit() {
    // このコードは、テレオペが始まったときに自律が確実に止まるようにするもの
    // 自律を「他のコマンドに割り込まれるまで」動かし続けたいなら、この行を消すかコメントアウトする
    if (m_autonomousCommand != null) {
      CommandScheduler.getInstance().cancel(m_autonomousCommand);
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
