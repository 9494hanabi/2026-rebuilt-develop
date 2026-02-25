package frc.robot.lib.constants;

// 担当晴太

public final class ControlConstants {
    private ControlConstants() {}

    public enum DriverControllerType {
        XBOX,
        LOGITECH
    }

    // 使用するドライバーコントローラー種別をここで切り替える
    public static final DriverControllerType kDriverControllerType = DriverControllerType.LOGITECH;

    public static final int kDriverControllerPort = 0;
    public static final double kDeadband = 0.08;
    public static final double kTriggerPressedThreshold = 0.5;

    // Xbox標準マッピング
    public static final class XboxMapping {
        private XboxMapping() {}

        public static final int kLeftXAxis = 0;
        public static final int kLeftYAxis = 1;
        public static final int kLeftTriggerAxis = 2;
        public static final int kRightXAxis = 4;
        public static final int kRightYAxis = 5;

        public static final int kAButton = 1;
        public static final int kBButton = 2;
        public static final int kXButton = 3;
        public static final int kYButton = 4;
        public static final int kBackButton = 7;
        public static final int kStartButton = 8;

        // シュートよう
        public static final int kRightTriggerAxis = 3;
        public static final int kLeftBumperButton = 5;
        public static final int kRightBumperButton = 6;

    }

    // Logitech F310 (XInput) を想定したマッピング
    public static final class LogitechMapping {
        private LogitechMapping() {}

        public static final int kLeftXAxis = 0;
        public static final int kLeftYAxis = 1;
        public static final int kLeftTriggerAxis = 2;
        public static final int kRightXAxis = 4;
        public static final int kRightYAxis = 5;

        public static final int kAButton = 1;
        public static final int kBButton = 2;
        public static final int kXButton = 3;
        public static final int kYButton = 4;
        public static final int kBackButton = 7;
        public static final int kStartButton = 8;

        // シューター用
        public static final int kRightTriggerAxis = 3;
        public static final int kLeftBumperButton = 5;
        public static final int kRightBumperButton = 6;

    }
}
