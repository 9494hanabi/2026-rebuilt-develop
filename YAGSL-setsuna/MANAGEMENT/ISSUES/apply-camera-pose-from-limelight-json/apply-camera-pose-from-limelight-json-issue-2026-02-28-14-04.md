## WHERE
- src/main/java/frc/robot/subsystems/vision/VisionIOHardwareLimelight.java
- src/main/java/frc/robot/lib/limelight/LimelightConfig.java
- src/main/deploy/limelight/limelights.json

## ASSIGN
- lead

## SUMMARY
- LimelightのcameraPose設定もJSONから起動時に反映する
- limelights.json にある cameraPose と robotToCamera の値を起動時にLimelightへ適用し、Web UI手動設定に依存しないようにする

## CRITERIA
- 起動時に各有効Limelightへ cameraPose がJSON設定から送信される
- JSONの値が欠損しても安全な既定値で動作し、初期化が失敗しない
- 関連コードがビルド可能で既存のVision初期化を壊さない

## VERIFICATION
- コード差分確認と ./gradlew build --offline で検証する

## RELATION
- なし

## WANTED
FALSE

## FLAG
FLAG = INCOMPLETE
