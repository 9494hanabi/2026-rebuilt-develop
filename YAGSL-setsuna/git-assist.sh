#!/usr/bin/env bash
set -euo pipefail

# =========================
# git-assist.sh (SETSUNA-ONLY)
# - You edit ONLY YAGSL-setsuna
# - YAGSL-daisha is protected from commits/push and accidental edits
# - After commit&push on setsuna, sync frc/robot into daisha
# + branch creation / branch switch / pull target selection
# =========================

# ---------- constants ----------
REL_ROBOT_DIR="src/main/java/frc/robot"
SETSUNA_NAME="YAGSL-setsuna"
DAISHA_NAME="YAGSL-daisha"

# Protection toggles
PROTECT_DAISHA_STRICT="Y"    # Y: forbid running commit/merge in daisha project root
AUTO_REVERT_DAISHA_DIRTY="N" # Y: if daisha has local changes, auto reset+clean (DANGEROUS)
AUTO_SYNC_AFTER_PUSH="Y"     # Y: after setsuna commit&push, sync to daisha
ASK_BEFORE_SYNC="Y"          # Y: ask before syncing

# Branch/Pull behavior
PULL_MODE_DEFAULT="ff-only"  # ff-only | rebase | merge
ALLOW_BRANCH_OPS_IN_DAISHA="Y" # Y: allow switch/create/pull even in daisha (recommended). N: forbid.

# ---------- helpers ----------
die() { echo "❌ $*" >&2; exit 1; }

need_git_repo() {
  git rev-parse --is-inside-work-tree >/dev/null 2>&1 || die "ここはGitリポジトリではありません。"
}

repo_root() {
  git rev-parse --show-toplevel
}

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
  local default="${2:-Y}"
  local ans=""
  read -r -p "$msg [${default}/$( [[ "$default" == "Y" ]] && echo "n" || echo "y" )]: " ans
  ans="${ans:-$default}"
  case "${ans,,}" in
    y|yes) return 0 ;;
    n|no)  return 1 ;;
    *)     [[ "$default" == "Y" ]] && return 0 || return 1 ;;
  esac
}

# UI -> stderr, return -> stdout
select_one() {
  local title="$1"; shift
  local -a options=("$@")
  local PS3="番号を選んでください: "

  echo "" >&2
  echo "🧩 $title" >&2

  select opt in "${options[@]}"; do
    if [[ -n "${opt:-}" ]]; then
      printf "%s\n" "$opt"
      return 0
    fi
    echo "もう一度選んでね。" >&2
  done
}

ensure_clean_or_confirm() {
  if has_changes; then
    echo "" >&2
    echo "📌 現在の変更があります:" >&2
    git status --short >&2
    echo "" >&2
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
    シューター) echo "shooter" ;;
    インテーク) echo "intake" ;;
    その他の機能) echo "misc" ;;
    *) echo "misc" ;;
  esac
}

map_type() {
  local t="$1"
  case "$t" in
    エディット) echo "feat" ;;
    リファクタリング\(整形\)) echo "refactor" ;;
    デバッグ) echo "fix" ;;
    テスト) echo "test" ;;
    *) echo "chore" ;;
  esac
}

