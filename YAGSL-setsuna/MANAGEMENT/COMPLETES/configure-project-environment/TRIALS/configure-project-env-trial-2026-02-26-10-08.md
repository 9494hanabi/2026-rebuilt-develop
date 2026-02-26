# configure-project-env-trial-2026-02-26-10-08.md

## 0.PARENT ISSUE/TRIAL
- /Users/suzukiakiramuki/Hanabi/2026-develop/2026-rebuilt-develop/YAGSL-setsuna/MANAGEMENT/ISSUES/configure-project-environment/configure-project-environment-issue-2026-02-26-10-05.md

## ENVIRONMENT
- Java / WPILib (FRC) + YAGSL (Swerve Drive Library) + PathPlanner
- ビルドツール: Gradle (GradleRIO)
- Javaソースファイル: 59ファイル
- コーディング規約: 未策定

## HYPOTHESIS
- スキップ（TODO型タスク）

## SOLUTION
- スキップ（TODO型タスク）

## PLAN
- ANALYST分析完了。以下の手順で実装する:
1. コーディング規約・設計ガイドラインドキュメントを `MANAGEMENT/CONVENTIONS.md` として作成
   - Java命名規則（kPrefixPascalCase定数、camelCase変数、PascalCaseクラス）
   - パッケージ構造規約
   - コメント・Javadoc規約（日本語継続）
   - インポート順序（WPILib -> PathPlanner -> YAGSL -> org -> java -> frc.robot）
   - フォーマッティングルール（4スペース、120文字幅、K&R）
   - Command-Based設計ガイドライン
   - Subsystem設計原則（IO抽象化パターン推奨）
   - 定数管理方針（ドメイン別分割維持）
   - ログ・デバッグ方針（AdvantageKit + System.out.printf + DriverStation）
2. CLAUDE.mdを更新してプロジェクトの実際のファイル構造・技術スタックを反映
3. REVIEWER_VERIFICATERにレビュー依頼
- 注意: typo修正（controllboard, odmetry）とm_プレフィクス廃止は別TRIALとして計画（本TRIALのスコープ外）

## CRITERIA
- CLAUDE.mdのファイル構造セクションがプロジェクトの実際の構造と一致している
- コーディング規約・設計ガイドラインドキュメントが作成されている
- 使用技術・フレームワークがドキュメントに記録されている

## VERIFICATION
- CLAUDE.mdのファイル構造セクションと実際のディレクトリ構造を比較して一致を確認
- 合意形成ドキュメントにコーディング規約・設計ガイドラインが明記されていることを目視確認

## SUMMARY
- プロジェクト環境設定として、コーディング規約・設計ガイドラインドキュメントの作成とCLAUDE.mdの更新を実施した。

## DETAIL
- ANALYSTによる既存コードベース分析に基づき、FRC/WPILib/YAGSLのベストプラクティスを考慮したコーディング規約・設計ガイドラインを策定した。
- 規約は以下を網羅: 命名規則、パッケージ構造、コメント・Javadoc、インポート順序、フォーマッティング、Command-Based設計、Subsystem設計原則、定数管理、ログ・デバッグ方針。
- CLAUDE.mdに技術スタック情報と、実際のディレクトリ構造に基づくファイル構造セクションを追加・更新した。
- typo修正（controllboard, odmetry）やm_プレフィクス廃止は既存コードへの影響が大きいため、本TRIALのスコープ外とし将来の別TRIALで対応予定とした。

## CHANGES
- MANAGEMENT/CONVENTIONS.md（新規作成）
- CLAUDE.md（更新: 技術スタックセクション追加、ファイル構造セクションを実際の構造に更新、CONVENTIONS.mdへの参照追加）

## REVIEW SUMMARY
- **判定: PASS（修正対応後の再レビューで全指摘解消を確認）**

### 合格項目
1. CONVENTIONS.mdの規約内容は既存コードベースの分析に基づいており、FRC/WPILib/YAGSLのベストプラクティスに沿っている。
2. 命名規則、パッケージ構造、Command-Based設計、定数管理、ログ方針の各セクションは網羅的で実用的。
3. 既存コードとの乖離（`m_`プレフィクス、typo）を「将来修正」として明確にスコープ外にしている点は適切。
4. CLAUDE.mdの技術スタックセクションは正確。
5. CONVENTIONS.mdとCLAUDE.mdの技術スタック表が同一で整合性がある。

