# FILE_MAP

## Purpose
- ファイルの役割と、関連するファイルを記録する
- 「このファイルは何をするのか」に答えやすくする

## Record Format
- ファイル名
- 役割
- 主な関連ファイル
- 入力
- 出力
- 注意点

## Notes
- まずは主要ファイルから埋める
- 説明は単体ではなく、関連ファイルとの関係で書く
- WPILib command-based の標準構成に読み替えて理解する
- このプロジェクトでは `bindings/` が `RobotContainer` の button binding 役割を分担している点が特徴

## Core Entry Files
### `src/main/java/frc/robot/Robot.java`
- 役割:
  - WPILib のモード遷移入口
  - `RobotContainer` の生成
  - autonomous 開始、teleop 開始、scheduler 実行
- 主な関連ファイル:
  - `src/main/java/frc/robot/RobotContainer.java`
  - `src/main/java/frc/robot/lib/util/LogCleanupService.java`
- 入力:
  - WPILib のライフサイクルイベント
- 出力:
  - autonomous command の schedule / cancel
  - scheduler 実行
- 注意点:
  - 実際の機能ロジックはほぼ `RobotContainer` と各 subsystem 側にある

### `src/main/java/frc/robot/RobotContainer.java`
- 役割:
  - システム全体の配線ハブ
  - subsystem 生成
  - driver 操作入力を走行 command に変換
  - bindings と auto chooser の設定
- 主な関連ファイル:
  - `src/main/java/frc/robot/RobotState.java`
  - `src/main/java/frc/robot/subsystems/SwerveSubsystem.java`
  - `src/main/java/frc/robot/subsystems/vision/VisionSubsystem.java`
  - `src/main/java/frc/robot/subsystems/vision/PieceVisionSubsystem.java`
  - `src/main/java/frc/robot/subsystems/shooter/ShooterSubsystem.java`
  - `src/main/java/frc/robot/subsystems/shooter/ShootAngleSubsystems.java`
  - `src/main/java/frc/robot/bindings/DriveBindings.java`
  - `src/main/java/frc/robot/bindings/AutoBindings.java`
  - `src/main/java/frc/robot/bindings/DebugBindings.java`
- 入力:
  - `DriverController`
  - Limelight 有効状態
  - PathPlanner Auto 名
- 出力:
  - default drive command
  - auto chooser
  - vision consumer の接続
- 注意点:
  - ここが依存関係の中心なので、全体挙動を知るときに最初に読む価値が高い

### `src/main/java/frc/robot/RobotState.java`
- 役割:
  - ロボットの共有状態を持つ
  - pose、speed、IMU 系履歴、trajectory pose、vision 推定を管理する
- 主な関連ファイル:
  - `src/main/java/frc/robot/subsystems/SwerveSubsystem.java`
  - `src/main/java/frc/robot/subsystems/vision/VisionSubsystem.java`
  - `src/main/java/frc/robot/lib/util/ConcurrentTimeInterpolatableBuffer.java`
  - `src/main/java/frc/robot/lib/constants/FieldConstants.java`
- 入力:
  - odometry 測定
  - drive motion 測定
  - vision pose estimate
- 出力:
  - 各 subsystem や command から参照される共有状態
  - vision estimate consumer 経由の pose 受け渡し
- 注意点:
  - vision と odometry をつなぐ中心なので、挙動理解の要点

## Subsystems
### `src/main/java/frc/robot/subsystems/SwerveSubsystem.java`
- 役割:
  - YAGSL ベースのスワーブ走行 subsystem
  - odometry 更新
  - PathPlanner AutoBuilder 設定
  - path follow / pathfind command の入口提供
- 主な関連ファイル:
  - `src/main/java/frc/robot/RobotState.java`
  - `src/main/java/frc/robot/lib/constants/TeleopConstants.java`
  - `src/main/java/frc/robot/lib/constants/FieldConstants.java`
  - `src/main/java/frc/robot/lib/constants/PathPlannerConstants.java`
  - `src/main/deploy/swerve/`
  - `src/main/deploy/pathplanner/`
