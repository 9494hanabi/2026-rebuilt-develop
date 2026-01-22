// src/main/java/frc/robot/subsystems/ElevatorSubsystem.java
package frc.robot.subsystems;

import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.motorcontrol.MotorController;
import edu.wpi.first.wpilibj.motorcontrol.PWMSparkMax;
import edu.wpi.first.wpilibj2.command.SubsystemBase;

/* === 担当者 ===
 * はるた
 */
public class ElevatorSubsystem extends SubsystemBase {

  // エレベータ用モータ
  private final MotorController m_elevatorMotor;

  public ElevatorSubsystem() {
    // 実際のポート番号に合わせて変更する
    m_elevatorMotor = new PWMSparkMax(0);
  }

  /**
   * エレベータに出力を与える
   * @param power -1.0 ～ 1.0 の範囲
   */
  public void setPower(double power) {
    m_elevatorMotor.set(power);
  }

  /** エレベータのモータを停止する */
  public void stop() {
    m_elevatorMotor.stopMotor();
  }
}