build_commit_message() {
  local op="$1"

  local action
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
    tags="$(echo "$tags" | tr -d ' ' )"
    tag_line="Tags: $tags"
  fi

  local type scope status
  type="$(map_type "$edit_kind")"
  scope="$(map_scope "$action")"
  status="$([[ "$stability" == "安定" ]] && echo "stable" || echo "buggy")"

  local subject="${type}(${scope}): ${feature_name} - ${detail} [${status}]"

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

# ---------- project discovery for sync ----------
contains_robot_dir() {
  local base="$1"
  [[ -d "$base/$REL_ROBOT_DIR" ]]
}

resolve_project_root_with_robot() {
  local base="$1"

  if contains_robot_dir "$base"; then
    echo "$base"; return 0
  fi

  local bn
  bn="$(basename "$base")"
  if [[ -d "$base/$bn" ]] && contains_robot_dir "$base/$bn"; then
    echo "$base/$bn"; return 0
  fi

  local d
  for d in "$base"/*; do
    [[ -d "$d" ]] || continue
    if contains_robot_dir "$d"; then
      echo "$d"; return 0
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
    *"/${SETSUNA_NAME}"/*|*"/${SETSUNA_NAME}") echo "$SETSUNA_NAME" ;;
    *"/${DAISHA_NAME}"/*|*"/${DAISHA_NAME}") echo "$DAISHA_NAME" ;;
    *) echo "" ;;
  esac
}

# Hard guard: prevent dangerous operations while you're inside DAISHA working tree
guard_not_in_daisha() {
  local cur
  cur="$(detect_current_project)"
  if [[ "$PROTECT_DAISHA_STRICT" == "Y" && "$cur" == "$DAISHA_NAME" ]]; then
    die "保護: ${DAISHA_NAME} 側ではこの操作は禁止です。setsuna で作業してください。"
  fi
}

guard_branch_ops_allowed_here() {
  local cur
  cur="$(detect_current_project)"
  if [[ "$ALLOW_BRANCH_OPS_IN_DAISHA" != "Y" && "$cur" == "$DAISHA_NAME" ]]; then
    die "保護: ${DAISHA_NAME} 側ではブランチ操作も禁止です。setsuna 側で実行してね。"
  fi
}

# Soft guard: check DAISHA has no changes; optionally auto revert
guard_daisha_clean_or_fix() {
  local daisha_root="$1"
  if ! git -C "$daisha_root" rev-parse --is-inside-work-tree >/dev/null 2>&1; then
    echo "⚠️ daisha 側がGitとして認識できません: $daisha_root" >&2
    return 0
  fi

  local st
  st="$(git -C "$daisha_root" status --porcelain || true)"
  if [[ -z "$st" ]]; then
    return 0
  fi

  echo "" >&2
  echo "🛡️ 保護: ${DAISHA_NAME} に変更が入っています（本来は編集しない想定）" >&2
  git -C "$daisha_root" status --short >&2

  if [[ "$AUTO_REVERT_DAISHA_DIRTY" == "Y" ]]; then
    echo "⚠️ AUTO_REVERT_DAISHA_DIRTY=Y のため、daisha を強制的に元に戻します。" >&2
    echo "   (git reset --hard + git clean -fd)" >&2
    if prompt_yn "本当に実行しますか？（取り消せません）" "N"; then
      git -C "$daisha_root" reset --hard
      git -C "$daisha_root" clean -fd
      echo "✅ daisha をクリーン状態に戻しました" >&2
    else
      die "中断しました。daisha の変更を先に処理してね。"
    fi
  else
    die "中断しました。daisha 側に変更があるので同期/更新できません。"
  fi
}

sync_robot_code_setsuna_to_daisha() {
  local setsuna_root="$1"
  local daisha_root="$2"

  local src="$setsuna_root/$REL_ROBOT_DIR"
  local dst="$daisha_root/$REL_ROBOT_DIR"

  [[ -d "$src" ]] || die "同期元が見つかりません: $src"
  mkdir -p "$(dirname "$dst")"

  guard_daisha_clean_or_fix "$daisha_root"

  echo "" >&2
  echo "🔁 同期: ${SETSUNA_NAME} -> ${DAISHA_NAME}" >&2
  echo "   FROM: $src" >&2
  echo "   TO  : $dst" >&2

  if prompt_yn "同期前にバックアップを作りますか？" "Y"; then
    local ts bak
    ts="$(date +"%Y%m%d_%H%M%S")"
    bak="$daisha_root/.git-assist-backup/$ts/$REL_ROBOT_DIR"
    mkdir -p "$bak"
    if [[ -d "$dst" ]]; then
      rsync -a "$dst/" "$bak/"
    fi
    echo "🗄️ Backup: $bak" >&2
  fi

  rsync -a --delete "$src/" "$dst/"
  echo "✅ 同期完了" >&2

  if git -C "$daisha_root" rev-parse --is-inside-work-tree >/dev/null 2>&1; then
    echo "" >&2
    echo "📌 同期後の daisha 差分:" >&2
    git -C "$daisha_root" status --short >&2 || true
  fi
}

# ---------- branch utilities ----------
fetch_origin() {
  git fetch origin --prune >/dev/null 2>&1 || true
}

list_local_branches() {
  git branch --format="%(refname:short)"
}

branch_exists_local() {
  local b="$1"
  git show-ref --verify --quiet "refs/heads/$b"
}

branch_exists_remote() {
  local b="$1"
  git show-ref --verify --quiet "refs/remotes/origin/$b"
}

do_branch_create() {
  need_git_repo
  guard_branch_ops_allowed_here

  ensure_clean_or_confirm
  fetch_origin

  echo "" >&2
  echo "🌱 新しいブランチを作成します。" >&2
  echo "現在: $(current_branch)" >&2

  local base
  base="$(select_one "ベースにするブランチを選んでください" "develop" "main" "今のブランチ( $(current_branch) )" "その他（手入力）" | tail -n 1 | tr -d '\r')"
  if [[ "$base" == "今のブランチ( $(current_branch) )" ]]; then
    base="$(current_branch)"
  elif [[ "$base" == "その他（手入力）" ]]; then
    base="$(prompt "ベースブランチ名" "develop")"
  fi

  local newb
  newb="$(prompt "新しいブランチ名" "feature/xxx")"

  # checkout base (local or remote)
  if branch_exists_local "$base"; then
    git switch "$base"
  elif branch_exists_remote "$base"; then
    git switch -c "$base" "origin/$base"
  else
    die "ベースブランチが見つかりません: $base"
  fi

  # create
  if branch_exists_local "$newb"; then
    die "そのブランチは既に存在します: $newb"
  fi

  git switch -c "$newb"
  echo "✅ 作成 & 移動しました: $newb" >&2

  if prompt_yn "このブランチを origin に push（-u）しますか？" "Y"; then
    git push -u origin "$newb"
    echo "🚀 push 完了: origin/$newb" >&2
  fi
}

do_branch_switch() {
  need_git_repo
  guard_branch_ops_allowed_here

  ensure_clean_or_confirm
  fetch_origin

  echo "" >&2
  echo "🧭 ブランチを移動します。" >&2

  local target
  target="$(prompt "移動先ブランチ名（local/remoteどちらでも）" "develop")"

  if branch_exists_local "$target"; then
    git switch "$target"
  elif branch_exists_remote "$target"; then
    # create tracking local branch
    git switch -c "$target" "origin/$target"
  else
    die "ブランチが見つかりません: $target"
  fi

  echo "✅ 現在のブランチ: $(current_branch)" >&2
}

# Pull options:
# A) "checkoutしてそのブランチを pull" (develop/main など)
# B) "今のブランチに origin/develop などを取り込む" (merge/rebase/ff-only)
do_pull() {
  need_git_repo
  guard_branch_ops_allowed_here

  ensure_clean_or_confirm
  fetch_origin

  echo "" >&2
  echo "⬇️ Pull を開始します。" >&2
  echo "現在: $(current_branch)" >&2

  local pull_style
  pull_style="$(select_one "どのやり方で更新しますか？" \
    "ブランチを切り替えて pull（例: develop を pull）" \
    "今のブランチに別ブランチを取り込む（例: develop を取り込む）" \
    | tail -n 1 | tr -d '\r')"

  local mode
  mode="$(select_one "pull/取り込みモードを選んでください" "ff-only（安全）" "rebase（履歴を直線に）" "merge（マージコミット）" \
    | tail -n 1 | tr -d '\r')"
  case "$mode" in
    ff-only* ) mode="ff-only" ;;
    rebase*  ) mode="rebase" ;;
    merge*   ) mode="merge" ;;
    *        ) mode="$PULL_MODE_DEFAULT" ;;
  esac

  if [[ "$pull_style" == "ブランチを切り替えて pull（例: develop を pull）" ]]; then
    local tgt
    tgt="$(select_one "どのブランチを pull しますか？" "develop" "main" "その他（手入力）" | tail -n 1 | tr -d '\r')"
    if [[ "$tgt" == "その他（手入力）" ]]; then
      tgt="$(prompt "pull するブランチ名" "develop")"
    fi

    # switch to target
    if branch_exists_local "$tgt"; then
      git switch "$tgt"
    elif branch_exists_remote "$tgt"; then
      git switch -c "$tgt" "origin/$tgt"
    else
      die "ブランチが見つかりません: $tgt"
    fi

    echo "📍 現在: $(current_branch) を更新します（mode=$mode）" >&2
    case "$mode" in
      ff-only) git pull --ff-only ;;
      rebase)  git pull --rebase ;;
      merge)   git pull --no-rebase ;;
    esac
    echo "✅ pull 完了: $(current_branch)" >&2
    return 0
  fi

  # "取り込む" モード
  local src
  src="$(select_one "どのブランチを今のブランチに取り込みますか？" "develop" "main" "その他（手入力）" | tail -n 1 | tr -d '\r')"
  if [[ "$src" == "その他（手入力）" ]]; then
    src="$(prompt "取り込むブランチ名" "develop")"
  fi

  # ensure origin/<src> exists
  if ! branch_exists_remote "$src"; then
    # maybe only local exists
    if ! branch_exists_local "$src"; then
      die "取り込み元ブランチが見つかりません: $src"
    fi
  fi

  local cur
  cur="$(current_branch)"
  echo "🧲 取り込み: $cur <- $src（mode=$mode）" >&2

  case "$mode" in
    ff-only)
      # fast-forward only possible when current is behind src (rare for "取り込み")
      # We'll attempt merge --ff-only from origin/src
      if branch_exists_remote "$src"; then
        git merge --ff-only "origin/$src" || die "ff-only できませんでした。rebase/merge を選んでね。"
      else
        git merge --ff-only "$src" || die "ff-only できませんでした。rebase/merge を選んでね。"
      fi
      ;;
    rebase)
      if branch_exists_remote "$src"; then
        git rebase "origin/$src"
      else
        git rebase "$src"
      fi
      ;;
    merge)
      if branch_exists_remote "$src"; then
        git merge --no-ff "origin/$src"
      else
        git merge --no-ff "$src"
      fi
      ;;
  esac

  echo "✅ 取り込み完了: $cur" >&2
}

# ---------- existing operations ----------
do_commit_and_push() {
  need_git_repo
  guard_not_in_daisha

    # --- safety gate: confirm branch before doing anything ---
  local branch_now
  branch_now="$(current_branch)"
  echo "" >&2
  echo "🧭 現在のブランチは '${branch_now}' です。" >&2
  if ! prompt_yn "このブランチでコミット&プッシュを実行しても良いですか？" "N"; then
    die "中断しました。"
  fi

  echo "" >&2
  echo "🧾 Commit & Push を開始します。" >&2
  echo "現在のブランチ: $(current_branch)" >&2
  echo "" >&2

  if ! has_changes; then
    echo "✅ 変更がありません。コミット不要です。" >&2
    return 0
  fi

  git status --short >&2
  echo "" >&2

  if prompt_yn "git add -A (全変更をステージ) しますか？" "Y"; then
    git add -A
  else
    die "中断しました。（手動で git add してから再実行してね）"
  fi

  local msg
  msg="$(build_commit_message "commit-push")"

  echo "" >&2
  echo "📝 生成されたコミットメッセージ:" >&2
  echo "------------------------------" >&2
  echo "$msg" >&2
  echo "------------------------------" >&2
  echo "" >&2

  if ! prompt_yn "このメッセージで commit しますか？" "Y"; then
    die "中断しました。"
  fi

  git commit -m "$(echo "$msg" | head -n 1)" -m "$(echo "$msg" | tail -n +3)"

  local branch
  branch="$(current_branch)"

  echo "" >&2
  echo "🚀 push します: origin ${branch}" >&2

  if git rev-parse --abbrev-ref --symbolic-full-name "@{u}" >/dev/null 2>&1; then
    git push
  else
    git push -u origin "$branch"
  fi

  echo "✅ 完了: commit & push" >&2

  if prompt_yn "Gitのタグ (git tag -a) も作りますか？" "N"; then
    local tname tmsg
    tname="$(prompt "タグ名" "例: v0.3.0")"
    tmsg="$(prompt "タグの説明" "release")"
    git tag -a "$tname" -m "$tmsg"
    git push origin "$tname"
    echo "🏷️ タグ作成＆push: $tname" >&2
  fi

  if [[ "$AUTO_SYNC_AFTER_PUSH" == "Y" ]]; then
    local root setsuna_dir daisha_dir
    root="$(repo_root)"

    setsuna_dir="$(find_project_dir "$root" "$SETSUNA_NAME" || true)"
    daisha_dir="$(find_project_dir "$root" "$DAISHA_NAME" || true)"

    if [[ -n "${setsuna_dir:-}" && -n "${daisha_dir:-}" ]]; then
      if [[ "$ASK_BEFORE_SYNC" == "Y" ]]; then
        if prompt_yn "setsuna の $REL_ROBOT_DIR を daisha に同期しますか？" "Y"; then
          sync_robot_code_setsuna_to_daisha "$setsuna_dir" "$daisha_dir"
        else
          echo "ℹ️ 同期をスキップしました。" >&2
        fi
      else
        sync_robot_code_setsuna_to_daisha "$setsuna_dir" "$daisha_dir"
      fi
    else
      echo "ℹ️ 同期先/元が見つからないため、同期をスキップしました。" >&2
      echo "   setsuna='${setsuna_dir:-}' daisha='${daisha_dir:-}'" >&2
    fi
  fi
}

do_merge() {
  need_git_repo
  guard_not_in_daisha

  echo "" >&2
  echo "🔀 Merge を開始します。" >&2
  echo "現在のブランチ(マージ先): $(current_branch)" >&2

  ensure_clean_or_confirm
  fetch_origin

  echo "" >&2
  echo "📚 ローカルブランチ:" >&2
  git branch --format="%(refname:short)" | sed 's/^/  - /' >&2

  echo "" >&2
  echo "🌐 リモート(origin)ブランチ:" >&2
  git branch -r --format="%(refname:short)" | sed 's/^/  - /' >&2

  local from
  from="$(prompt "どのブランチを取り込みますか？ (例: dev)" "dev")"

  if git show-ref --verify --quiet "refs/heads/$from"; then
    git merge --no-ff "$from"
  elif git show-ref --verify --quiet "refs/remotes/origin/$from"; then
    git merge --no-ff "origin/$from"
  else
    die "ブランチが見つかりません: $from"
  fi

  echo "✅ merge 完了。" >&2

  if prompt_yn "push しますか？" "Y"; then
    local branch
    branch="$(current_branch)"
    if git rev-parse --abbrev-ref --symbolic-full-name "@{u}" >/dev/null 2>&1; then
      git push
    else
      git push -u origin "$branch"
    fi
  fi
}

# ---------- main ----------
main() {
  need_git_repo

  echo "" >&2
  echo "🧰 Git Assist (SETSUNA-ONLY)" >&2
  echo "Repo: $(repo_root)" >&2
  echo "Branch: $(current_branch)" >&2

  local op
  op="$(select_one "やりたいことを教えて下さい。" \
    "コミット&プッシュ" \
    "マージ" \
    "プル（選択式）" \
    "新しいブランチの作成" \
    "ブランチの移動" \
    | tail -n 1 | tr -d '\r')"

  case "$op" in
    "コミット&プッシュ") do_commit_and_push ;;
    "マージ") do_merge ;;
    "プル（選択式）") do_pull ;;
    "新しいブランチの作成") do_branch_create ;;
    "ブランチの移動") do_branch_switch ;;
    *) die "不明な操作です: $op" ;;
  esac
}

main "$@"