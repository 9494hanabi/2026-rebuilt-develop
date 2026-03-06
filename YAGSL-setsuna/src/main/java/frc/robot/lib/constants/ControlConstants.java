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

    // バインディング有効/無効切り替え
    public static final boolean kEnableDriveBindings = false;
    public static final boolean kEnableAutoBindings = false;
    public static final boolean kEnableDebugBindings = false;
    public static final boolean kEnableTrajectoryBindings = true;

    // 以下HUB正対制御の定数
    // HUB座標が未確定の間は、フィールド中心を仮の目標点として使う。
    // 実際のHUB座標が分かったら、この定数だけ差し替える。
    public static final edu.wpi.first.math.geometry.Pose2d kHubAimFieldPose =
            new edu.wpi.first.math.geometry.Pose2d(
                    FieldConstants.fieldLengthMeter / 2.0,
                    FieldConstants.fieldWidthMeter / 2.0,
                    new edu.wpi.first.math.geometry.Rotation2d());

    // 左スティック入力がこの値を超えた時だけ、HUBへ向く自動回頭を有効にする。
    public static final double kHubAimEnableTranslationDeadband = 0.12;
    // 右スティックで明示的に回したい時は、自動回頭を止めるための上書き閾値。
    public static final double kHubAimManualOverrideDeadband = 0.10;

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
        public static final int kRightStickButton = 4;

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
        public static final int kRightStickButton = 4;
    }
}
