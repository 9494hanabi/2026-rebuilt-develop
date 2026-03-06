# CHANGELOG

## 2026-03-01
- `Haruta_dev/` の初期構成を作成
- `HarutaAGENTS.md` を運用入口として整理
- `WORKLOG.md` と `handoff-template.md` を追加
- Codex app と AGENT の役割分担を明確化
- 無断変更禁止、無断削除禁止、公式情報優先、SIM フェーズを追加
- `ROADMAP.md`、`PROJECT_OVERVIEW.md`、`FILE_MAP.md`、`reference/` を追加
- `policies/` を追加して、編集方針、調査方針、ルール管理方針を分離
- Codex app の編集範囲を `Haruta_dev/` のみに制限
- `Haruta_dev/` の変更も事前確認制に変更
- 公式情報は複数の公式ページを比較して判断するルールを追加
- ルール追加時の追記ルールと、矛盾時の確認ルールを追加
- Codex app の回答スタイルを追加
- AGENT のコードコメント方針を追加
- 担当表記ベースの編集制限を追加
- 調査時の確認手順を強化
- 公式優先、2026 シーズン優先、WPILib 常時確認、非公式は発想参考のみを追加
- AGENT にルールを読み込ませる起動用プロンプトを追加
- AGENT がコード生成時だけ履歴を残すルールを追加
- AGENT のコード提案を、差し込み位置と操作種別まで明示する形式に強化
- AGENT の既存ファイル提案では、全文ではなく差分ブロックだけを出すルールを追加
- `HarutaAGENTS`、`workflow`、`prompts/` の重複を減らし、正本の置き場所を整理
- AGENT の提案を「対象ファイル + 対象行 + アンカー」で出す形に強化
- 世界最強クラスの FRC メンター視点、世界レベルの制御と安全性、ファイル増殖防止、ルール再確認を追加
- 命名を「短くて意味が分かる」方針に追加
- AGENT の提案を「変更後コードだけ + どこに入れるか」に簡略化
- WPILib 公式 command-based 構成の参照メモを追加し、AGENT の標準読込に入れた
- AGENT の回答末尾に、Haruta が次にやるタスクを短く出すルールを追加
- AGENT の提案を「1変更ずつ」、解説を分かりやすく、コメントをコード上に統一する方針を追加
- AGENT の提案で `何行目付近` の表記を必須化

## 2026-03-02
- `PieceVisionSubsystem` と Fuel 回収 command の実コードに合わせて `PROJECT_OVERVIEW.md` と `FILE_MAP.md` を更新
- ownership ルールに `誰でも`、`共通`、`専用`、複数表記の優先順位を追加
- `AGENT_CODE_HISTORY.md` を bootstrap の標準読込に追加
- `コード理解タイム` の運用を `HarutaAGENTS.md`、`workflow.md`、`response-style.md`、prompt に追加
- `ROADMAP.md` を Fuel 回収と AGENT 運用改善の現在地に合わせて更新

## Rule
- このファイルには運用ルールやテンプレートの変更だけを書く
- 日々の作業記録は `WORKLOG.md` に書く
