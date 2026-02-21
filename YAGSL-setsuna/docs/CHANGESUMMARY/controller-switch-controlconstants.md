## Summary
- ドライバーコントローラーを `ControlConstants` で Xbox / Logitech 切替できるようにした。
- コントローラー依存を `DriverController` に集約し、`RobotContainer` と各Bindingsから直接 `CommandXboxController` へ依存しない構成に変更した。
- 現在設定は `LOGITECH`（`ControlConstants.kDriverControllerType`）をデフォルトにした。

## Changed Files
- `src/main/java/frc/robot/lib/constants/ControlConstants.java` - コントローラー種別、共通しきい値、Xbox/Logitechの軸・ボタンマッピング定義を追加。
- `src/main/java/frc/robot/controllboard/DriverController.java` - 軸取得とボタントリガー生成をラップし、`ControlConstants` に応じてマッピングを切替える実装を追加。
- `src/main/java/frc/robot/RobotContainer.java` - `DriverController` と `ControlConstants` を利用するように変更し、ドライブ入力デッドバンドの参照先を切替え。
- `src/main/java/frc/robot/bindings/DriveBindings.java` - 受け取るコントローラー型を `DriverController` に変更。
- `src/main/java/frc/robot/bindings/DebugBindings.java` - 受け取るコントローラー型を `DriverController` に変更。

## Review Status
- Reviewed by: reviewer
- Date: 2026-02-21
- Result: APPROVED (Note: 既存の `SetToTagCommandRefactored.java` コンパイルエラーは別件のまま)
