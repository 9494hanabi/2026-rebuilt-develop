# PathPlanner 実装状況（2026-02-26）

最終更新: 2026-02-26 13:14:24 JST

## 要約

- 現在状態: **基本運用は実装済み / 高度拡張は未完了**
- 方針: `frc-2025-public` を参考にしつつ、**外部 PathPlannerLib (`com.pathplanner.lib`) を優先利用**
- ビルド状態: `compileJava`, `build` ともに成功

## 実装済み

### 1. 外部ライブラリ連携

- PathPlanner vendordep 導入済み（`PathplannerLib 2026.1.2`）
  - `vendordeps/PathplannerLib-2026.1.2.json`

### 2. AutoBuilder 初期化とフォールバック

- `SwerveSubsystem.setupPathPlanner()` で `AutoBuilder.configure(...)` を実行
  - `src/main/java/frc/robot/subsystems/SwerveSubsystem.java`
- `pathplanner/settings.json` が無い場合は警告して無効化
- 初期化失敗時は `PathPlannerLogging.clearLoggingCallbacks()` を実行して安全側へフォールバック

### 3. 自律選択フロー（Chooser）

- `AutoBuilder.buildAutoChooser("New Auto")` を使用
  - `src/main/java/frc/robot/RobotContainer.java`
- `Robot.autonomousInit()` は chooser 選択コマンドをスケジュール
  - `src/main/java/frc/robot/Robot.java`

### 4. PathPlanner ログ連携

- `PathPlannerLogging` callback を実装済み
  - `PathPlanner/targetPose`
  - `PathPlanner/currentPose`
  - `PathPlanner/activePath`
- `RobotState` の trajectory pose 更新と連動
  - `src/main/java/frc/robot/subsystems/SwerveSubsystem.java`

### 5. PathPlanner 呼び出し API（Subsystem）

- 追加済みメソッド:
  - `followPath(String pathName)`
  - `pathfindToPose(Pose2d targetPose, PathConstraints constraints, double goalEndVelocityMps)`
  - `pathfindThenFollowPath(String goalPathName, PathConstraints constraints)`
- すべて `AutoBuilder` 経由で外部PathPlannerLibを使用
  - `src/main/java/frc/robot/subsystems/SwerveSubsystem.java`

### 6. AutoCommand / NamedCommands 拡張

- `AutoCommand` に以下を追加:
  - `followPath(...)`
  - `pathfindToPose(...)`
  - `pathfindThenFollowPath(...)`
  - `src/main/java/frc/robot/commands/AutoCommand.java`
- `NamedCommands` にデバッグ用 pathfinding コマンドを追加:
  - `pathfindToFieldCenter`
  - `pathfindThenFollowNewPath`
  - `src/main/java/frc/robot/bindings/AutoBindings.java`

### 7. デフォルト制約定義

- `PathPlannerConstants` を追加:
  - `kDefaultPathfindingConstraints`
  - `src/main/java/frc/robot/lib/constants/PathPlannerConstants.java`

## 現在の制約・未完了項目

### A. ローカル複製 pathplanner 実装は未採用

- `src/main/java/frc/robot/lib/pathplanner/**` は未完成ファイルを含むため、現状はコンパイル対象外
  - `build.gradle` の `sourceSets.main.java.exclude 'frc/robot/lib/pathplanner/**'`

### B. `frc-2025-public` 相当の高度拡張は未実装

- 動的障害物制御（コンテキスト別 obstacle セット切替）
- 独自 pathfinder 運用（LocalADStar 相当）
- 実運用チューニング値（PathConstraints / PID）の現場最適化

### C. 実機検証は未完了

- コード上のビルドは成功しているが、実機（roboRIO）接続下での最終走行検証は未完了

## 検証ログ（この時点）

- `./gradlew compileJava --offline -Dorg.gradle.java.home="/Users/suzukiakiramuki/wpilib/2026/jdk"`
  - 成功（UP-TO-DATE）
- `./gradlew build --offline -Dorg.gradle.java.home="/Users/suzukiakiramuki/wpilib/2026/jdk"`
  - 成功（UP-TO-DATE）

## 次ステップ（推奨）

1. 実機で `pathfindToFieldCenter` と `pathfindThenFollowNewPath` を実行し、追従品質を確認
2. 実測に基づき `PathPlannerConstants` の制約値を調整
3. 必要なら `frc-2025-public` 同等の動的障害物制御を追加
