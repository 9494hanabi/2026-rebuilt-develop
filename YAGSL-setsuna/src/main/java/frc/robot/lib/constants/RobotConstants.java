package frc.robot.lib.constants;

public class RobotConstants {
    // ===== ロボット/シューター物理パラメータ（実機で要調整） =====
    public static final double kRollerAxisHeightMeters = 0.405;    // ローラー軸の地上高 [m]
    public static final double kRollerToBallCenterMeters = 0.1135; // ローラー軸からボール中心までの距離 [m] (11.35cm)

    // ===== シューターギア/ローラー =====
    // モーター側ギア半径15mm → ローラー側ギア半径22mm
    public static final double kShooterGearRatio = 15.0 / 22.0;   // ローラーRPS = モーターRPS × この値

    // ローラーは楕円形: 長径100mm(半径50mm) / 短径50mm(半径25mm)
    // Hood側は直径100mmの円形
    // ボールは長径側で最も強く加速されるため、有効半径 ≈ 長径側半径
    public static final double kRollerEffectiveRadiusMeters = 0.050; // 有効半径 [m] (実機で要調整)

    public static final double kSpinEfficiency = 0.70;             // ホイール→ボール速度の変換効率 (0~1)
}