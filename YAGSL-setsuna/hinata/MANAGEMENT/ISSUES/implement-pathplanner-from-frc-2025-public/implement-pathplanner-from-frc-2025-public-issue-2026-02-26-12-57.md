## WHERE
- src/main/java/frc/robot/subsystems/SwerveSubsystem.java
- src/main/java/frc/robot/Robot.java
- src/main/java/frc/robot/RobotContainer.java
- src/main/java/frc/robot/bindings/AutoBindings.java
- src/main/deploy/pathplanner/

## ASSIGN
- lead

## SUMMARY
- frc-2025-publicを参考に、PathPlanner機能を現行ロボットコードへ実装する
- 外部PathPlannerLibを優先利用し、AutoBuilder構成・Auto選択・ログ出力・基本動作確認までを行う

## CRITERIA
- AutoBuilderがPathPlannerLibの公式APIで初期化され、失敗時に安全フォールバックする
- SmartDashboard上でPathPlannerの.auto一覧を選択可能になり、Do Nothingを含む
- PathPlannerログ(現在姿勢/目標姿勢/アクティブパス)が出力される
- ./gradlew build --offline が成功する

## VERIFICATION
- コード差分確認と ./gradlew build --offline 実行ログで検証する

## RELATION
- なし

## WANTED
FALSE

## FLAG
FLAG = INCOMPLETE
