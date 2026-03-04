# WANTED.md

WANTEDは、RELATIONが5つを超えたISSUEを高難度案件として管理するためのリスト。

## 追加タイミング
- ISSUEのRELATIONが5つを超えた場合、LEADERが対象ISSUEをWANTEDへ追加する。

## 記述フォーマット
## issue name

    DIFFICULTY  - I ~ IIIII
    DEADLY      - I ~ IIIII
    URGENCY     - I ~ IIIII

    - summary

## 優先度要素の編集トリガー
- 以下の更新があった場合、LEADERまたはANALYSTがDIFFICULTY/DEADLY/URGENCYを再評価して変更する。
  - 新しい子TRIALが追加され、実装の複雑性が増えた場合
  - 検証で重大な失敗や副作用が発見された場合
  - 期限や外部依存の変更で緊急度が変わった場合

## 見直しタイミング
- 最低でも以下のタイミングで見直しを行う。
  - 週次の進行確認時
  - ISSUEのFLAGがINCOMPLETEのまま2回連続で継続した場合
  - ユーザーから優先度変更の要求があった場合

## 編集ルール
- 更新時はDIFFICULTY/DEADLY/URGENCYの変更理由を1行で追記する。
- 変更後の優先度に応じてIMPLEMENTERへの再割り当てをLEADERが判断する。
