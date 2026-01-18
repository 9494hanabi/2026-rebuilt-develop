#!/usr/bin/env bash
set -euo pipefail

# =========================
# git-assist.sh (SETSUNA-ONLY)
# - You edit ONLY YAGSL-setsuna
# - YAGSL-daisha is protected from commits/push and accidental edits
# - After commit&push on setsuna, sync frc/robot into daisha
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

# Soft guard: check DAISHA has no changes; optionally auto revert
guard_daisha_clean_or_fix() {
  local daisha_root="$1"
  if ! git -C "$daisha_root" rev-parse --is-inside-work-tree >/dev/null 2>&1; then
    # still allow, but warn
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

  # protect daisha first
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

  # mirror sync
  rsync -a --delete "$src/" "$dst/"
  echo "✅ 同期完了" >&2

  if git -C "$daisha_root" rev-parse --is-inside-work-tree >/dev/null 2>&1; then
    echo "" >&2
    echo "📌 同期後の daisha 差分:" >&2
    git -C "$daisha_root" status --short >&2 || true
  fi
}

# ---------- operations ----------
do_commit_and_push() {
  need_git_repo
  guard_not_in_daisha

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

  # optional git tag
  if prompt_yn "Gitのタグ (git tag -a) も作りますか？" "N"; then
    local tname tmsg
    tname="$(prompt "タグ名" "例: v0.3.0")"
    tmsg="$(prompt "タグの説明" "release")"
    git tag -a "$tname" -m "$tmsg"
    git push origin "$tname"
    echo "🏷️ タグ作成＆push: $tname" >&2
  fi

  # auto sync setsuna -> daisha
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
  git fetch origin --prune

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

do_pull() {
  need_git_repo
  # pull は daisha でも許可するか悩むけど、
  # 「編集しない」だけなら pull はOKにするのが自然。
  # もし pull も禁止したいなら次の行を有効化:
  # guard_not_in_daisha

  ensure_clean_or_confirm
  echo "（安全のため --ff-only）" >&2
  git pull --ff-only || die "fast-forward できませんでした。fetchして状況確認してください。"
  echo "✅ pull 完了" >&2
}

# ---------- main ----------
main() {
  need_git_repo

  echo "" >&2
  echo "🧰 Git Assist (SETSUNA-ONLY)" >&2
  echo "Repo: $(repo_root)" >&2
  echo "Branch: $(current_branch)" >&2

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