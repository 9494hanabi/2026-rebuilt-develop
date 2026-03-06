# PROJECT_OVERVIEW

## Purpose
- YAGSL-SETSUNA がどのような動きをするプロジェクトかを説明するためのファイル
- 高レベルの構成、主要機能、データの流れを蓄積する

## Confirmed Summary
- YAGSL-SETSUNA は FRC ロボット向けの Command-Based 制御ソフトウェア
- 現在のコードでは、スワーブ走行、ビジョン姿勢補正、自律経路追従、Fuel 塊検出、シューター制御、射角制御、デバッグ用操作を持つ
- `RobotContainer` が全体の配線中心になっており、`RobotState` を共有状態として各 subsystem をつないでいる

## WPILib Command-Based Mapping
- WPILib 標準の `Robot` に相当するのが `Robot.java`
- WPILib 標準の `RobotContainer` に相当するのが `RobotContainer.java`
- WPILib 標準の `Constants` は、このプロジェクトでは `lib/constants/` に分割されている
- WPILib 標準の `subsystems/` と `commands/` は、このプロジェクトでもそのままの役割
- WPILib テンプレートで `RobotContainer.configureBindings()` に集まりやすい内容を、このプロジェクトでは `bindings/` に役割分割している
- そのため、command-based の挙動を追うときは `Robot` -> `RobotContainer` -> `bindings/` -> `commands/` -> `subsystems/` の順で見ると理解しやすい

## Main Runtime Flow
1. `Robot` が起動し、`RobotContainer` を生成する
2. `RobotContainer` が `RobotState`、`SwerveSubsystem`、`VisionSubsystem`、`PieceVisionSubsystem`、`ShooterSubsystem`、`ShootAngleSubsystems` を生成する
3. ドライバー入力は `SwerveInputStream` に変換され、通常時のデフォルトコマンドとしてスワーブ走行に使われる
4. `DriveBindings`、`AutoBindings`、`DebugBindings` がボタン操作と NamedCommands を登録する
5. 自律開始時は `Robot` が `autoChooser` から選ばれたコマンドを取得してスケジューリングする
6. `robotPeriodic()` では `CommandScheduler` が全コマンドを進め、ログ整理も走る

## Core Modules
### `Robot`
- WPILib のモード遷移を管理する入口
- `autonomousInit()` で自律コマンドを開始し、`teleopInit()` で自律を止める

### `RobotContainer`
- システム全体の配線担当
- 各 subsystem を生成する
- ドライバー入力を field-oriented の走行コマンドへ変換する
- bindings を初期化し、デフォルト走行コマンドと Auto chooser を設定する

### `RobotState`
- 共有状態の中心
- オドメトリ姿勢、オドメトリ単独姿勢、速度、IMU系履歴、軌道ターゲット、ビジョン推定を保持する
- Vision から来た推定値を drivebase へ流すための callback も持つ

### `SwerveSubsystem`
- YAGSL ベースの走行 subsystem
- スワーブ生成、オドメトリ更新、Field2d 表示、PathPlanner 設定を担当する
- `AutoBuilder` を設定し、`followPath`、`pathfindToPose`、`pathfindThenFollowPath` などの自律走行入口を持つ

### `VisionSubsystem`
- VisionIO 経由でカメラ入力を読む
- MegaTag 推定を評価し、条件を満たす推定だけを採用する
- 複数カメラの推定がある場合は分散ベースで融合する
- 採用した姿勢を `RobotState` に反映し、drivebase の vision measurement に流す

### `PieceVisionSubsystem`
- intake 側 Limelight detector の観測をまとめる
- RawDetection を filter して、Fuel の塊を cluster として整理する
- `bestCluster` と `lastSeenCluster` を持ち、Auto の Fuel 回収に使う
- detector pipeline と idle pipeline の切り替えもここで扱う

### `ShooterSubsystem`
- 2 台の TalonFX を速度閉ループで回す
- 目標 RPS、実測 RPS、Ready 判定を管理する
- Auto 用にも Teleop 用にも使える共通 shooter 制御を持つ

### `ShootAngleSubsystems`
- TalonFX 1 台で射角を位置制御する
- ソフトリミットと目標角判定を持つ
- preset や auto event から射角を変更する

## Driver Operation Flow
### Normal Drive
- 左スティックで並進、右スティックで回転を入力する
- 入力は `SwerveInputStream` によりデッドバンド、スケーリング、alliance relative 補正を受ける
- 通常のデフォルトコマンドは field-oriented の angular velocity drive になっている

### Drive Calibration And Safety
- `DriveBindings` には、姿勢補正や水平合わせ、ゼロ合わせ、コーナーへの pathfinding、強制停止が入っている
- 強制停止は左トリガーと start ボタンに割り当てられている

