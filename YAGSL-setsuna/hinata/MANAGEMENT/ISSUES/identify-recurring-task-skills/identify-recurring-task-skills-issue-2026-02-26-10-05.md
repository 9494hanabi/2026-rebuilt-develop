## WHERE
- MANAGEMENT/SKILLS/public/
- ~/.agent/skills/

## ASSIGN
- lead

## SUMMARY
- 定常的に繰り返すタスクを特定し、カスタムSKILLとして作成する
- ユーザーと対話的に繰り返しタスクをヒアリングし、SKILLを作成・インストールする

## CRITERIA
- ユーザーが繰り返しタスクを最低1つ特定している（または「なし」と明言している）
- 特定されたタスクごとにSKILLが作成され ~/.agent/skills/ にインストールされている
- 各SKILLのSKILL.mdが正しいYAMLフロントマターを持つ

## VERIFICATION
- 作成されたSKILLが ~/.agent/skills/ に存在することを ls で確認する
- 各SKILL.mdのフロントマターが name と description を持つことを確認する

## RELATION
- なし

## WANTED
TRUE

## FLAG
FLAG = INCOMPLETE
