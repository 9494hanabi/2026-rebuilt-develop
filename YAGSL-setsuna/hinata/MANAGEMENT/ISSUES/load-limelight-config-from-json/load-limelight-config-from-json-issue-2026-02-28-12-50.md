## WHERE
- src/main/java/frc/robot/lib/limelight/LimelightConfig.java
- src/main/java/frc/robot/subsystems/vision/
- src/main/deploy/limelight/

## ASSIGN
- lead

## SUMMARY
- Limelight設定を起動時にJSONから読み込むように変更する
- 現在コード内で固定されているLimelightの有効/無効やテーブル名などの設定を、起動時にJSONファイルから読み込み、設定変更をコード編集なしで反映できるようにする

## CRITERIA
- 起動時にLimelight設定がJSONから読み込まれる
- 設定ファイルが欠損または不正な場合は安全な既定値または明確なフォールバックで動作する
- 関連コードがビルド可能で、既存のVision初期化を壊さない

## VERIFICATION
- コード差分確認と ./gradlew build --offline で検証する

## RELATION
- なし

## WANTED
FALSE

## FLAG
FLAG = INCOMPLETE
