# develop rebase log (2026-03-01)

## 背景

- 2026-03-01 に `git push origin develop` が `non-fast-forward` で拒否された。
- その時点の状態は `develop...origin/develop [ahead 1, behind 3]` だった。
- ローカル側の先頭コミットは `06c8e04`。
- リモート側の先頭コミットは `0ce38e6`。

## 実施した操作

1. `git pull --rebase origin develop`
2. 競合解消
3. `./gradlew compileJava`
4. `git rebase --continue`
5. `git push origin develop`

最終的に rebase 後のコミットは `0884177` になり、`origin/develop` への push は成功した。

## 何が残ったか

### `src/main/java/frc/robot/Robot.java`

- `RobotContainer#getAutonomousCommand()` を使う構成を残した。
- `autonomousInit()` で `null` のときに `Commands.none()` を入れて `schedule()` する流れを残した。

理由:
- Auto chooser を `RobotContainer` に集約した方が二重管理を避けられるため。

### `src/main/java/frc/robot/RobotContainer.java`

- `autoChooser = drivebase.buildAutoChooser("New Auto")` を残した。
- `SmartDashboard.putData("Auto setting", autoChooser)` を残した。
- `ShootAngleSubsystems` の生成を残した。
- `AutoBindings(drivebase, robotState, shooter, shootAngle)` を残した。
- `DebugBindings(..., shooter, shootAngle)` を残した。

理由:
- リモート側の Auto chooser 管理を維持しつつ、ローカル側の `ShootAngle` 系追加を取り込めるため。

### `src/main/java/frc/robot/bindings/AutoBindings.java`

- `frc.robot.commands.auto.AutoCommand` への移動後パッケージ構成を残した。
- `AutoVisionCommand` / `AutoVisionConstants` を使う NamedCommands を残した。
- `ShootAngleSubsystems` / `ShootAngleConstants` を使う NamedCommands を残した。
- `pathfindToFieldCenter` と `pathfindThenFollowNewPath` も残した。

理由:
- リモート側の AutoVision 追加と、ローカル側の ShootAngle 追加は両方とも機能として成立していたため、片方を捨てず統合した。

### `src/main/deploy/pathplanner/autos/test.auto`

- `test.auto` は残した。
- 内容は `start` -> `wait 3.0s` -> `middle` -> `visionPieceModeOn` -> `visionAcquirePiece` -> `return`。

理由:
- rebase 中にローカルコミット側の Auto 資産として残す価値があり、AutoVision 系イベントの確認にも使えるため。

### `src/main/deploy/pathplanner/autos/tuning.auto`

- 旧 `New New Auto.auto` から改名された `tuning.auto` を残した。

理由:
- リモート側で既にファイル名整理が進んでいたため、旧名を復活させず現行名を採用した。

## 何が消えたか

### `src/main/java/frc/robot/Robot.java`

- `SendableChooser<String> m_chooser` を消した。
- `kDoNothingAuto` / `kMyAuto` / `m_autoSelected` を消した。
- `PathPlannerAuto` を直接生成して `switch` する実装を消した。
- `System.out.println("RobotContainer" + m_robotContainer);` を消した。

理由:
- `RobotContainer` 側の chooser と責務が重複していたため。

### `src/main/java/frc/robot/RobotContainer.java`

- `SendableChooser<Command> m_chooser` のローカル案を消した。
- `startupOdometryLockCommand` を消した。
- `ClearOdometryAndLockStopCommand` の import を消した。

理由:
- 競合解消後の最終コードでは未使用で、Auto chooser の管理も `autoChooser` に統一したため。

### `src/main/deploy/pathplanner/autos/New New Auto.auto`

- 旧名の `New New Auto.auto` は復活させなかった。

理由:
- 最終状態では `tuning.auto` に改名された構成を採用したため。

## 競合解消の要点

- `Robot` は簡素化して、Auto 選択責務を `RobotContainer` に寄せた。
- `RobotContainer` はリモート側の chooser 管理をベースに、ローカル側の `ShootAngleSubsystems` 注入だけ取り込んだ。
- `AutoBindings` は「リモート側の AutoVision」と「ローカル側の ShootAngle」を両方残す形で手動統合した。
- `test.auto` は追加し、`New New Auto.auto` は復活させず `tuning.auto` を採用した。

## 検証結果

- `./gradlew compileJava` は成功した。
- 最終的な push 結果は `0ce38e6..0884177  develop -> develop` だった。
