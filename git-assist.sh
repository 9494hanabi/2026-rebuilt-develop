#!/usr/bin/env bash
set -euo pipefail

# =========================
# git-assist.sh
# Interactive helper for:
#  - commit & push
#  - merge
#  - pull
<<<<<<< HEAD
# =========================

=======
#  + sync frc/robot between YAGSL-setsuna <-> YAGSL-daisha
# =========================

# ---------- constants ----------
REL_ROBOT_DIR="src/main/java/frc/robot"

>>>>>>> a3d8c66 (test(misc): テスト - テスト [stable])
# ---------- helpers ----------
die() { echo "❌ $*" >&2; exit 1; }

need_git_repo() {
  git rev-parse --is-inside-work-tree >/dev/null 2>&1 || die "ここはGitリポジトリではありません。"
}

<<<<<<< HEAD
=======
repo_root() {
  git rev-parse --show-toplevel
}

>>>>>>> a3d8c66 (test(misc): テスト - テスト [stable])
current_branch() {
  git rev-parse --abbrev-ref HEAD
}

has_changes() {
  [[ -n "$(git status --porcelain)" ]]
}

prompt() {
  local msg="$1"
  local default="${2:-}"
  local ans=""
  if [[ -n "$default" ]]; then
    read -r -p "$msg [$default]: " ans
    ans="${ans:-$default}"
  else
    read -r -p "$msg: " ans
  fi
  echo "$ans"
}

prompt_yn() {
  local msg="$1"
  local default="${2:-Y}" # Y or N
  local ans=""
  read -r -p "$msg [${default}/$( [[ "$default" == "Y" ]] && echo "n" || echo "y" )]: " ans
  ans="${ans:-$default}"
  case "${ans,,}" in
    y|yes) return 0 ;;
    n|no)  return 1 ;;
<<<<<<< HEAD
    *)     # if weird input, use default
           [[ "$default" == "Y" ]] && return 0 || return 1 ;;
  esac
}

=======
    *)     [[ "$default" == "Y" ]] && return 0 || return 1 ;;
  esac
}

# IMPORTANT:
# - UI output -> stderr
# - return value -> stdout
>>>>>>> a3d8c66 (test(misc): テスト - テスト [stable])
select_one() {
  local title="$1"; shift
  local -a options=("$@")
  local PS3="番号を選んでください: "

  echo "" >&2
  echo "🧩 $title" >&2

  select opt in "${options[@]}"; do
    if [[ -n "${opt:-}" ]]; then
<<<<<<< HEAD
      printf "%s\n" "$opt"   # ← 戻り値はstdout
=======
      printf "%s\n" "$opt"
>>>>>>> a3d8c66 (test(misc): テスト - テスト [stable])
      return 0
    fi
    echo "もう一度選んでね。" >&2
  done
}

ensure_clean_or_confirm() {
  if has_changes; then
<<<<<<< HEAD
    echo ""
    echo "📌 現在の変更があります:"
    git status --short
    echo ""
=======
    echo "" >&2
    echo "📌 現在の変更があります:" >&2
    git status --short >&2
    echo "" >&2
>>>>>>> a3d8c66 (test(misc): テスト - テスト [stable])
    if ! prompt_yn "このまま進めますか？" "Y"; then
      die "中断しました。"
    fi
  fi
}

# ---------- commit message builder ----------
map_scope() {
  local s="$1"
  case "$s" in
    ドライブ) echo "drive" ;;
    ビジョン) echo "vision" ;;
<<<<<<< HEAD
    オート) echo "auto" ;;
    タレット) echo "talet" ;;
    シューター) echo "shooter" ;;
    インテーク) echo "intake" ;;
    クライム) echo "clime" ;;
=======
    シューター) echo "shooter" ;;
    インテーク) echo "intake" ;;
>>>>>>> a3d8c66 (test(misc): テスト - テスト [stable])
    その他の機能) echo "misc" ;;
    *) echo "misc" ;;
  esac
}

map_type() {
  local t="$1"
  case "$t" in
<<<<<<< HEAD
    エディット) echo "feat" ;;             # 新規/改善もここに寄せる
=======
    エディット) echo "feat" ;;                  # 新規/改善はここに寄せる
>>>>>>> a3d8c66 (test(misc): テスト - テスト [stable])
    リファクタリング\(整形\)) echo "refactor" ;;
    デバッグ) echo "fix" ;;
    テスト) echo "test" ;;
    *) echo "chore" ;;
  esac
}

