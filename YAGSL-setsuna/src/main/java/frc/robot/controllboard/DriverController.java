package frc.robot.controllboard;

import edu.wpi.first.wpilibj2.command.button.CommandGenericHID;
import edu.wpi.first.wpilibj2.command.button.Trigger;
import frc.robot.lib.constants.ControlConstants;
import frc.robot.lib.constants.ControlConstants.DriverControllerType;

public class DriverController {
    private final CommandGenericHID hid;

    public DriverController(int port) {
        this.hid = new CommandGenericHID(port);
    }

    public double getLeftX() {
        return hid.getRawAxis(getLeftXAxis());
    }

    public double getLeftY() {
        return hid.getRawAxis(getLeftYAxis());
    }

    public double getRightX() {
        return hid.getRawAxis(getRightXAxis());
    }

    public double getRightY() {
        return hid.getRawAxis(getRightYAxis());
    }

    public Trigger a() {
        return hid.button(getAButton());
    }

    public Trigger b() {
        return hid.button(getBButton());
    }

    public Trigger x() {
        return hid.button(getXButton());
    }

    public Trigger y() {
        return hid.button(getYButton());
    }

    public Trigger start() {
        return hid.button(getStartButton());
    }

    public Trigger back() {
        return hid.button(getBackButton());
    }

    public Trigger leftBumper() {
        return hid.button(getLeftBumperButton());
    }

    public Trigger rightBumper() {
        return hid.button(getRightBumperButton());
    }

    public Trigger rightTrigger() {
        return new Trigger(() -> hid.getRawAxis(getRightTriggerAxis()) > ControlConstants.kTriggerPressedThreshold);
    }


    public Trigger leftTrigger() {
        return new Trigger(() -> hid.getRawAxis(getLeftTriggerAxis()) > ControlConstants.kTriggerPressedThreshold);
    }

    public Trigger povUp() {
        return new Trigger(() -> hid.getHID().getPOV() == 0);
    }

    public Trigger povRight() {
        return new Trigger(() -> hid.getHID().getPOV() == 90);
    }

    public Trigger povDown() {
        return new Trigger(() -> hid.getHID().getPOV() == 180);
    }

    public Trigger povLeft() {
        return new Trigger(() -> hid.getHID().getPOV() == 270);
    }

    private static int getLeftXAxis() {
        return isXbox()
                ? ControlConstants.XboxMapping.kLeftXAxis
                : ControlConstants.LogitechMapping.kLeftXAxis;
    }

    private static int getLeftYAxis() {
        return isXbox()
                ? ControlConstants.XboxMapping.kLeftYAxis
                : ControlConstants.LogitechMapping.kLeftYAxis;
    }

    private static int getRightXAxis() {
        return isXbox()
                ? ControlConstants.XboxMapping.kRightXAxis
                : ControlConstants.LogitechMapping.kRightXAxis;
    }

    private static int getRightYAxis() {
        return isXbox()
                ? ControlConstants.XboxMapping.kRightYAxis
                : ControlConstants.LogitechMapping.kRightYAxis;
    }

    private static int getLeftTriggerAxis() {
        return isXbox()
                ? ControlConstants.XboxMapping.kLeftTriggerAxis
                : ControlConstants.LogitechMapping.kLeftTriggerAxis;
    }

    private static int getRightTriggerAxis() {
    return isXbox()
            ? ControlConstants.XboxMapping.kRightTriggerAxis
            : ControlConstants.LogitechMapping.kRightTriggerAxis;
    }


    private static int getAButton() {
        return isXbox()
                ? ControlConstants.XboxMapping.kAButton
                : ControlConstants.LogitechMapping.kAButton;
    }

    private static int getBButton() {
        return isXbox()
                ? ControlConstants.XboxMapping.kBButton
                : ControlConstants.LogitechMapping.kBButton;
    }

    private static int getXButton() {
        return isXbox()
                ? ControlConstants.XboxMapping.kXButton
                : ControlConstants.LogitechMapping.kXButton;
    }

    private static int getYButton() {
        return isXbox()
                ? ControlConstants.XboxMapping.kYButton
                : ControlConstants.LogitechMapping.kYButton;
    }

    private static int getBackButton() {
        return isXbox()
                ? ControlConstants.XboxMapping.kBackButton
                : ControlConstants.LogitechMapping.kBackButton;
    }

    private static int getStartButton() {
        return isXbox()
                ? ControlConstants.XboxMapping.kStartButton
                : ControlConstants.LogitechMapping.kStartButton;
    }

    private static int getLeftBumperButton() {
    return isXbox()
            ? ControlConstants.XboxMapping.kLeftBumperButton
            : ControlConstants.LogitechMapping.kLeftBumperButton;
}

    private static int getRightBumperButton() {
        return isXbox()
            ? ControlConstants.XboxMapping.kRightBumperButton
            : ControlConstants.LogitechMapping.kRightBumperButton;
    }


    private static boolean isXbox() {
        return ControlConstants.kDriverControllerType == DriverControllerType.XBOX;
    }
    public Trigger rightStick() { return hid.button(getRightStickButton()); }

    private static int getRightStickButton() {
        return isXbox()
        ? ControlConstants.XboxMapping.kRightStickButton
        : ControlConstants.LogitechMapping.kRightStickButton;
}

}