- 入力:
  - driver 操作由来の chassis command
  - vision measurement
  - deploy 配下の YAGSL / PathPlanner 設定
- 出力:
  - chassis motion
  - field pose
  - Auto chooser と path follow 系 command
- 注意点:
  - YAGSL と PathPlanner の接続点
  - deploy 設定の影響を強く受ける

### `src/main/java/frc/robot/subsystems/vision/VisionSubsystem.java`
- 役割:
  - VisionIO からカメラ入力を取得する
  - 推定品質判定と姿勢採用判定を行う
  - 複数カメラ時は pose estimate を融合する
- 主な関連ファイル:
  - `src/main/java/frc/robot/subsystems/vision/VisionIO.java`
  - `src/main/java/frc/robot/subsystems/vision/VisionIOHardwareLimelight.java`
  - `src/main/java/frc/robot/subsystems/vision/VisionIODummy.java`
  - `src/main/java/frc/robot/subsystems/vision/VisionFieldPoseEstimate.java`
  - `src/main/java/frc/robot/subsystems/vision/MegatagPoseEstimate.java`
  - `src/main/java/frc/robot/RobotState.java`
  - `src/main/java/frc/robot/lib/constants/VisionConstants.java`
- 入力:
  - Limelight 系カメラ観測
  - RobotState 上の odometry pose
- 出力:
  - 採用された vision pose
  - log / dashboard 出力
- 注意点:
  - vision を使うかどうか、どの推定を採用するかの判定ロジックが集中している

### `src/main/java/frc/robot/subsystems/vision/PieceVisionSubsystem.java`
- 役割:
  - intake 側 Limelight detector の観測をまとめる
  - Fuel detection を filter / cluster 化して最良塊を返す
  - piece detector pipeline 切替と last seen 管理を行う
- 主な関連ファイル:
  - `src/main/java/frc/robot/commands/auto/autovision/AutoVisionCommand.java`
  - `src/main/java/frc/robot/commands/auto/autovision/AutoIntakeAssistCommand.java`
  - `src/main/java/frc/robot/lib/constants/AutoVisionConstants.java`
  - `src/main/java/frc/robot/lib/limelight/LimelightHelpers.java`
  - `src/main/java/frc/robot/bindings/AutoBindings.java`
- 入力:
  - `limelight-intake` の RawDetection
  - detector pipeline index
- 出力:
  - `bestCluster`
  - `lastSeenCluster`
  - piece detector telemetry
- 注意点:
  - pose vision の `VisionSubsystem` とは役割が別
  - Auto の Fuel 回収精度はこの subsystem の filter / cluster 条件に強く依存する

### `src/main/java/frc/robot/subsystems/shooter/ShooterSubsystem.java`
- 役割:
  - shooter モーター 2 台の速度閉ループ制御
  - target RPS 管理
  - Ready 判定
- 主な関連ファイル:
  - `src/main/java/frc/robot/commands/auto/AutoCommand.java`
  - `src/main/java/frc/robot/bindings/AutoBindings.java`
  - `src/main/java/frc/robot/bindings/DebugBindings.java`
- 入力:
  - target RPS
- 出力:
  - motor speed control
  - ready state
  - SmartDashboard telemetry
- 注意点:
  - 実機調整値の影響が大きい

### `src/main/java/frc/robot/subsystems/shooter/ShootAngleSubsystems.java`
- 役割:
  - 射角モーターの位置制御
  - soft limit と atTarget 判定
- 主な関連ファイル:
  - `src/main/java/frc/robot/commands/auto/AutoCommand.java`
  - `src/main/java/frc/robot/bindings/AutoBindings.java`
  - `src/main/java/frc/robot/bindings/DebugBindings.java`
  - `src/main/java/frc/robot/lib/constants/ShootAngleConstants.java`
- 入力:
  - target motor rotation
  - manual percent
- 出力:
  - motor position control
  - SmartDashboard telemetry
- 注意点:
  - calibration 値と soft limit が強く効く

