# DriveControllCommand 定数分離 提案

## 現状分析

`DriveControllCommand.java` が利用している定数は以下の3カテゴリに分類できる。

### 1. PID ゲイン・許容値 (既に `PIDConstants` に分離済み)

| 定数名 | 現在の定義場所 | 用途 |
|--------|---------------|------|
| `kTranslationKp/Ki/Kd` | `PIDConstants` | Translation PID ゲイン |
| `kHeadingKp/Ki/Kd` | `PIDConstants` | Heading PID ゲイン |
| `kTranslationToleranceMeters` | `PIDConstants` | Translation 位置許容誤差 |
| `kTranslationVelocityToleranceMPerSec` | `PIDConstants` | Translation 速度許容誤差 |
| `kTranslationIntegralContributionLimit` | `PIDConstants` | Translation I値出力上限 |
| `kHeadingToleranceRad` | `PIDConstants` | Heading 角度許容誤差 |
| `kHeadingVelocityToleranceRadPerSec` | `PIDConstants` | Heading 角速度許容誤差 |
| `kHeadingIntegralContributionLimit` | `PIDConstants` | Heading I値出力上限 |

→ **対応不要** (既に分離済み)

### 2. 速度上限 (既に `SemiAutoConstants` に分離済み)

| 定数名 | 現在の定義場所 | 用途 |
|--------|---------------|------|
| `velocityMaximum` | `SemiAutoConstants` | 並進速度の上限 [m/s] |
| `omegaMaximum` | `SemiAutoConstants` | 回転速度の上限 [rad/s] |

→ **対応不要** (既に分離済み)

### 3. ログ出力周期 (コマンド内にハードコード)

| 定数名 | 現在の定義場所 | 値 | 用途 |
|--------|---------------|-----|------|
| `kStatusLogPeriodSec` | `DriveControllCommand` 内 | `1.00` | 通常ステータスログの出力間隔 [秒] |
| `kWarningLogPeriodSec` | `DriveControllCommand` 内 | `1.00` | 警告ログの出力間隔 [秒] |

→ **分離対象**

---

## 提案: 分離の方針

### 方針A: `commandconstants/DriveControllCommandConstants.java` に分離 (推奨)

既存の `commandconstants/SetToTagCommandConstants.java` と同じパターンに従い、コマンド専用定数クラスを作る。

```
frc/robot/lib/constants/
├── Constants.java
├── FieldConstants.java
├── PIDConstants.java
├── SemiAutoConstants.java
├── VisionConstants.java
└── commandconstants/
    ├── SetToTagCommandConstants.java     ← 既存
    └── DriveControllCommandConstants.java ← 新規
```

```java
package frc.robot.lib.constants.commandconstants;

public final class DriveControllCommandConstants {

    private DriveControllCommandConstants() {}

    /** 通常ステータスログの出力間隔 [秒] */
    public static final double kStatusLogPeriodSec = 1.00;

    /** 警告ログの出力間隔 [秒] */
    public static final double kWarningLogPeriodSec = 1.00;
}
```

**メリット:**
- 既存の `commandconstants` パッケージの命名規則に一致
- コマンドごとに定数ファイルが1対1で対応し、探しやすい
- 将来このコマンドに固有の定数が増えても、追加先が明確

**デメリット:**
- 現時点では定数が2つだけなので、ファイルとしては小さい

### 方針B: ログ周期はコマンド内に残す

`kStatusLogPeriodSec` と `kWarningLogPeriodSec` はこのコマンド固有のログ制御パラメータであり、他コマンドと共有する性質のものではない。現状の `private static final` のままにする。

**メリット:**
- ファイル数が増えない
- ログ周期はコマンドの振る舞いに密接で、分離する必要性が低い

**デメリット:**
- 一貫性の観点では、他のコマンドと方針がずれる可能性がある

---

## 推奨: 方針A

理由:
1. **既存パターンとの一貫性** — `commandconstants/` パッケージが既に存在し、コマンド固有定数の置き場として確立されている
2. **将来の拡張性** — DriveControllCommand に新しいチューニングパラメータ(例: デッドバンド、タイムアウト閾値 等)が増えた場合、追加先が明確
3. **テスト容易性** — 定数を外部化しておくことで、テスト時に値を確認・比較しやすい

---

## 変更後の DriveControllCommand.java (差分イメージ)

```java
// 追加 import
import static frc.robot.lib.constants.commandconstants.DriveControllCommandConstants.*;

// 削除される行:
// - private static final double kStatusLogPeriodSec = 1.00;
// - private static final double kWarningLogPeriodSec = 1.00;

// 利用箇所はそのまま (定数名が同じなので変更不要)
```

---

## 補足: OdomHeadingController / OdomTranslationController 内の定数について

これら2クラス内にもハードコードされたデフォルト値がある:

| クラス | 定数 | 値 |
|--------|------|----|
| `OdomHeadingController` | `kDefaultMinEffectiveOmegaRadPerSec` | `0.35` |
| `OdomHeadingController` | `kDefaultMinOmegaEnableErrorRad` | `toRadians(4.0)` |
| `OdomHeadingController` | `kDefaultOdomOmegaSign` | `-1.0` |
| `OdomTranslationController` | `kDefaultMinEffectiveTranslationMeterPerSec` | `0.05` |
| `OdomTranslationController` | `kDefaultMinTranslationEnableErrorMeter` | `0.1` |
| `OdomTranslationController` | `kDefaultOdomTranslationXSign` | `1.0` |
| `OdomTranslationController` | `kDefaultOdomTranslationYSign` | `1.0` |

これらは各コントローラのデフォルト動作を定義するものであり、**コンストラクタ引数で上書き可能**なため、現状はクラス内に残す形で問題ない。ただし、ロボットごとのキャリブレーション値（特に `odomOmegaSign`, `odomTranslationXSign/YSign`）を別ロボットでも再利用する場合は、将来的に `PIDConstants` や新設の `OdomConstants` への移動を検討してもよい。
