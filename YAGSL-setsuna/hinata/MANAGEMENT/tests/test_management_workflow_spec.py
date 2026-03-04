"""MANAGEMENT workflow specification tests.

This test module treats markdown docs under MANAGEMENT/ as the source of truth
and verifies whether timing/trigger rules are explicitly defined.
"""

from __future__ import annotations

import re
import unittest
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]

DOC_PATHS = {
    "issue": REPO_ROOT / "MANAGEMENT/ISSUES/ISSUE.md",
    "trial": REPO_ROOT / "MANAGEMENT/ISSUES/TRIAL.md",
    "wanted": REPO_ROOT / "MANAGEMENT/ISSUES/WANTED.md",
    "workflow": REPO_ROOT / "MANAGEMENT/ROLE/WORKFLOW.md",
    "leader": REPO_ROOT / "MANAGEMENT/ROLE/LEADER.md",
    "implementer": REPO_ROOT / "MANAGEMENT/ROLE/IMPLEMENTER.md",
    "analyst": REPO_ROOT / "MANAGEMENT/ROLE/ANALYST.md",
    "reviewer_verificater": REPO_ROOT / "MANAGEMENT/ROLE/REVIEWER_VERIFICATER.md",
}


class ManagementWorkflowSpecTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.docs = {
            key: path.read_text(encoding="utf-8")
            for key, path in DOC_PATHS.items()
        }

    def assertRegexAny(self, text: str, patterns: list[str], msg: str) -> None:
        for pattern in patterns:
            if re.search(pattern, text, flags=re.MULTILINE):
                return
        self.fail(msg)

    # --- Utilization timing definitions ---
    def test_issue_utilization_timing_is_defined(self) -> None:
        issue = self.docs["issue"]
        workflow = self.docs["workflow"]
        self.assertIn("ユーザーからの依頼について必ずISSUEを立てること", issue)
        self.assertRegexAny(
            workflow,
            patterns=[
                r"LEADER ->> IMPLEMENTER : /spawn ISSUEを作成し渡す",
                r"LEADER ->> IMPLEMENTER : ISSUEを作成し渡す",
            ],
            msg="WORKFLOWでISSUE受け渡しタイミングが定義されていません。",
        )

    def test_trial_utilization_timing_is_defined(self) -> None:
        workflow = self.docs["workflow"]
        implementer = self.docs["implementer"]
        self.assertRegex(
            workflow,
            r"TRIALの作成|子TRIALの作成依頼",
            msg="WORKFLOWでTRIAL利用タイミングが定義されていません。",
        )
        self.assertIn("TRIALを作成または更新", implementer)

    def test_wanted_utilization_timing_is_defined(self) -> None:
        issue = self.docs["issue"]
        workflow = self.docs["workflow"]
        implementer = self.docs["implementer"]
        self.assertRegex(
            issue,
            r"RELATIONが5つを超えた場合はWANTEDにissueを追加する",
            msg="ISSUE.mdにWANTED追加トリガーがありません。",
        )
        self.assertRegexAny(
            workflow,
            patterns=[
                r"ISSUE\.WANTED\s*==\s*TRUE",
                r"else\s+ISSUE\s*==\s*WANTED",
            ],
            msg="WORKFLOWでWANTED分岐が定義されていません。",
        )
        self.assertIn("`WANTED = TRUE` の場合はANALYSTへPLANNING作成を依頼", implementer)

    # --- Edit trigger definitions ---
    def test_issue_edit_triggers_are_defined(self) -> None:
        issue = self.docs["issue"]
        self.assertIn("FLAG = COMPLETE", issue)
        self.assertIn("MANAGEMENT/COMPLETES/にissueを移動", issue)

    def test_trial_edit_triggers_are_defined(self) -> None:
        trial = self.docs["trial"]
        self.assertIn("実行前に記述する", trial)
        self.assertIn("TRIALを実行後に記述する", trial)
        self.assertIn("TRIALを書いたら、親ISSUEのRELATIONに作成したTRIALを追加しなさい", trial)

    def test_incomplete_issue_edit_trigger_is_defined(self) -> None:
        leader = self.docs["leader"]
        self.assertIn("`ISSUE != COMPLETE`", leader)
        self.assertIn("IMPLEMENTERへ子TRIAL作成を依頼", leader)

    def test_wanted_priority_edit_trigger_is_defined(self) -> None:
        """WANTEDの優先度要素をいつ更新するかが明記されているかを検証する。"""

        wanted = self.docs["wanted"]
        # DIFFICULTY/DEADLY/URGENCY の更新タイミングを示す規則が必要。
        self.assertIn("DIFFICULTY", wanted)
        self.assertIn("DEADLY", wanted)
        self.assertIn("URGENCY", wanted)
        self.assertRegexAny(
            wanted,
            patterns=[
                r"(更新|見直し|再評価|変更).*(場合|タイミング)",
                r"(場合|タイミング).*(更新|見直し|再評価|変更)",
            ],
            msg=(
                "WANTED.mdにDIFFICULTY/DEADLY/URGENCYの編集トリガー"
                "（いつ更新するか）が定義されていません。"
            ),
        )

    def test_spawn_is_required_for_role_handoff(self) -> None:
        workflow = self.docs["workflow"]
        leader = self.docs["leader"]
        implementer = self.docs["implementer"]
        reviewer_verificater = self.docs["reviewer_verificater"]
        analyst = self.docs["analyst"]

        self.assertIn("/spawn ISSUEを作成し渡す", workflow)
        self.assertIn("/spawn PLANNING作成の依頼", workflow)
        self.assertIn("/spawn レビュー/検証の依頼", workflow)
        self.assertIn("/spawn DISCOVERY/REFUTED HYPOTHESES/SUPPORTED HYPOTHESES記述依頼", workflow)
        self.assertIn("/spawn 子TRIALの作成依頼", workflow)

        self.assertIn("ロール起動は必ず `/spawn` を使用", leader)
        self.assertIn("ANALYSTを起動する場合は必ず `/spawn` を使用", implementer)
        self.assertIn("REVIEWER_VERIFICATERの起動は必ず `/spawn` を使用", implementer)
        self.assertIn("ANALYSTの起動は必ず `/spawn` を使用", reviewer_verificater)
        self.assertIn("必ず `/spawn` で起動される", analyst)

    def test_trial_dnp_owner_is_analyst(self) -> None:
        trial = self.docs["trial"]
        self.assertIn("<!-- ANALYSTが記述する。 -->", trial)
        self.assertRegexAny(
            trial,
            patterns=[
                r"### DISCOVERY",
                r"### REFUTED HYPOTHESES",
                r"### SUPPORTED HYPOTHESES",
            ],
            msg="TRIAL.mdにD/R/Sセクションが存在しません。",
        )

    def test_trial_creator_skill_is_required(self) -> None:
        workflow = self.docs["workflow"]
        implementer = self.docs["implementer"]
        trial = self.docs["trial"]

        self.assertIn("trial-creatorでTRIAL作成/更新", workflow)
        self.assertIn("trial-creatorで子TRIAL作成", workflow)
        self.assertIn("TRIAL作成時は必ず `trial-creator` SKILL を使用", implementer)
        self.assertIn("子TRIAL作成時も必ず `trial-creator` SKILL を使用", implementer)
        self.assertIn("TRIAL作成時は必ず `trial-creator` SKILL を使用すること。", trial)


if __name__ == "__main__":
    unittest.main(verbosity=2)