### 初回指摘と対応結果
1. **CLAUDE.md: `HOWTOCONTRIBUTE.md` -> `README.md`** -- 実在しない `HOWTOCONTRIBUTE.md` を実在する `README.md` に修正。指摘1,2を同時に解決。**解決済み。**
2. **CLAUDE.md: `lib/pathplanner/util/swerve/`** -- 再検証の結果、空ディレクトリであり記載不要。**初回指摘が不正確であった。修正不要。**
3. **CONVENTIONS.md: `kPrefix` なし定数** -- 備考セクションに既に移行方針が記載されている。**修正不要。**

## VERIFICATION SUMMARY
- **判定: PASS（修正対応後の再検証で全3項目合格）**

### CRITERIA 1: CLAUDE.mdのファイル構造セクションがプロジェクトの実際の構造と一致している
- **結果: PASS**
- 検証方法: `find` コマンドで実際のディレクトリ構造・ファイル一覧を取得し、CLAUDE.mdの記載と照合。
- Javaソースファイル構造（`src/main/java/frc/robot/` 以下）は全ディレクトリ・主要ファイルが正確に記載されている。
- deploy構造（`src/main/deploy/`）も正確。
- MANAGEMENT構造も正確（初回指摘の `HOWTOCONTRIBUTE.md` -> `README.md` 修正により解消）。
- 空ディレクトリ（`pathplanner/util/swerve/`）は記載不要と判断。

### CRITERIA 2: コーディング規約・設計ガイドラインドキュメントが作成されている
- **結果: PASS**
- 検証方法: `MANAGEMENT/CONVENTIONS.md` の存在および内容を確認。
- 命名規則、パッケージ構造、コメント・Javadoc規約、インポート順序、フォーマッティングルール、Command-Based設計ガイドライン、Subsystem設計原則、定数管理方針、ログ・デバッグ方針が明記されている。

### CRITERIA 3: 使用技術・フレームワークがドキュメントに記録されている
- **結果: PASS**
- 検証方法: CLAUDE.mdの技術スタックセクションを確認。
- Java / WPILib / YAGSL / PathPlanner / Gradle (GradleRIO) / JUnit 5 / AdvantageKit が全て記載されている。

### DISCOVERY
- 既存コードベース59ファイルの命名パターン調査により、`m_`プレフィクス使用は59ファイル中2ファイル（Robot.java, RobotContainer.java）のみであり、プロジェクト全体ではcamelCase（`m_`なし）が事実上の標準であることが判明した。
- 定数命名に3パターンが混在していた: (1)`k`+PascalCase（多数派）、(2)`SCREAMING_SNAKE_CASE`（SwerveSubsystem内部定数）、(3)`k`なしcamelCase（`useMegaTag2`, `maxSpeed`等）。このうち(3)は規約策定時に明示的な移行方針の記載が不足していた点がレビューで指摘された。
- CLAUDE.mdのファイル構造記述において、実在しないファイル（HOWTOCONTRIBUTE.md）の記載と、実在するファイル/ディレクトリ（README.md, pathplanner/util/swerve/）の欠落が検出された。ファイル構造のドキュメント化は手動で行ったため、網羅性の検証が不十分だった。
- プロジェクト内にtypoが2箇所定着している（`controllboard`, `odmetry`）。パッケージ名のtypoはimport文の全面書き換えを伴うため、本TRIALのスコープ外として分離する判断は妥当だった。
- `lib/pathplanner/`配下にPathPlannerライブラリの内部クラスを独自再実装しているファイル群が存在する。カスタマイズ目的か上流ライブラリ未提供APIの補完かは本TRIALでは未調査。ライブラリ更新時の互換性リスクとして認識された。

### REFUTED HYPOTHESES
- 本TRIALはTODO型タスクとしてHYPOTHESISをスキップしているため、明示的に反証された仮説はない。
- ただし暗黙の前提として「CLAUDE.mdのファイル構造を手動記述すれば実態と一致する」という想定があったが、レビューにより3箇所の不一致が検出された。手動記述のみでは網羅性を保証できないことが示された。

### SUPPORTED HYPOTHESES
- 本TRIALはTODO型タスクとしてHYPOTHESISをスキップしているため、明示的に検証された仮説はない。
- ただし、PLANの前提であった「既存コードベースの実態分析に基づいて規約を策定すれば、チームにとって実用的かつ採用可能な規約になる」という方針は、レビューにおいてCONVENTIONS.mdが「既存コードベースの分析に基づいており、FRC/WPILib/YAGSLのベストプラクティスに沿っている」と評価されたことで支持された。

## RELATION
- なし

## STATUS
PROGRESS
