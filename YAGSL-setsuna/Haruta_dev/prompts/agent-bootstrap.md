# AGENT Bootstrap Prompt

最初に次のファイルを読んでください。
- `Haruta_dev/HarutaAGENTS.md`
- `Haruta_dev/workflow.md`
- `Haruta_dev/AGENT_CODE_HISTORY.md`
- `Haruta_dev/policies/edit-policy.md`
- `Haruta_dev/policies/file-ownership.md`
- `Haruta_dev/policies/research-policy.md`
- `Haruta_dev/policies/response-style.md`
- `Haruta_dev/policies/rule-management.md`
- `Haruta_dev/reference/wpilib-command-based.txt`
- 必要なら `Haruta_dev/PROJECT_OVERVIEW.md`
- 必要なら `Haruta_dev/FILE_MAP.md`
- FRC/ライブラリ情報が必要なら `Haruta_dev/reference/`

読んだあと、次を短く整理して返してください。
1. 読み込んだファイル一覧
2. 理解した最重要ルール
3. 編集対象にできるファイルの条件
4. 調査時の確認手順
5. 今回の依頼の理解
6. 最初に確認するべき関連ファイル
7. 今回見直した最低限守るルール

この chat では、読んだファイルを正本として扱ってください。

追加ルールは次の 4 点だけです。
- 既存ファイルの提案は全文ではなく変更ブロックだけを出す
- 変更ブロックには、何行目付近かと、どのコードの中、下、上に入れるかを示す
- コードを生成したときだけ `Haruta_dev/AGENT_CODE_HISTORY.md` に履歴を残す
- ルールが矛盾したら進めず Haruta に確認する
- 世界レベルの制御、安全性、ファイル構成のシンプルさを意識する
- 命名は、短くて意味が分かるものを優先する
- 変更前コードは出さず、変更後コードだけを出す
- 提案は1変更ずつ順番に出す
- 解説は分かりやすく短く区切る
- コメントはコードの上に書く
- `コード理解タイム` と言われたら、理解用コメント案を 1 変更ずつ出す

作業開始時テンプレート:
- 目的:
- 対象ファイル:
- 制約:
- 完了条件:
- 確認方法:
