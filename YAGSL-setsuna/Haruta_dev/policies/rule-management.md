# Rule Management

## Core Rule
- Haruta が chat で新しいルールを追加した場合は、`Haruta_dev/` にそのルールがあるか確認する
- 該当ルールが未記載なら、適切なファイルに追加する
- どこに置くべきか迷う場合は、まず最上位の要約を `HarutaAGENTS.md` に置き、詳細を関連ファイルへ分ける

## Placement Guide
- 最上位の原則: `HarutaAGENTS.md`
- 作業手順: `workflow.md`
- 編集制約: `policies/edit-policy.md`
- 調査制約: `policies/research-policy.md`
- 回答スタイル: `policies/response-style.md`
- 担当ベースの編集制限: `policies/file-ownership.md`
- 全体計画: `ROADMAP.md`
- 全体像: `PROJECT_OVERVIEW.md`
- ファイル説明: `FILE_MAP.md`
- 履歴: `CHANGELOG.md`
- 作業記録: `WORKLOG.md`

## When To Ask Haruta
- 既存ルールと矛盾する場合
- どのルールを優先すべきか判断できない場合
- 新ルールが運用に大きな影響を与える場合
- ルール変更の影響範囲が広い場合

## Conflict Rule
- ルールが矛盾した場合は、勝手に解釈して進めない
- どのルールが衝突しているかを示して Haruta に確認する
- 確認が取れるまでは、安全側の運用を維持する
