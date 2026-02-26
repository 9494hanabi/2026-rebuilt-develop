---
name: spawn-team
description: MANAGEMENT/ROLE/ の定義に基づき、LEADER/IMPLEMENTER/ANALYST/REVIEWER_VERIFICATER のagent teamを起動する。LEADERがISSUEを委譲する前にチームを立ち上げる際に使用する。
---

# Spawn Team

CLAUDE.md と MANAGEMENT/ROLE/*.md に従い、プロジェクトのワークフローロールをagent teamとして起動する。

## 前提

- このスキルを呼び出すエージェントは自身を **LEADER** として認識していること。
- MANAGEMENT/ROLE/WORKFLOW.md に定義されたワークフローに従って運用すること。

## Run This Skill

### 1. チーム名を決定する

引数でチーム名が渡された場合はそれを使用する。渡されなかった場合はユーザーに確認する。

### 2. TeamCreate でチームを作成する

```
TeamCreate:
  team_name: <チーム名>
  description: "MANAGEMENT workflow team"
  agent_type: "leader"
```

### 3. ロール定義を読み込む

以下のファイルを読み込み、各ロールのプロンプトに反映する。

- `MANAGEMENT/ROLE/WORKFLOW.md`
- `MANAGEMENT/ROLE/IMPLEMENTER.md`
- `MANAGEMENT/ROLE/ANALYST.md`
- `MANAGEMENT/ROLE/REVIEWER_VERIFICATER.md`

### 4. 各ロールのagentを起動する

以下の3エージェントを Task ツールで起動する。**全エージェントを並列で起動すること。**

LEADER（自分自身）はチームリーダーとして振る舞う。spawn不要。

#### IMPLEMENTER

```
Task:
  name: "implementer"
  team_name: <チーム名>
  subagent_type: "general-purpose"
  prompt: |
    あなたは IMPLEMENTER ロールです。

    ## 行動規則
    以下のファイルを読み、その内容に厳密に従って行動しなさい。
    - CLAUDE.md
    - MANAGEMENT/ROLE/IMPLEMENTER.md
    - MANAGEMENT/ROLE/WORKFLOW.md

    ## チーム内コミュニケーション
    - LEADERからISSUEを受け取ったら作業を開始する。
    - ANALYST への依頼は SendMessage で "analyst" 宛に送る。
    - REVIEWER_VERIFICATER への依頼は SendMessage で "reviewer-verificater" 宛に送る。
    - 検証完了後は SendMessage で "leader" 宛に通知する。
    - TRIAL作成時は必ず trial-creator スキルを使用する。

    まず上記ファイルを読み、タスクの割り当てを待ちなさい。
```

#### ANALYST

```
Task:
  name: "analyst"
  team_name: <チーム名>
  subagent_type: "general-purpose"
  prompt: |
    あなたは ANALYST ロールです。

    ## 行動規則
    以下のファイルを読み、その内容に厳密に従って行動しなさい。
    - CLAUDE.md
    - MANAGEMENT/ROLE/ANALYST.md
    - MANAGEMENT/ROLE/WORKFLOW.md

    ## チーム内コミュニケーション
    - IMPLEMENTER または REVIEWER_VERIFICATER から依頼を受けて作業を開始する。
    - 分析結果は依頼元に SendMessage で返却する。
    - PLANNING依頼の場合: 推奨方針/根拠/リスク/次アクションを返す。
    - D/R/S記述依頼の場合: DISCOVERY/REFUTED HYPOTHESES/SUPPORTED HYPOTHESES をTRIALに記述する。

    まず上記ファイルを読み、依頼を待ちなさい。
```

#### REVIEWER_VERIFICATER

```
Task:
  name: "reviewer-verificater"
  team_name: <チーム名>
  subagent_type: "general-purpose"
  prompt: |
    あなたは REVIEWER_VERIFICATER ロールです。

    ## 行動規則
    以下のファイルを読み、その内容に厳密に従って行動しなさい。
    - CLAUDE.md
    - MANAGEMENT/ROLE/REVIEWER_VERIFICATER.md
    - MANAGEMENT/ROLE/WORKFLOW.md

    ## チーム内コミュニケーション
    - IMPLEMENTER からレビュー/検証依頼を受けて作業を開始する。
    - ANALYST への D/R/S 記述依頼は SendMessage で "analyst" 宛に送る。
    - レビュー・検証完了後は SendMessage で依頼元の IMPLEMENTER に結果を返す。
    - テスト作成が必要な場合は /codex-test スキルを使用する。

    まず上記ファイルを読み、依頼を待ちなさい。
```

### 5. ユーザーに報告する

起動完了後、以下を報告する。

- チーム名
- 起動したロール一覧（LEADER=自分, IMPLEMENTER, ANALYST, REVIEWER_VERIFICATER）
- 次のアクション: ISSUEを作成してIMPLEMENTERへ委譲する（issue-creator スキル使用）
