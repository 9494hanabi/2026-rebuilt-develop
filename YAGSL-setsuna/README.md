# yagsl-setsuna

## セットアップ

### 1. 前提条件
- Claude Code または Codex-CLI がインストールされていること
- Python 3.10以上

### 2. MANAGEMENTスキルのインストール

以下のいずれかの方法でスキルをインストールする:

#### 方法A: 自動インストール（推奨）
エージェントを起動すると、ブートストラップISSUE「install-management-skills」が自動的に処理される。

#### 方法B: 手動インストール
```bash
cp -r MANAGEMENT/SKILLS/public/issue-creator ~/.agent/skills/
cp -r MANAGEMENT/SKILLS/public/trial-creator ~/.agent/skills/
cp -r MANAGEMENT/SKILLS/public/spawn-team ~/.agent/skills/
```

### 3. エージェントの起動
```
/spawn-team
```
でエージェントチームを起動する。

### 4. ブートストラップISSUE
初回起動時、以下のISSUEが用意されている:
1. **install-management-skills** - スキルのインストール
2. **configure-project-environment** - プロジェクト環境の設定（対話的）
3. **identify-recurring-task-skills** - 定常タスクSKILLの特定（対話的）

## ワークフロー概要
MANAGEMENT/ROLE/WORKFLOW.md を参照。
