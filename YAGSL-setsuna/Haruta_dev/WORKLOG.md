# WORKLOG

## 2026-03-01
- `Haruta_dev/` のファイル構成を作成
- `HarutaAGENTS.md` の初版を整理
- `workflow.md` と prompt を実運用向けに更新
- 役割分担、出力形式、禁止事項、情報源ルールを反映
- `policies/` を追加して運用ルールの置き場所を整理
- `PROJECT_OVERVIEW.md` にコード読解ベースの全体像を追加
- `FILE_MAP.md` に主要ファイルの役割と関連を追加
- 回答スタイルと担当ベースの編集制限を追加
- 調査時の確認手順を具体化
- AGENT 起動用プロンプトを追加
- AGENT のコード生成時のみ履歴を残す仕組みを追加
- AGENT のコード提案フォーマットを改善
- AGENT が既存ファイル全文を出さず、変更点だけ出すルールを追加
- `Haruta_dev/` 内の重複ルールを整理して、AI が読みやすい構成に寄せた
- AGENT の提案を「どのファイルの何行目付近に入れるか」が分かる形に強化
- 世界レベルの制御、安全性、ファイル構成のシンプルさを重視するルールを追加
- 命名を短く分かりやすくする方針を追加
- AGENT の提案を「変更後コードだけ + アンカー指示」に簡略化
- WPILib command-based の公式構造を AGENT が読み込める参照メモを追加
- AGENT の回答末尾に、Haruta が次にやるタスクを短く出す運用を追加
- AGENT の提案を「1変更ずつ」、解説を分かりやすく、コメントをコード上に統一する方針を追加
- AGENT の提案で `何行目付近` の表記を必須化
- 次は `ROADMAP.md`、`PROJECT_OVERVIEW.md`、`FILE_MAP.md` の中身を育てる

## 2026-03-02
- `PieceVisionSubsystem`、`AutoVisionCommand`、`AutoIntakeAssistCommand` を基準に `Haruta_dev` の理解文書を補正
- ownership コメントの曖昧ケースに対応できるように、priority ルールを追加
- `コード理解タイム` の運用を追加し、AGENT が理解用コメント提案へ切り替えられるようにした
- bootstrap 読込対象に `AGENT_CODE_HISTORY.md` を追加
- `ROADMAP.md` を今の開発段階に合わせて更新

## Rule
- このファイルには日付ごとの作業記録を書く
- 変更した内容、判断したこと、次にやることを短く残す
