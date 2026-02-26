# AGENTS.md

Codex-CLI向けの設定。CLAUDE.mdと共通の指示はCLAUDE.mdを参照すること。

## Skills
A skill is a set of local instructions to follow that is stored in a `SKILL.md` file.

### How to use skills
- Discovery: 利用可能なスキル一覧（name / description / file path）はセッション側で提示される。必要時にその `SKILL.md` を開いて参照すること。
- Trigger rules: ユーザーがスキル名（`$skill-name` または平文）を指定した場合、または依頼内容がスキル説明と明確に一致する場合はそのスキルを使うこと。複数指定時は必要最小限の組み合わせで適用すること。
- Missing/blocked: 指定されたスキルが見つからない/読めない場合は短く理由を述べ、代替手段で継続すること。
- Progressive disclosure:
  1) まず `SKILL.md` を開き、実行に必要な範囲だけ読む。
  2) 相対パス参照は、まずそのスキルディレクトリ基準で解決する。
  3) `references/` などの追加資料は必要ファイルだけ読む（全読みしない）。
  4) `scripts/` やテンプレートがある場合は再利用を優先する。
- Coordination and sequencing:
  1) 複数スキルが該当する場合は適用順を明示する。
  2) 明らかに該当するスキルを使わない場合は理由を一言添える。
- Context hygiene:
  1) 長文は要約を優先し、不要な全文貼り付けを避ける。
  2) 深い参照追跡は避け、直接必要なファイルを優先する。
  3) 複数バリアントがある場合は対象に必要な参照のみ選択する。
- Safety and fallback: スキル適用が不明瞭または破綻する場合は、問題点を示してから最善の代替で継続すること。
