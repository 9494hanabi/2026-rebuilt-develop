package frc.robot.lib.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.wpi.first.math.controller.PIDController;
import frc.robot.lib.constants.OdomConstants;
import org.junit.jupiter.api.Test;

class OdomTranslationControllerTest {

  private static OdomTranslationController newController(
      PIDController xPid, PIDController yPid, double maxTranslation) {
    xPid.setTolerance(1e-3, 1e-3);
    yPid.setTolerance(1e-3, 1e-3);
    return new OdomTranslationController(xPid, yPid, maxTranslation);
  }

  @Test
  void calculateReturnsEmptyForNonFiniteInputs() {
    var controller =
        newController(new PIDController(1.0, 0.0, 0.0), new PIDController(1.0, 0.0, 0.0), 2.0);

    assertTrue(controller.calculate(Double.NaN, 0.0, 1.0, 1.0).isEmpty());
    assertTrue(controller.calculate(0.0, Double.POSITIVE_INFINITY, 1.0, 1.0).isEmpty());
    assertTrue(controller.calculate(0.0, 0.0, Double.NaN, 1.0).isEmpty());
    assertTrue(controller.calculate(0.0, 0.0, 1.0, Double.NEGATIVE_INFINITY).isEmpty());
  }

  @Test
  void calculateAppliesClampAndConfiguredAxisSigns() {
    var controller =
        newController(new PIDController(4.0, 0.0, 0.0), new PIDController(4.0, 0.0, 0.0), 1.2);

    var result = controller.calculate(0.0, 0.0, 1.0, -1.0).orElseThrow();

    assertEquals(
        OdomConstants.kOdomTranslationXSign * 1.2,
        result.translationXMeterPerSec(),
        1e-9);
    assertEquals(
        OdomConstants.kOdomTranslationYSign * -1.2,
        result.translationYMeterPerSec(),
        1e-9);
  }

  @Test
  void calculateAppliesMinimumEffectiveVelocityWhenErrorIsLarge() {
    var controller =
        newController(new PIDController(0.01, 0.0, 0.0), new PIDController(0.01, 0.0, 0.0), 2.0);

    var result = controller.calculate(0.0, 0.0, 0.2, -0.2).orElseThrow();

    assertEquals(
        OdomConstants.kMinEffectiveTranslationMeterPerSec,
        result.translationXMeterPerSec(),
        1e-9);
    assertEquals(
        -OdomConstants.kMinEffectiveTranslationMeterPerSec,
        result.translationYMeterPerSec(),
        1e-9);
  }

  @Test
  void calculateZerosOutputAtSetpoint() {
    var controller =
        newController(new PIDController(1.0, 0.0, 0.0), new PIDController(1.0, 0.0, 0.0), 2.0);

    var result = controller.calculate(1.0, -2.0, 1.0, -2.0).orElseThrow();

    assertTrue(result.xAtSetpoint());
    assertTrue(result.yAtSetpoint());
    assertEquals(0.0, result.translationXMeterPerSec(), 1e-9);
    assertEquals(0.0, result.translationYMeterPerSec(), 1e-9);
  }
}

