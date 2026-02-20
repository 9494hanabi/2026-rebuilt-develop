# Vision/Drive Review (yagsl-setsuna)

## 結論 (評価項目)
- ドライブはVision観測を受け取り、自己位置推定に利用できている: **はい**
  - 経路: `VisionSubsystem.periodic` -> `RobotState.updateMegatagEstimate` -> `RobotContainer.visionEstimateConsumer` -> `SwerveSubsystem.addVisionMeasurement` -> `SwerveDrive.addVisionMeasurement`
    - 参照: `src/main/java/frc/robot/subsystems/vision/VisionSubsystem.java:145-182`, `src/main/java/frc/robot/RobotState.java:290-294`, `src/main/java/frc/robot/RobotContainer.java:39-55`, `src/main/java/frc/robot/subsystems/SwerveSubsystem.java:208-213`

## 発見したミスと対応策
1) VisionとDriveの循環依存による自己整合バイアス
- 内容: `RobotState` の `fieldToRobot` は `SwerveSubsystem.periodic` から `swerveDrive.getPose()` を追加しています。`VisionSubsystem` はこの `RobotState` の姿勢 (`getFieldToRobot`) を使って vision の妥当性判定をしています。`getPose()` が vision 融合済みの場合、vision の判定に vision を再利用する循環が発生し、外れ値の通過や過信のリスクがあります。
- 参照: `src/main/java/frc/robot/subsystems/SwerveSubsystem.java:176-179`, `src/main/java/frc/robot/subsystems/vision/VisionSubsystem.java:64-67`, `src/main/java/frc/robot/subsystems/vision/VisionSubsystem.java:297-373`
- 対応策:
  - `RobotState` に「odometry-only」姿勢バッファを追加し、Vision側のゲーティング/補正は **odometry-only** を参照する。
  - `SwerveDrive` が odometry-only の取得APIを持つならそれを使用し、持たない場合はホイール+ジャイロから独立に推定する。

2) Vision計測の信頼度(標準偏差)がDriveに渡っていない
- 内容: `VisionSubsystem` で標準偏差を計算しているが、`SwerveSubsystem.addVisionMeasurement` は `Pose2d` と `timestamp` しか渡していないため、推定の信頼度が drive へ反映されません。
- 参照: `src/main/java/frc/robot/subsystems/vision/VisionSubsystem.java:420-433`, `src/main/java/frc/robot/subsystems/SwerveSubsystem.java:208-213`
- 対応策:
  - `SwerveDrive.addVisionMeasurement` のオーバーロードがあれば `est.getVisionMeasurementsStdDevs()` を渡す。
  - ない場合は `SwerveDrivePoseEstimator` を直接管理するか、YAGSL 側のAPIに合わせてノイズを設定する。

3) Limelight B のNetworkTableがAと同じ名前を参照
- 内容: `tableB` が `kLimelightATableName` を参照しているため、Bカメラの `tv`/`stddevs` がAの値で上書きされます。結果としてBの観測が正しく評価・融合されません。
- 参照: `src/main/java/frc/robot/subsystems/vision/VisionIOHardwareLimelight.java:13-16`
- 対応策:
  - `tableB` を `VisionConstants.kLimelightBTableName` に修正する。

4) `fuseEstimates` が Optional の未チェック `get()` を使用
- 内容: `state.getFieldToRobot(...)` が空のとき `Optional.get()` で例外になる可能性がある。例外が出ると vision->drive の注入が止まります。
- 参照: `src/main/java/frc/robot/subsystems/vision/VisionSubsystem.java:64-67`
- 対応策:
  - Optional を確認して空なら単独推定のみ採用/融合をスキップする。

