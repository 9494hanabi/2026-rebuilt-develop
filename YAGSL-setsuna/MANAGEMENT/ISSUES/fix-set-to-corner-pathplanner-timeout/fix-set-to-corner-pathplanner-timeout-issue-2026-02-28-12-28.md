## WHERE
- src/main/java/frc/robot/commands/debug/odmetry/SetToCornerPathPlannerCommand.java
- src/main/java/frc/robot/bindings/DriveBindings.java
- src/main/java/frc/robot/commands/debug/safety/ClearOdometryAndLockStopCommand.java

## ASSIGN
- lead

## SUMMARY
- SetToCornerPathPlannerCommand の依存起因バグを修正する
- ClearOdometryAndLockStopCommand 後でも SetToCorner が途中停止しないようにし、必要に応じてタイムアウトや目標姿勢の扱いを見直す

## CRITERIA
- フィールド中心から角への遷移でも SetToCornerPathPlannerCommand が制約上不必要に 3 秒で打ち切られない
- 修正内容の根拠がコード上で明確で、PathPlanner コマンド終了条件が意図と一致する
- 関連コードがビルド可能で、回帰を起こす新たなコンパイルエラーがない

## VERIFICATION
- コード差分確認と ./gradlew build --offline もしくは同等のビルド確認で検証する

## RELATION
- なし

## WANTED
FALSE

## FLAG
FLAG = INCOMPLETE
