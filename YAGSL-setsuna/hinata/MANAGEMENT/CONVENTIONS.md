# コーディング規約・設計ガイドライン

本ドキュメントはyagsl-setsunaプロジェクトのコーディング規約と設計ガイドラインを定義する。

## 技術スタック

| 項目 | 値 |
|---|---|
| 言語 | Java |
| フレームワーク | WPILib (FRC) + YAGSL (Swerve Drive Library) + PathPlanner |
| ビルドツール | Gradle (GradleRIO) |
| テスト | JUnit 5 |
| ログ | AdvantageKit (org.littletonrobotics.junction.Logger) |

---

## 1. 命名規則

| 対象 | 規約 | 例 |
|---|---|---|
| クラス名 | PascalCase | `SwerveSubsystem`, `VisionFieldPoseEstimate` |
| メソッド名 | camelCase | `getSwerveDrive()`, `addOdometryMeasurement()` |
| インスタンス変数 | camelCase（`m_` なし） | `driverController`, `chooser` |
| ローカル変数 | camelCase | `currentPose`, `tagId` |
| 定数（public/private static final） | `k` + PascalCase | `kMaxSpeed`, `kDeadband`, `kDriverControllerPort` |
| enum値 | PascalCase | `Alliance.Red`, `DriveMode.FieldOriented` |
| パッケージ名 | 全小文字、単語連結 | `frc.robot.subsystems.vision` |

### 備考
- `m_` プレフィクスは使用しない。既存の `m_` 使用箇所（Robot.java, RobotContainer.java）は段階的に修正する。
- 既存の `SCREAMING_SNAKE_CASE` 定数（`ENABLE_PATHFINDING_WARMUP` 等）は、新規コードから `k` プレフィクスを適用する。既存コードの一括変更は別TRIALで計画する。

---

## 2. パッケージ構造

```
frc.robot
frc.robot.bindings              # コントローラーバインディング
frc.robot.commands               # Command群
frc.robot.commands.debug         # デバッグ用Command
frc.robot.commands.debug.odmetry # オドメトリデバッグ（※将来odometryに修正予定）
frc.robot.commands.debug.safety  # セーフティデバッグ
frc.robot.commands.debug.vision  # ビジョンデバッグ
frc.robot.controllboard          # コントローラー抽象化（※将来controlboardに修正予定）
frc.robot.lib.constants          # 定数クラス群
frc.robot.lib.constants.commandconstants  # Command固有定数
frc.robot.lib.limelight          # Limelight関連ユーティリティ
frc.robot.lib.pathplanner        # PathPlanner拡張
frc.robot.lib.time               # 時刻ユーティリティ
frc.robot.lib.util               # 汎用ユーティリティ
frc.robot.subsystems             # Subsystem群
frc.robot.subsystems.vision      # Vision Subsystem + IO + データクラス
```

---

## 3. コメント・Javadoc規約

- コメント言語: **日本語**
- publicメソッドには1行の日本語Javadocを推奨（強制しない）
  - 形式: `/** 走行系オドメトリを完全に初期化する。 */`
- `// === 担当者 ===` ヘッダは継続する（チーム運営上の価値がある）
- TODOコメント形式: `// TODO(担当者名): 内容`
- コメントアウトされたコードは残さず削除する（gitに履歴がある）

---

## 4. インポート順序

```java
// 1. edu.wpi.first.* (WPILib)
// 2. com.pathplanner.* (PathPlanner)
// 3. swervelib.* (YAGSL)
// 4. org.* (AdvantageKit等)
// 5. java.* / javax.*
// 6. frc.robot.* (自プロジェクト)
```

- 各グループ間に空行を入れる
- ワイルドカードimport禁止（static importは例外的に許可: 定数クラスのstatic import）

---

## 5. フォーマッティングルール

| 項目 | 規約 |
|---|---|
| インデント | スペース4つ |
| 行幅上限 | 120文字 |
| ブレーススタイル | K&R（開き中括弧は同行末） |
| メソッド間の空行 | 1行 |
| ファイル末尾 | 改行1つ |

---

## 6. Command-Based設計ガイドライン

### 6-1. 基本原則
1. **Subsystemは1つの物理機構に対応**させる。
2. **Commandは可能な限りCompositionで構築**する。単純な動作は `Commands.runOnce()` / `Commands.run()` で構築する。
3. **Commandクラスを作る基準**: 内部状態を持つ必要がある / 3行以上のexecute()ロジック / デバッグログが必要
4. **RobotContainerはSubsystem生成とバインディング接続のみ**にとどめる。

### 6-2. Subsystem設計の原則
1. Subsystem内で他のSubsystemを直接参照しない。依存はRobotContainerでCommandを通じて接続する。
2. `RobotState` を中間ストアとして利用する。Subsystem間のデータ共有はRobotState経由で行う。
3. IO抽象化パターン（`VisionIO` interface + Hardware/Dummy実装）を新しいSubsystemでも推奨する。

### 6-3. Commandの構造化方針
```
commands/
  AutoCommand.java               # Auto用ファクトリメソッドクラス
  debug/                         # デバッグ用Command
    odmetry/                     # オドメトリデバッグ
    safety/                      # セーフティデバッグ
    vision/                      # ビジョンデバッグ
```
- Auto専用の複合Commandが必要な場合は `commands/auto/` を作成する。
- Teleop用Commandが必要な場合は `commands/teleop/` を作成する。

---

## 7. 定数管理の方針

1. **ドメイン別定数クラス体系を維持**する（ControlConstants, FieldConstants, VisionConstants等）。
2. **Command固有定数**: 2箇所以上で参照される定数は `commandconstants/` に置く。1箇所のみならCommand内の `private static final` で可。
3. **定数クラスは `final class` + `private` コンストラクタ**で統一する。
4. **PID定数**: 機構が少ない間はPIDConstants.javaにまとめる。機構が増えた場合は機構別に分割する。
5. **マジックナンバー禁止**: 繰り返し使われる数値は定数化する。

---

## 8. ログ・デバッグの方針

| 用途 | 手段 |
|---|---|
| 構造化テレメトリ（状態変数、ポーズ推定等） | `Logger.recordOutput()` (AdvantageKit) |
| イベントログ（開始/終了/エラー） | `System.out.printf` |
| 重大な構成エラー | `DriverStation.reportWarning()` / `DriverStation.reportError()` |

### ログルール
- ログプレフィクス: `[クラス名]` 形式（`[SetToTag]`, `[VisionFilter]`, `[AUTO]`）
- 本番モードフラグ: `ENABLE_VERBOSE_*` パターンを全Subsystemで統一する
- ログレート制限: 高頻度ログ（periodic内）は必ずthrottleする。`LogConstants` に統一定義済みのものを使う

---

## 既知のtypo・将来修正予定

| 現在 | 修正後 | 備考 |
|---|---|---|
| `controllboard/` | `controlboard/` | 別TRIALで一括修正予定 |
| `commands/debug/odmetry/` | `commands/debug/odometry/` | 別TRIALで一括修正予定 |