build_commit_message() {
  local op="$1"

  local action
<<<<<<< HEAD
  action="$(select_one "編集した機能を教えて下さい。" "ドライブ" "ビジョン" "シューター" "インテーク" "その他の機能")"
  local feature_name
  feature_name="$(prompt "機能の名前を教えて下さい" "例: クライム")"
  local edit_kind
  edit_kind="$(select_one "編集内容を教えて下さい" "エディット" "リファクタリング(整形)" "デバッグ" "テスト")"
  local detail
  detail="$(prompt "編集内容の詳細を教えて下さい" "例: L1に登る機能を作成した。")"
  local stability
  stability="$(select_one "コードの状態を教えて下さい。" "安定" "バグ有り")"

  local tag_line=""
  if prompt_yn "タグを打ちますか？" "N"; then
    local tags
    tags="$(prompt "タグ(カンマ区切り)を入力" "例: climb,auto")"
    # normalize spaces
=======
  action="$(select_one "編集した機能を教えて下さい。" "ドライブ" "ビジョン" "シューター" "インテーク" "その他の機能" | tail -n 1 | tr -d '\r')"

  local feature_name
  feature_name="$(prompt "機能の名前を教えて下さい" "例: クライム")"

  local edit_kind
  edit_kind="$(select_one "編集内容を教えて下さい" "エディット" "リファクタリング(整形)" "デバッグ" "テスト" | tail -n 1 | tr -d '\r')"

  local detail
  detail="$(prompt "編集内容の詳細を教えて下さい" "例: L1に登る機能を作成した。")"

  local stability
  stability="$(select_one "コードの状態を教えて下さい。" "安定" "バグ有り" | tail -n 1 | tr -d '\r')"

  local tag_line=""
  if prompt_yn "タグを打ちますか？（コミット本文に Tags: として追記）" "N"; then
    local tags
    tags="$(prompt "タグ(カンマ区切り)を入力" "例: climb,auto")"
>>>>>>> a3d8c66 (test(misc): テスト - テスト [stable])
    tags="$(echo "$tags" | tr -d ' ' )"
    tag_line="Tags: $tags"
  fi

  local type scope status
  type="$(map_type "$edit_kind")"
  scope="$(map_scope "$action")"
  status="$([[ "$stability" == "安定" ]] && echo "stable" || echo "buggy")"

  # Subject (1行目)
  local subject="${type}(${scope}): ${feature_name} - ${detail} [${status}]"

  # Body (2行目以降)
  local body=""
  body+="Operation: ${op}\n"
  body+="Category: ${action}\n"
  body+="Feature: ${feature_name}\n"
  body+="Change: ${edit_kind}\n"
  body+="Status: ${stability}\n"
  if [[ -n "$tag_line" ]]; then
    body+="${tag_line}\n"
  fi

  echo -e "${subject}\n\n${body}"
}

<<<<<<< HEAD
=======
# ---------- project discovery for sync ----------
# いろんな置き方に対応:
# - <repo>/YAGSL-setsuna/<REL_ROBOT_DIR>
# - <repo>/YAGSL-daisha/<REL_ROBOT_DIR>
# - <repo>/YAGSL-daisha/YAGSL-daisha/<REL_ROBOT_DIR> みたいな一段ネスト
contains_robot_dir() {
  local base="$1"
  [[ -d "$base/$REL_ROBOT_DIR" ]]
}

