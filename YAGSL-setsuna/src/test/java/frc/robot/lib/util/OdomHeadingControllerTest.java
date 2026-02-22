package frc.robot.lib.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.wpi.first.math.controller.PIDController;
import frc.robot.lib.constants.OdomConstants;
import org.junit.jupiter.api.Test;

class OdomHeadingControllerTest {

  private static OdomHeadingController newController(double kp, double maxOmegaRadPerSec) {
    var pid = new PIDController(kp, 0.0, 0.0);
    pid.setTolerance(1e-3, 1e-3);
    return new OdomHeadingController(pid, maxOmegaRadPerSec);
  }

  @Test
  void calculateReturnsEmptyForNonFiniteInputs() {
    var controller = newController(1.0, 2.0);

    assertTrue(controller.calculate(Double.NaN, 0.0).isEmpty());
    assertTrue(controller.calculate(0.0, Double.POSITIVE_INFINITY).isEmpty());
  }

  @Test
  void calculateUnwrapsCurrentHeadingAcrossWrapBoundary() {
    var controller = newController(1.0, 4.0);

    controller
        .calculate(Math.toRadians(179.0), Math.toRadians(179.0))
        .orElseThrow();
    var result =
        controller
            .calculate(Math.toRadians(-179.0), Math.toRadians(-179.0))
            .orElseThrow();

    assertEquals(Math.toRadians(181.0), result.continuousCurrentHeadingRad(), Math.toRadians(0.2));
  }

  @Test
  void calculateUsesShortestPathForTargetAcrossWrapBoundary() {
    var controller = newController(1.0, 4.0);

    var result =
        controller
            .calculate(Math.toRadians(179.0), Math.toRadians(-179.0))
            .orElseThrow();

    assertEquals(Math.toRadians(2.0), result.headingErrorRad(), Math.toRadians(0.2));
  }

  @Test
  void calculateAppliesClampAndConfiguredOmegaSign() {
    var controller = newController(20.0, 1.2);

    var result = controller.calculate(0.0, Math.PI / 2.0).orElseThrow();

    assertEquals(OdomConstants.kOdomOmegaSign * 1.2, result.omegaRadPerSec(), 1e-9);
  }
}