## Bindings
### `src/main/java/frc/robot/bindings/DriveBindings.java`
- 役割:
  - driver controller の drive 系ボタン割当
  - zeroing、horizon 補正、corner pathfinding、安全停止の登録
- 主な関連ファイル:
  - `src/main/java/frc/robot/RobotContainer.java`
  - `src/main/java/frc/robot/commands/debug/odmetry/`
  - `src/main/java/frc/robot/commands/debug/safety/ClearOdometryAndLockStopCommand.java`
- 入力:
  - controller button / trigger
- 出力:
  - drive 系 debug / safety command の起動
- 注意点:
  - driver の通常操作と calibration / safety 操作の境目を理解する時に重要

### `src/main/java/frc/robot/bindings/AutoBindings.java`
- 役割:
  - PathPlanner `NamedCommands` の登録
  - shooter、shoot angle、pathfind、vision mode 切替、piece detector mode、piece acquire などの auto event を辞書化する
- 主な関連ファイル:
  - `src/main/java/frc/robot/commands/auto/AutoCommand.java`
  - `src/main/java/frc/robot/commands/auto/autovision/AutoVisionCommand.java`
  - `src/main/java/frc/robot/subsystems/vision/PieceVisionSubsystem.java`
  - `src/main/java/frc/robot/lib/constants/AutoVisionConstants.java`
  - `src/main/java/frc/robot/lib/constants/PathPlannerConstants.java`
  - `src/main/deploy/pathplanner/`
- 入力:
  - PathPlanner から来る marker / event 名
- 出力:
  - event 名に対応する command
- 注意点:
  - Auto の動作は path file だけでは完結せず、このファイルの辞書登録とセットで見る必要がある

### `src/main/java/frc/robot/bindings/DebugBindings.java`
- 役割:
  - 試験用、デバッグ用操作の割当
  - shooter 動作確認、射角 preset 切替、現在 pose 表示
- 主な関連ファイル:
  - `src/main/java/frc/robot/subsystems/shooter/ShooterSubsystem.java`
  - `src/main/java/frc/robot/subsystems/shooter/ShootAngleSubsystems.java`
  - `src/main/java/frc/robot/subsystems/SwerveSubsystem.java`
- 入力:
  - controller button / trigger / dpad
- 出力:
  - debug action 実行
- 注意点:
  - コメント上、本番前に無効化する前提

## Command Utility Files
### `src/main/java/frc/robot/commands/auto/AutoCommand.java`
- 役割:
  - auto 用の再利用 command をまとめる utility
  - drive 停止、path 実行、pathfind、shooter 制御、shoot angle 制御、安全停止を組み立てる
- 主な関連ファイル:
  - `src/main/java/frc/robot/subsystems/SwerveSubsystem.java`
  - `src/main/java/frc/robot/subsystems/shooter/ShooterSubsystem.java`
  - `src/main/java/frc/robot/subsystems/shooter/ShootAngleSubsystems.java`
  - `src/main/java/frc/robot/bindings/AutoBindings.java`
- 入力:
  - subsystem
  - target pose / path name / target RPS / target angle
- 出力:
  - PathPlanner や auto marker から呼ばれる Command
- 注意点:
  - Auto の「実処理部」に近い

### `src/main/java/frc/robot/commands/auto/autovision/AutoVisionCommand.java`
- 役割:
  - Auto 中の vision 用 command を組み立てる utility
  - pipeline 切替、exclusive tag 制御、tag align、piece detector mode、piece acquire の入口
- 主な関連ファイル:
  - `src/main/java/frc/robot/bindings/AutoBindings.java`
  - `src/main/java/frc/robot/RobotState.java`
  - `src/main/java/frc/robot/subsystems/SwerveSubsystem.java`
  - `src/main/java/frc/robot/subsystems/vision/PieceVisionSubsystem.java`
  - `src/main/java/frc/robot/commands/auto/autovision/AutoIntakeAssistCommand.java`
  - `src/main/java/frc/robot/lib/constants/AutoVisionConstants.java`
  - `src/main/java/frc/robot/commands/auto/AutoTelemetry.java`