# base (candidate root) の中で REL_ROBOT_DIR を持つ「実プロジェクトroot」を返す
resolve_project_root_with_robot() {
  local base="$1"

  # 1) base直下
  if contains_robot_dir "$base"; then
    echo "$base"
    return 0
  fi

  # 2) base直下の "同名フォルダ" (よくある)
  local bn
  bn="$(basename "$base")"
  if [[ -d "$base/$bn" ]] && contains_robot_dir "$base/$bn"; then
    echo "$base/$bn"
    return 0
  fi

  # 3) base直下の子を1段だけ走査
  local d
  for d in "$base"/*; do
    [[ -d "$d" ]] || continue
    if contains_robot_dir "$d"; then
      echo "$d"
      return 0
    fi
  done

  echo ""
  return 1
}

find_project_dir() {
  local repo="$1"
  local name="$2"
  local candidate="$repo/$name"
  [[ -d "$candidate" ]] || { echo ""; return 1; }
  resolve_project_root_with_robot "$candidate"
}

detect_current_project() {
  local pwd_abs
  pwd_abs="$(pwd)"
  case "$pwd_abs" in
    *"/YAGSL-setsuna"/*|*"/YAGSL-setsuna") echo "YAGSL-setsuna" ;;
    *"/YAGSL-daisha"/*|*"/YAGSL-daisha") echo "YAGSL-daisha" ;;
    *) echo "" ;;
  esac
}

sync_robot_code() {
  local src_project="$1"
  local dst_project="$2"

  local src="$src_project/$REL_ROBOT_DIR"
  local dst="$dst_project/$REL_ROBOT_DIR"

  [[ -d "$src" ]] || die "同期元が見つかりません: $src"
  mkdir -p "$(dirname "$dst")"

  echo "" >&2
  echo "🔁 同期: $(basename "$src_project") -> $(basename "$dst_project")" >&2
  echo "   FROM: $src" >&2
  echo "   TO  : $dst" >&2

  # 同期先がGit管理なら、未コミットがあれば警告
  if git -C "$dst_project" rev-parse --is-inside-work-tree >/dev/null 2>&1; then
    local st
    st="$(git -C "$dst_project" status --porcelain || true)"
    if [[ -n "$st" ]]; then
      echo "⚠️ 同期先に未コミット変更があります:" >&2
      git -C "$dst_project" status --short >&2
      if ! prompt_yn "それでも上書き同期しますか？" "N"; then
        die "同期を中断しました。"
      fi
    fi
  fi

  # バックアップ（任意）
  if prompt_yn "同期前にバックアップを作りますか？" "Y"; then
    local ts bak
    ts="$(date +"%Y%m%d_%H%M%S")"
    bak="$dst_project/.git-assist-backup/$ts/$REL_ROBOT_DIR"
    mkdir -p "$bak"
    if [[ -d "$dst" ]]; then
      rsync -a "$dst/" "$bak/"
    fi
    echo "🗄️ Backup: $bak" >&2
  fi

  # 実同期（ミラー）
  # ※ --delete: 同期元に無いファイルは同期先から消える（完全に同じにする）
  rsync -a --delete "$src/" "$dst/"
  echo "✅ 同期完了" >&2

  # ついでに同期先の差分を表示
  if git -C "$dst_project" rev-parse --is-inside-work-tree >/dev/null 2>&1; then
    echo "" >&2
    echo "📌 同期先の差分 (git status):" >&2
    git -C "$dst_project" status --short >&2 || true
  fi
}

>>>>>>> a3d8c66 (test(misc): テスト - テスト [stable])
# ---------- operations ----------
do_commit_and_push() {
  need_git_repo

<<<<<<< HEAD
  echo ""
  echo "🧾 Commit & Push を開始します。"
  echo "現在のブランチ: $(current_branch)"
  echo ""

  if ! has_changes; then
    echo "✅ 変更がありません (git status が空)。コミット不要です。"
    return 0
  fi

  git status --short
  echo ""
=======
  echo "" >&2
  echo "🧾 Commit & Push を開始します。" >&2
  echo "現在のブランチ: $(current_branch)" >&2
  echo "" >&2

  if ! has_changes; then
    echo "✅ 変更がありません (git status が空)。コミット不要です。" >&2
    return 0
  fi

  git status --short >&2
  echo "" >&2
>>>>>>> a3d8c66 (test(misc): テスト - テスト [stable])

  if prompt_yn "git add -A (全変更をステージ) しますか？" "Y"; then
    git add -A
  else
<<<<<<< HEAD
    echo "ℹ️ ステージしない場合、手動で git add してからもう一度実行してね。"
=======
    echo "ℹ️ 手動で git add してからもう一度実行してね。" >&2
>>>>>>> a3d8c66 (test(misc): テスト - テスト [stable])
    die "中断しました。"
  fi

  local msg
  msg="$(build_commit_message "commit-push")"

<<<<<<< HEAD
  echo ""
  echo "📝 生成されたコミットメッセージ:"
  echo "------------------------------"
  echo "$msg"
  echo "------------------------------"
  echo ""
=======
  echo "" >&2
  echo "📝 生成されたコミットメッセージ:" >&2
  echo "------------------------------" >&2
  echo "$msg" >&2
  echo "------------------------------" >&2
  echo "" >&2
>>>>>>> a3d8c66 (test(misc): テスト - テスト [stable])

  if ! prompt_yn "このメッセージで commit しますか？" "Y"; then
    die "中断しました。"
  fi

  git commit -m "$(echo "$msg" | head -n 1)" -m "$(echo "$msg" | tail -n +3)"

  # push (set upstream if needed)
  local branch
  branch="$(current_branch)"

<<<<<<< HEAD
  echo ""
  echo "🚀 push します: origin ${branch}"
=======
  echo "" >&2
  echo "🚀 push します: origin ${branch}" >&2
>>>>>>> a3d8c66 (test(misc): テスト - テスト [stable])

  if git rev-parse --abbrev-ref --symbolic-full-name "@{u}" >/dev/null 2>&1; then
    git push
  else
    git push -u origin "$branch"
  fi

<<<<<<< HEAD
  echo "✅ 完了: commit & push"

  # optional annotated git tag (Git tag, not "Tags:" in message)
=======
  echo "✅ 完了: commit & push" >&2

  # optional annotated git tag (Git tag)
>>>>>>> a3d8c66 (test(misc): テスト - テスト [stable])
  if prompt_yn "Gitのタグ (git tag -a) も作りますか？" "N"; then
    local tname
    tname="$(prompt "タグ名" "例: v0.3.0")"
    local tmsg
    tmsg="$(prompt "タグの説明" "release")"
    git tag -a "$tname" -m "$tmsg"
    git push origin "$tname"
<<<<<<< HEAD
    echo "🏷️ タグ作成＆push: $tname"
=======
    echo "🏷️ タグ作成＆push: $tname" >&2
  fi

  # ----- optional: sync frc/robot to the other project -----
  local root setsuna_dir daisha_dir
  root="$(repo_root)"

  setsuna_dir="$(find_project_dir "$root" "YAGSL-setsuna" || true)"
  daisha_dir="$(find_project_dir "$root" "YAGSL-daisha" || true)"

  if [[ -n "${setsuna_dir:-}" && -n "${daisha_dir:-}" ]]; then
    if prompt_yn "もう片方へ $REL_ROBOT_DIR を同期（貼り付け）しますか？" "Y"; then
      local cur
      cur="$(detect_current_project)"

      if [[ -z "$cur" ]]; then
        cur="$(select_one "どちらを編集してコミットしましたか？" "YAGSL-setsuna" "YAGSL-daisha" | tail -n 1 | tr -d '\r')"
      fi

      if [[ "$cur" == "YAGSL-setsuna" ]]; then
        sync_robot_code "$setsuna_dir" "$daisha_dir"
      else
        sync_robot_code "$daisha_dir" "$setsuna_dir"
      fi
    fi
  else
    echo "ℹ️ 同期先/元のプロジェクトが見つからないため、同期はスキップしました。" >&2
    echo "   検出結果: setsuna='${setsuna_dir:-}' daisha='${daisha_dir:-}'" >&2
    echo "   期待: <repo>/YAGSL-setsuna と <repo>/YAGSL-daisha (または一段ネスト) に $REL_ROBOT_DIR がある" >&2
>>>>>>> a3d8c66 (test(misc): テスト - テスト [stable])
  fi
}

do_merge() {
  need_git_repo

<<<<<<< HEAD
  echo ""
  echo "🔀 Merge を開始します。"
  echo "現在のブランチ(マージ先): $(current_branch)"
  echo ""
=======
  echo "" >&2
  echo "🔀 Merge を開始します。" >&2
  echo "現在のブランチ(マージ先): $(current_branch)" >&2
  echo "" >&2
>>>>>>> a3d8c66 (test(misc): テスト - テスト [stable])

  ensure_clean_or_confirm

  git fetch origin --prune

<<<<<<< HEAD
  echo ""
  echo "📚 ローカルブランチ:"
  git branch --format="%(refname:short)" | sed 's/^/  - /'

  echo ""
  echo "🌐 リモート(origin)ブランチ:"
  git branch -r --format="%(refname:short)" | sed 's/^/  - /'

  echo ""
  local from
  from="$(prompt "どのブランチを取り込みますか？ (例: dev)" "dev")"

  # try local first, fallback to origin/<from>
  if git show-ref --verify --quiet "refs/heads/$from"; then
    echo "➡️ 取り込み元: $from (local)"
    git merge --no-ff "$from"
  elif git show-ref --verify --quiet "refs/remotes/origin/$from"; then
    echo "➡️ 取り込み元: origin/$from (remote)"
=======
  echo "" >&2
  echo "📚 ローカルブランチ:" >&2
  git branch --format="%(refname:short)" | sed 's/^/  - /' >&2

  echo "" >&2
  echo "🌐 リモート(origin)ブランチ:" >&2
  git branch -r --format="%(refname:short)" | sed 's/^/  - /' >&2

  echo "" >&2
  local from
  from="$(prompt "どのブランチを取り込みますか？ (例: dev)" "dev")"

  if git show-ref --verify --quiet "refs/heads/$from"; then
    echo "➡️ 取り込み元: $from (local)" >&2
    git merge --no-ff "$from"
  elif git show-ref --verify --quiet "refs/remotes/origin/$from"; then
    echo "➡️ 取り込み元: origin/$from (remote)" >&2
>>>>>>> a3d8c66 (test(misc): テスト - テスト [stable])
    git merge --no-ff "origin/$from"
  else
    die "ブランチが見つかりません: $from"
  fi

<<<<<<< HEAD
  echo ""
  echo "✅ merge 完了。"
=======
  echo "" >&2
  echo "✅ merge 完了。" >&2
>>>>>>> a3d8c66 (test(misc): テスト - テスト [stable])

  if prompt_yn "マージ先ブランチを push しますか？" "Y"; then
    local branch
    branch="$(current_branch)"
    if git rev-parse --abbrev-ref --symbolic-full-name "@{u}" >/dev/null 2>&1; then
      git push
    else
      git push -u origin "$branch"
    fi
<<<<<<< HEAD
    echo "🚀 push 完了"
=======
    echo "🚀 push 完了" >&2
>>>>>>> a3d8c66 (test(misc): テスト - テスト [stable])
  fi
}

do_pull() {
  need_git_repo

<<<<<<< HEAD
  echo ""
  echo "⬇️ Pull を開始します。"
  echo "対象ブランチ: $(current_branch)"
  echo ""

  ensure_clean_or_confirm

  echo "（安全のため --ff-only を使います。履歴が分岐していると停止します）"
  if git pull --ff-only; then
    echo "✅ pull 完了"
  else
    echo ""
    echo "⚠️ fast-forward できませんでした。"
    echo "   対応案:"
    echo "   1) git fetch origin"
    echo "   2) git log --oneline --decorate --graph --all | head"
    echo "   3) 必要なら: git merge origin/$(current_branch)  または git rebase origin/$(current_branch)"
=======
  echo "" >&2
  echo "⬇️ Pull を開始します。" >&2
  echo "対象ブランチ: $(current_branch)" >&2
  echo "" >&2

  ensure_clean_or_confirm

  echo "（安全のため --ff-only を使います。履歴が分岐していると停止します）" >&2
  if git pull --ff-only; then
    echo "✅ pull 完了" >&2
  else
    echo "" >&2
    echo "⚠️ fast-forward できませんでした。" >&2
    echo "   対応案:" >&2
    echo "   1) git fetch origin" >&2
    echo "   2) git log --oneline --decorate --graph --all | head" >&2
    echo "   3) 必要なら: git merge origin/$(current_branch)  または git rebase origin/$(current_branch)" >&2
>>>>>>> a3d8c66 (test(misc): テスト - テスト [stable])
    exit 1
  fi
}

# ---------- main ----------
main() {
  need_git_repo

<<<<<<< HEAD
  echo ""
  echo "🧰 Git Assist"
  echo "Repo: $(git rev-parse --show-toplevel)"
  echo "Branch: $(current_branch)"
=======
  echo "" >&2
  echo "🧰 Git Assist" >&2
  echo "Repo: $(repo_root)" >&2
  echo "Branch: $(current_branch)" >&2
>>>>>>> a3d8c66 (test(misc): テスト - テスト [stable])

  local op
  op="$(select_one "やりたいことを教えて下さい。" "コミット&プッシュ" "マージ" "プル" | tail -n 1 | tr -d '\r')"

  case "$op" in
    "コミット&プッシュ") do_commit_and_push ;;
    "マージ") do_merge ;;
    "プル") do_pull ;;
    *) die "不明な操作です: $op" ;;
  esac
}

main "$@"