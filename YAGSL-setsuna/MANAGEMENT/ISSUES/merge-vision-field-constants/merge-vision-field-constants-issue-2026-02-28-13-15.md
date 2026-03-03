## WHERE
- src/main/java/frc/robot/lib/constants/FieldConstants.java
- src/main/java/frc/robot/lib/constants/VisionConstants.java
- src/main/java/frc/robot/lib/constants/

## ASSIGN
- lead

## SUMMARY
- FieldConstants と VisionConstants を統合して VisionFieldConstants に再編する
- フィールド切替フラグとビジョン定数を1ファイルへ統合し、先頭の useTestField / useOfficialField で一括切替できるようにする

## CRITERIA
- VisionFieldConstants が新設され、FieldConstants と VisionConstants の役割を包含する
- useTestField / useOfficialField が先頭にあり、フィールド選択が1か所で切り替えられる
- 既存コードが新定数クラスを参照してビルド可能である

## VERIFICATION
- コード差分確認と ./gradlew build --offline で検証する

## RELATION
- なし

## WANTED
FALSE

## FLAG
FLAG = INCOMPLETE