- 入力:
  - table name
  - pipeline 番号
  - tag id
  - timeout や取得パラメータ
- 出力:
  - Auto 用 vision command
- 注意点:
  - vision 本体の重い処理はここではなく、別 command や vision subsystem 側にある
  - 現在の Fuel 回収では `acquirePieceOrFallback` が `AutoIntakeAssistCommand` と fallback path をまとめる窓口

### `src/main/java/frc/robot/commands/auto/autovision/AutoIntakeAssistCommand.java`
- 役割:
  - Fuel 塊の中心へ向かって前進と旋回を出す
  - collect window、last seen hold、fallback 判定を持つ
- 主な関連ファイル:
  - `src/main/java/frc/robot/commands/auto/autovision/AutoVisionCommand.java`
  - `src/main/java/frc/robot/subsystems/SwerveSubsystem.java`
  - `src/main/java/frc/robot/subsystems/vision/PieceVisionSubsystem.java`
  - `src/main/java/frc/robot/commands/auto/AutoTelemetry.java`
  - `src/main/java/frc/robot/lib/constants/AutoVisionConstants.java`
- 入力:
  - `PieceVisionSubsystem` の `bestCluster` / `lastSeenCluster`
  - piece intake 用の速度、deadband、timeout、fallback 閾値
- 出力:
  - drivebase の chassis speeds
  - collect 完了 / fallback 要求
  - piece vision telemetry
- 注意点:
  - 今は timeout と collect window が主な終了条件
  - 将来 CANrange の captured 判定とつながる前提が constants に残っている

## Support Files
### `src/main/java/frc/robot/controllboard/DriverController.java`
- 役割:
  - driver 用 controller ラッパ
- 主な関連ファイル:
  - `src/main/java/frc/robot/RobotContainer.java`
  - `src/main/java/frc/robot/bindings/*.java`
- 入力:
  - joystick / gamepad 入力
- 出力:
  - button、trigger、axis accessor
- 注意点:
  - bindings を読む前提として重要

### `src/main/java/frc/robot/lib/limelight/LimelightConfig.java`
- 役割:
  - Limelight の有効状態や構成判断に使う設定入口
- 主な関連ファイル:
  - `src/main/java/frc/robot/RobotContainer.java`
  - `src/main/java/frc/robot/subsystems/vision/VisionIOHardwareLimelight.java`
- 入力:
  - Limelight 設定
- 出力:
  - vision hardware を使うかどうかの判定
- 注意点:
  - vision 実機 / dummy 切替の判断点

### `src/main/java/frc/robot/lib/constants/`
- 役割:
  - 各 subsystem と command の tuning 値や設定値を保持する
- 主な関連ファイル:
  - `RobotContainer`
  - `SwerveSubsystem`
  - `VisionSubsystem`
  - `AutoBindings`
  - `AutoCommand`
- 入力:
  - 開発者が定義する各種定数
- 出力:
  - 制御ゲイン、目標値、しきい値
- 注意点:
  - ロジック理解には本体コードとセットで読む必要がある

### `src/main/deploy/pathplanner/`
- 役割:
  - PathPlanner の path、auto、settings を置く
- 主な関連ファイル:
  - `src/main/java/frc/robot/subsystems/SwerveSubsystem.java`
  - `src/main/java/frc/robot/bindings/AutoBindings.java`
- 入力:
  - PathPlanner GUI で作成した path / auto
- 出力:
  - 自律経路と event marker 設定
- 注意点:
  - Auto の挙動を知るには Java 側だけでなくここも必要

### `src/main/deploy/swerve/`
- 役割:
  - YAGSL の drivetrain 構成を置く
- 主な関連ファイル:
  - `src/main/java/frc/robot/subsystems/SwerveSubsystem.java`
- 入力:
  - module、physical property、controller property の設定
- 出力:
  - スワーブ実機構成
- 注意点:
  - 機体挙動の前提条件なので、走行がおかしい時はここも見る必要がある
