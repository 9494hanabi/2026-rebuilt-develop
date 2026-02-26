# CLAUDE.md

作業手順やこれまでの変更履歴についてはMANAGEMENT/以下を参照すること。
コーディング規約・設計ガイドラインについてはMANAGEMENT/CONVENTIONS.mdを参照すること。

## 0. 技術スタック

| 項目 | 値 |
|---|---|
| 言語 | Java |
| フレームワーク | WPILib (FRC) + YAGSL (Swerve Drive Library) + PathPlanner |
| ビルドツール | Gradle (GradleRIO) |
| テスト | JUnit 5 |
| ログ | AdvantageKit (org.littletonrobotics.junction.Logger) |

## 1. プロジェクトのファイル構造

```
/yagsl-setsuna
    /MANAGEMENT
        /COMPLETES          # 完了済みISSUE/TRIALのアーカイブ
        /ISSUES             # 進行中のISSUE/TRIAL
            ISSUE.md        # ISSUEテンプレート定義
            TRIAL.md        # TRIALテンプレート定義
            WANTED.md       # 優先度管理
        /ROLE               # ロール定義
            WORKFLOW.md
            LEADER.md
            IMPLEMENTER.md
            ANALYST.md
            REVIEWER_VERIFICATER.md
        /SKILLS             # MANAGEMENT用スキル
            /public
                /issue-creator
                /trial-creator
                /spawn-team
        /tests              # ワークフロー仕様テスト
            test_management_workflow_spec.py
        CONVENTIONS.md      # コーディング規約・設計ガイドライン
        README.md
    AGENTS.md               # Codex-CLI設定
    CLAUDE.md               # Claude Code設定（本ファイル）
    /src
        /main
            /deploy                     # ロボット設定ファイル
                /limelight              # Limelight設定
                /maps                   # フィールドマップ
                /pathplanner            # PathPlanner設定
                    /autos              # Auto経路
                    /paths              # パス定義
                /swerve                 # YAGSL Swerve設定
                    /modules            # 各モジュール設定
            /java/frc/robot
                Main.java
                Robot.java
                RobotContainer.java
                RobotState.java
                /bindings               # コントローラーバインディング
                    AutoBindings.java
                    DebugBindings.java
                    DriveBindings.java
                /commands               # Command群
                    AutoCommand.java
                    /debug
                        /odmetry        # オドメトリデバッグCommand
                        /safety         # セーフティデバッグCommand
                        /vision         # ビジョンデバッグCommand
                /controllboard          # コントローラー抽象化
                    DriverController.java
                    ModalControls.java
                /lib
                    /constants          # 定数クラス群
                        /commandconstants
                        ControlConstants.java
                        FieldConstants.java
                        LogConstants.java
                        OdomConstants.java
                        PIDConstants.java
                        SemiAutoConstants.java
                        TeleopConstants.java
                        VisionConstants.java
                    /limelight          # Limelight関連ユーティリティ
                        LimelightConfig.java
                        LimelightHelpers.java
                        VisionTargetSelector.java
                    /pathplanner        # PathPlanner拡張
                        /events
                        /path
                        /pathfinding
                        /trajectory
                        /util
                    /time               # 時刻ユーティリティ
                        RobotTime.java
                    /util               # 汎用ユーティリティ
                        ConcurrentTimeInterpolatableBuffer.java
                        FmapFieldLayoutLoader.java
                        LogCleanupService.java
                        MathHelpers.java
                        OdomHeadingController.java
                        OdomTranslationController.java
                /subsystems             # Subsystem群
                    ShooterSubsystem.java
                    SwerveSubsystem.java
                    /vision             # Vision Subsystem + IO + データクラス
                        FiducialObservation.java
                        MegatagPoseEstimate.java
                        VisionFieldPoseEstimate.java
                        VisionIO.java
                        VisionIODummy.java
                        VisionIOHardwareLimelight.java
                        VisionSubsystem.java
        /test                           # テスト（JUnit 5）
```

## 2. roleについて

role = IMPLEMENTER  ならば MANAGEMENT/ROLE/IMPLEMENTER.mdを読みなさい。
role = LEADER       ならば MANAGEMENT/ROLE/LEADER.mdを読みなさい。
role = ANALYST      ならば MANAGEMENT/ROLE/ANALYST.mdを読みなさい。
role = REVIEWER     ならば MANAGEMENT/ROLE/REVIEWER_VERIFICATER.mdを読みなさい。
role = VERIFICATER  ならば MANAGEMENT/ROLE/REVIEWER_VERIFICATER.mdを読みなさい。

最初に起動されたleadagent（Codex/Claude実行時に起動されるメインエージェント、すなわちこのセッションの自分自身）は、自身のroleをLEADERとして認識し、MANAGEMENT/ROLE/LEADER.mdを参照して行動すること。