### Debug Operation
- `DebugBindings` には shooter の開始停止、状態確認、射角 preset 切り替え、現在 pose 表示が入っている
- コメントから、本番前には debug binding を止める運用を想定している

## Autonomous Flow
- `AutoBindings` が PathPlanner の `NamedCommands` をまとめて登録している
- 現在のコードでは、shooter の spin-up、ready wait、shoot、stop、射角セット、pathfind、vision mode 切り替え、tag align、piece detector mode、piece acquire、fallback path などを登録している
- `AutoCommand` は、drive 停止、自律 path 実行、pathfinding、shooter 制御、射角制御、安全停止の共通コマンド群
- `AutoVisionCommand` は、AprilTag 系 vision command と piece acquire command の入口を持つ
- `AutoIntakeAssistCommand` は、Fuel 塊の中心へ向かう drive 出力と collect window、fallback 判定を持つ
- `RobotContainer` の `autoChooser` は `"New Auto"` をデフォルト名として構築されている

## Vision And State Flow
### Input
- Limelight 系入力は `VisionIOHardwareLimelight` または `VisionIODummy` から入る

### Processing
- `VisionSubsystem` が観測可否、推定品質、odometry との距離などを見て採用判定する
- 複数カメラがある場合は、標準偏差を使って姿勢を融合する

### Output
- 採用した姿勢は `RobotState` に記録される
- `RobotState` に設定された consumer を通じて `SwerveSubsystem.addVisionMeasurement` へ流れる

## Piece Vision Flow
### Input
- `PieceVisionSubsystem` が `limelight-intake` の detector RawDetection を読む

### Processing
- class id、面積、画角端を使って不要な detection を落とす
- 横方向に近い detection を 1 つの cluster にまとめる
- cluster の面積、個数、横幅から score を作って、最も価値の高い Fuel 塊を選ぶ
- `lastSeenCluster` を保持して、見失い直後の補助にも使う

### Output
- `AutoIntakeAssistCommand` が `bestCluster` を見て旋回と前進を決める
- Fuel が見つからない時間が長いと fallback path に切り替える
- 近づいたと判断したら collect window に入り、少し押し込んで回収成功率を上げる

## Telemetry And Logging
- `SmartDashboard` に Auto chooser、Field2d、Shooter、ShootAngle などの情報が出る
- `Logger.recordOutput` を使って PathPlanner や Vision のデータも出している
- `LogCleanupService` が `robotPeriodic()` で動く

## What This Robot Currently Looks Like
- 走る: スワーブで field-oriented 走行
- 見る: Limelight 系 vision で姿勢補正
- Fuel を探す: intake 側 Limelight detector で Fuel 塊を選ぶ
- 自律する: PathPlanner で経路追従とイベント実行
- 打つ: shooter を速度制御で回す
- 角度を変える: shoot angle motor を位置制御する
- 調整する: debug bindings で pose や shooter 状態を確認する

## Current Unknowns Or Later Verification
- 実機で shooter と shoot angle がどこまで完成しているかは別途確認が必要
- CANrange を使った captured 判定は、まだ constants と構想段階で実機統合前
- `AutoIntakeAssistCommand` の collect window、fallback 条件、cluster tuning は実機確認が必要
- 実際の scoring フローや shooter 連携は command 単位でもう少し分解して確認したい
- deploy 設定や path ファイルの中身まで含めた挙動説明は今後追加する

## Read Sources For This Version
- `src/main/java/frc/robot/Robot.java`
- `src/main/java/frc/robot/RobotContainer.java`
- `src/main/java/frc/robot/RobotState.java`
- `src/main/java/frc/robot/subsystems/SwerveSubsystem.java`
- `src/main/java/frc/robot/subsystems/vision/VisionSubsystem.java`
- `src/main/java/frc/robot/subsystems/vision/PieceVisionSubsystem.java`
- `src/main/java/frc/robot/subsystems/shooter/ShooterSubsystem.java`
- `src/main/java/frc/robot/subsystems/shooter/ShootAngleSubsystems.java`
- `src/main/java/frc/robot/bindings/DriveBindings.java`
- `src/main/java/frc/robot/bindings/AutoBindings.java`
- `src/main/java/frc/robot/bindings/DebugBindings.java`
- `src/main/java/frc/robot/commands/auto/AutoCommand.java`
- `src/main/java/frc/robot/commands/auto/autovision/AutoVisionCommand.java`
- `src/main/java/frc/robot/commands/auto/autovision/AutoIntakeAssistCommand.java`

## How To Grow This File
- subsystem 単位で役割を書く
- command が何を起動するかを書く
- 入力、処理、出力の流れを書く
- 実装を読んで分かった依存関係を追記する
