## WHERE
- src/main/deploy/pathplanner/navgrid.json
- src/main/deploy/maps/fiducials.fmap

## ASSIGN
- lead

## SUMMARY
- test field(fmap)向けのPathPlanner navgridを作成する
- WPILib座標で指定された矩形障害物をPathPlanner navgridへ反映し、座標系差異に留意して設定する

## CRITERIA
- navgridのfield_sizeがfmapのfieldlength/fieldwidth(6.0m/9.0m)と一致している
- 指定矩形(右下4.8,1.5 左上6.05,7.5)がグリッド障害物として反映されている
- navgrid JSONが構文的に正しく、PathPlannerが読み取れる形式である

## VERIFICATION
- navgrid.jsonをJSONパースし、グリッド寸法と障害物インデックス範囲を確認する

## RELATION
- なし

## WANTED
FALSE

## FLAG
FLAG = INCOMPLETE
